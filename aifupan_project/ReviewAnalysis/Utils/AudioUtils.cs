using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr.Local.Vad;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class AudioUtils
    {
        static string ffmpegPath = "tools\\ffmpeg";

        /// <summary>
        /// VAD 引擎挂载点。默认为 Stub（IsReady=false），所有调用走降级到旧硬切。
        /// Phase 2 将由 Program.cs 启动逻辑替换为真正的 FsmnVadEngine。
        /// 替换示例：<code>AudioUtils.VadEngine = new FsmnVadEngine(...);</code>
        /// </summary>
        public static IVadEngine VadEngine { get; set; } = new FsmnVadEngineStub();

        /// <summary>
        /// 读取 App.config 的 AsrVadEnabled 开关。缺失/非 "false" 即视为 true。
        /// 关闭方式：在 App.config &lt;appSettings&gt; 加 &lt;add key="AsrVadEnabled" value="false"/&gt;
        /// </summary>
        private static bool IsVadEnabled()
        {
            try
            {
                var val = ConfigurationManager.AppSettings["AsrVadEnabled"];
                return string.IsNullOrEmpty(val) || !val.Trim().Equals("false", StringComparison.OrdinalIgnoreCase);
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[Slicer] 读 AsrVadEnabled 配置异常，默认启用: {ex.Message}");
                return true;
            }
        }

        /// <summary>
        /// 智能切片可用性检查：开关 + 引擎就绪。
        /// 日志含 VadEngine 实际类型名，便于真模型接入后定位生产实例。
        /// </summary>
        private static bool CanUseSmartSlicing()
        {
            if (!IsVadEnabled())
            {
                FileUtils.LogAnalysis("[Slicer] VAD 开关关闭，走旧硬切");
                return false;
            }
            if (VadEngine == null)
            {
                FileUtils.LogAnalysis("[Slicer] VadEngine=null，走旧硬切");
                return false;
            }
            string engineName = VadEngine.GetType().Name;
            if (!VadEngine.IsReady)
            {
                FileUtils.LogAnalysis($"[Slicer] VadEngine={engineName} IsReady=false，走旧硬切");
                return false;
            }
            FileUtils.LogAnalysis($"[Slicer] VadEngine={engineName} IsReady=true，启用智能切片");
            return true;
        }

        /// <summary>
        /// 将视频切割成音频文件（顶层路由）。
        ///
        /// 路由逻辑：
        ///   ① VAD 开关开 + 引擎就绪 → 智能切片（在 silence gap 切，per spec 20260526_fsmn_vad_smart_slicing）
        ///   ② 否则或智能切片抛异常 → 降级到旧的 FFmpeg -f segment 59s 硬切
        ///
        /// Phase 1：智能切片路径尚未实施（VadEngine 默认是 Stub，IsReady=false）→ 始终走旧硬切，零行为变化。
        /// Phase 2：引入真正的 FsmnVadEngine + SlicingAudioSmart 实现，激活智能切片。
        /// </summary>
        /// <param name="videoPath">视频路径</param>
        /// <param name="outputDirectoryPath">输出音频文件的目录路径</param>
        public static void SlicingAudio(string videoPath, string outputDirectoryPath)
        {
            if (CanUseSmartSlicing())
            {
                try
                {
                    SlicingAudioSmart(videoPath, outputDirectoryPath);
                    return;
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[Slicer] 智能切片异常，降级硬切: {ex.Message}");
                }
            }
            SlicingAudioLegacy(videoPath, outputDirectoryPath);
        }

        /// <summary>
        /// 智能切片实现占位（Phase 2）。
        /// 完整实现待真正的 FsmnVadEngine 接入：
        ///   ① ffmpeg 解码全 PCM（单文件输出，不分段）
        ///   ② VadEngine.GetSegments(pcm) → VadSegment[]
        ///   ③ SmartSlicer.PlanCuts(segments, totalMs) → 切分计划
        ///   ④ ffmpeg 按 -ss/-t 提取每段写入 audio_NNN.wav
        ///   ⑤ 沿用现有 &lt;1KB 文件丢弃逻辑
        /// </summary>
        private static void SlicingAudioSmart(string videoPath, string outputDirectoryPath)
        {
            throw new NotImplementedException(
                "SlicingAudioSmart 在 Phase 2 实施。当前 Phase 1 仅完成架构落地，VadEngine 默认是 Stub。");
        }

        /// <summary>
        /// 旧的 FFmpeg -f segment 59s 硬切实现，作为降级路径长期保留。
        /// 当前 Phase 1 这是唯一被实际执行的切片路径。
        /// </summary>
        private static void SlicingAudioLegacy(string videoPath, string outputDirectoryPath)
        {
            FileUtils.LogAnalysis($"将视频切割成音频文件-输出音频文件的目录路径：{outputDirectoryPath}");

            // 创建输出文件夹
            try
            {
                if (!Directory.Exists(outputDirectoryPath))
                {
                    Directory.CreateDirectory(outputDirectoryPath);
                }
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"将视频切割成音频文件-创建输出文件夹异常：{e}");
                throw new IOException();
            }

            // 由于 FFmpeg 使用 %03d 作为分段命名占位符
            // 如果路径中包含 % 字符会导致解析错误
            // 解决方案：使用临时目录进行切割，完成后移动到目标目录
            string tempOutputDir = Path.Combine(Path.GetTempPath(), $"audio_{Guid.NewGuid():N}");
            Directory.CreateDirectory(tempOutputDir);
            FileUtils.LogAnalysis($"将视频切割成音频文件-使用临时目录：{tempOutputDir}");

            try
            {
                // 构建 ffmpeg 命令 - 使用临时目录
                // 滤镜链（人声增强 / 频谱整形 / 响度归一）：
                //   aresample        — 异步重采样保持音视频同步
                //   highpass=f=80    — 砍 <80Hz sub-bass（爆炸/枪声/低频鼓点），不伤人声
                //   equalizer 200Hz  — 方向反转：旧 g=-2（减闷腔/口播清晰度），新 g=+3（补人声基频/轻声+唱歌识别）
                //   equalizer 2800Hz — 轻增 +2dB 提升咝音/辅音能量（罚款vs性发 类同音字区分）
                //   equalizer 6000Hz — 新增 highshelf +1.5dB 提升辅音/齿音能量（多人混说字迹区分）
                //   dynaudnorm       — 改温和：f=500→200（响应短促轻声）、g=15→5（抑制噪声放大）、r=0.5→0.7（轻声补偿更充分）
                // 不加 lowpass / afftdn / arnndn / silenceremove / anlmdn / afir：详见 audio_voice_enhancement_rules.md 与 bgm_preservation_rules.md。
                //
                // ⚠️ 路线 B 滤镜调优（2026-05-29 / run_id: 20260529_111437_svs_ffmpeg_filter_tune）
                //    目标 badcase：(1) 轻声/混响唱歌无字 (2) 多人混说字迹混乱 (3) 多人轻声断续
                //    旧参数（2026-05-26 版）完整保留供回退参考：
                //      "...,highpass=f=80,equalizer=f=200:width_type=q:width=1.5:g=-2,equalizer=f=2800:width_type=q:width=1.5:g=2,dynaudnorm=f=500:g=15:r=0.5:n=1,..."
                //    紧急回退：git checkout HEAD -- Utils/AudioUtils.cs
                //    折中回退：若常规口播闷腔感增加，可将 200Hz g=+3 → g=0 或 g=+1
                //    完整设计：见 .claude/runs/20260529_111437_svs_ffmpeg_filter_tune/openspec.md §5
                string arguments = $"-i \"{videoPath}\" -vn -af \"aresample=async=1:min_hard_comp=0.100:first_pts=0,highpass=f=80,equalizer=f=200:t=q:w=1:g=3,equalizer=f=2800:t=q:w=1:g=2,equalizer=f=6000:t=h:w=1:g=1.5,dynaudnorm=f=200:g=5:r=0.7:n=1\" -ar 16000 -ac 1 -f segment -segment_time 59 -reset_timestamps 1 -segment_format wav \"{tempOutputDir}\\audio_%03d.wav\"";

                FileUtils.LogAnalysis($"FFmpeg命令参数：{arguments}");

                // 创建 ProcessStartInfo 对象
                ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true,
                    StandardOutputEncoding = Encoding.UTF8,
                    StandardErrorEncoding = Encoding.UTF8
                };

                // 创建 Process 对象
                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;
                    FileUtils.LogAnalysis($"将视频切割成音频文件-开始切割音频");

                    process.OutputDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);
                    process.ErrorDataReceived += new DataReceivedEventHandler(ErrorDataReceivedHandler);

                    // 启动 ffmpeg 进程
                    process.Start();

                    // 开始监听标准输出和错误输出
                    process.BeginOutputReadLine();
                    process.BeginErrorReadLine();

                    // 等待 ffmpeg 进程结束，设置超时时间为30分钟
                    int timeout = 30 * 60 * 1000; // 30分钟
                    bool exited = process.WaitForExit(timeout);
                    
                    int exitCode = process.ExitCode;
                    FileUtils.LogAnalysis($"FFmpeg进程退出码：{exitCode}，是否正常退出：{exited}");
                    
                    if (!exited)
                    {
                        try
                        {
                            process.Kill();
                            FileUtils.LogAnalysis($"将视频切割成音频文件-切割超时，已强制结束");
                            throw new TimeoutException("音频切割超时");
                        }
                        catch (TimeoutException)
                        {
                            throw;
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogAnalysis($"{ex}", $"将视频切割成音频文件-强制结束时发生异常");
                        }
                    }
                    
                    if (exitCode != 0)
                    {
                        FileUtils.LogAnalysis($"FFmpeg执行失败，退出码：{exitCode}");
                        throw new Exception($"FFmpeg执行失败，退出码：{exitCode}");
                    }
                }

                // 检查临时目录中是否有音频文件
                var tempAudioFiles = Directory.GetFiles(tempOutputDir, "*.wav");
                FileUtils.LogAnalysis($"临时目录音频文件数量：{tempAudioFiles.Length}");
                
                if (tempAudioFiles.Length == 0)
                {
                    FileUtils.LogAnalysis($"未生成任何音频文件");
                    throw new Exception("音频切割失败：未生成任何音频文件");
                }

                // 丢弃明显损坏 / 纯静音的尾段，保留正常切片。
                // 阈值由 10KB(~320ms) 改为 1KB(~32ms @ 16kHz mono 16-bit PCM)：
                //   旧阈值会把视频末尾不足 0.3s 的有效尾段当垃圾删除，导致末几字识别丢失；
                //   新阈值仅过滤 ffmpeg 偶发产出的空 WAV / 头损坏文件。
                //   静音检测不应靠文件大小（应由 dB 测量或 VAD 处理），本步骤只做最弱兜底。
                int validCount = 0;
                int deletedCount = 0;
                foreach (var tempFile in tempAudioFiles)
                {
                    long fileSize = new FileInfo(tempFile).Length;
                    if (fileSize < 1 * 1024) // 小于1KB（≈32ms）视为空/损坏
                    {
                        File.Delete(tempFile);
                        deletedCount++;
                    }
                    else
                    {
                        // 移动到目标目录
                        string fileName = Path.GetFileName(tempFile);
                        string destFile = Path.Combine(outputDirectoryPath, fileName);
                        // 兼容.NET Framework：先删除已存在的文件再移动
                        if (File.Exists(destFile))
                        {
                            File.Delete(destFile);
                        }
                        File.Move(tempFile, destFile);
                        validCount++;
                    }
                }

                FileUtils.LogAnalysis($"将视频切割成音频文件-结束切割音频，有效文件：{validCount}，删除小文件：{deletedCount}");
                
                // 检查目标目录中是否有有效音频文件
                var finalAudioFiles = Directory.GetFiles(outputDirectoryPath, "*.wav");
                if (finalAudioFiles.Length == 0)
                {
                    FileUtils.LogAnalysis($"所有音频文件都小于10KB，可能是视频无声音");
                    throw new Exception("音频切割失败：所有音频文件都小于10KB");
                }
            }
            finally
            {
                // 清理临时目录
                try
                {
                    if (Directory.Exists(tempOutputDir))
                    {
                        Directory.Delete(tempOutputDir, true);
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"清理临时目录失败：{ex.Message}");
                }
            }
        }


        private static void OutputDataReceivedHandler(object sendingProcess, DataReceivedEventArgs outLine)
        {
            if (!string.IsNullOrEmpty(outLine.Data))
            {
                FileUtils.LogAnalysis($"FFmpeg输出：{outLine.Data}");
            }
        }

        // FFmpeg 把版本/配置/Stream info/进度全打到 stderr，绝大多数不是真错误。
        // 仅命中下列关键字才标"FFmpeg错误"，否则标普通"FFmpeg"。
        // 参考: run_id 20260529_111437_svs_ffmpeg_filter_tune 的 smoke 日志（stderr 30+ 行均为正常 info）。
        private static readonly string[] FFmpegRealErrorMarkers = new[]
        {
            "Error opening", "Error while", "Invalid argument", "Invalid data",
            "Permission denied", "No such file", "Could not find", "Could not open",
            "Failed to", "Conversion failed"
        };

        private static void ErrorDataReceivedHandler(object sendingProcess, DataReceivedEventArgs outLine)
        {
            if (string.IsNullOrEmpty(outLine.Data)) return;

            bool isRealError = false;
            foreach (var marker in FFmpegRealErrorMarkers)
            {
                if (outLine.Data.IndexOf(marker, StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    isRealError = true;
                    break;
                }
            }

            string prefix = isRealError ? "FFmpeg错误" : "FFmpeg";
            FileUtils.LogAnalysis($"{prefix}：{outLine.Data}");
        }
    }
}
