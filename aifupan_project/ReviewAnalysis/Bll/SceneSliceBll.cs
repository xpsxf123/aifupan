using ReviewAnalysis.api;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using System;
using System.Diagnostics;
using System.IO;
using douyin.Utils;

namespace ReviewAnalysis.Bll
{
    public class SceneSliceBll
    {

        private static readonly string ffmpegPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "tools\\ffmpeg.exe");

        /// <summary>
        /// 执行场景切片：获取状态 → 截帧 → 上传OSS → 保存。
        /// </summary>
        /// <param name="videoId">视频ID</param>
        /// <returns>是否成功</returns>
        public static bool Execute(string videoId)
        {
            try
            {
                // 1. 获取状态
                SceneSliceStatusVo statusVo = SceneSliceApi.GetStatus(videoId);
                if (statusVo == null)
                {
                    FileUtils.LogError($"获取场景切片状态失败，videoId={videoId}", "场景切片");
                    return false;
                }

                // 2. 已完成 → 跳过
                if (statusVo.status == 2)
                {
                    return true;
                }

                // 3. 未完成 → 截帧 + 上传 + 保存
                if (statusVo.status == 0)
                {
                    VideoEntity video = VideoApi.GetVideoByVideoIdSync(videoId);
                    if (video == null || string.IsNullOrEmpty(video.storagePath))
                    {
                        FileUtils.LogError($"未找到视频文件，videoId={videoId}", "场景切片");
                        return false;
                    }
                    if (!File.Exists(video.storagePath))
                    {
                        FileUtils.LogError($"视频文件不存在: {video.storagePath}", "场景切片");
                        return false;
                    }

                    int duration = VideoUtils.getVideoDuration(video.storagePath);
                    if (duration <= 0)
                    {
                        FileUtils.LogError($"获取视频时长失败，videoId={videoId}", "场景切片");
                        return false;
                    }

                    int sliceSeconds = statusVo.sliceSeconds ?? 20;
                    int seekSeconds = Math.Max(0, duration - sliceSeconds);
                    string framePath = Path.Combine(Path.GetTempPath(), $"scene_slice_{videoId}_{DateTime.Now:yyyyMMddHHmmss}.jpg");

                    if (!ExtractFrame(video.storagePath, framePath, seekSeconds, duration))
                    {
                        FileUtils.LogError($"截帧失败，videoId={videoId}", "场景切片");
                        return false;
                    }

                    try
                    {
                        bool uploadOk = UploadUtils.UploadFileAsync(statusVo.signedUrl, framePath, null);
                        if (!uploadOk)
                        {
                            FileUtils.LogError($"OSS上传失败，videoId={videoId}", "场景切片");
                            return false;
                        }

                        bool saveOk = SceneSliceApi.Save(videoId, statusVo.ossKey, sliceSeconds);
                        if (!saveOk)
                        {
                            FileUtils.LogError($"保存场景切片记录失败，videoId={videoId}", "场景切片");
                            return false;
                        }

                        return true;
                    }
                    finally
                    {
                        try { if (File.Exists(framePath)) File.Delete(framePath); }
                        catch (Exception ex) { FileUtils.LogError($"{ex}", "场景切片-清理临时文件"); }
                    }
                }

                return false;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"场景切片流程异常，videoId={videoId}");
                return false;
            }
        }

        /// <summary>
        /// 两步截帧：先流拷贝关键帧附近的片段，再从临时视频精确解码目标帧，避免失帧
        /// 第1步: -ss clipStart → -t clipDuration → -c copy（极快，只拷贝不解码）
        /// 第2步: 从临时视频用 -ss targetOffset 解码目标帧
        /// </summary>
        private static bool ExtractFrame(string videoPath, string outputPath, int seekSeconds, int duration)
        {
            string tempVideo = null;
            try
            {
                // 往前 3 秒起切，但不超过视频起点；片段时间 6 秒，但不超过视频末尾
                int paddingBefore = Math.Min(3, seekSeconds);
                int clipStart = seekSeconds - paddingBefore;
                int clipDuration = Math.Min(paddingBefore + 3, duration - clipStart);
                int targetOffset = seekSeconds - clipStart;

                tempVideo = Path.Combine(Path.GetTempPath(), $"scene_slice_temp_{Guid.NewGuid():N}.ts");

                // 第1步：从关键帧位置截取前后几秒的视频片段
                string clipArgs = $"-ss {clipStart} -i \"{videoPath}\" -t {clipDuration} -c copy \"{tempVideo}\" -y";

                using (Process clipProcess = Process.Start(new ProcessStartInfo(ffmpegPath, clipArgs)
                {
                    UseShellExecute = false, CreateNoWindow = true,
                    RedirectStandardOutput = true, RedirectStandardError = true
                }))
                {
                    if (clipProcess == null) return false;
                    clipProcess.WaitForExit(30000);
                    if (clipProcess.ExitCode != 0 || !File.Exists(tempVideo))
                        return false;
                }

                // 第2步：从临时视频精确截取目标帧（-ss 放 -i 后面 = 精确解码到目标帧）
                string frameArgs = $"-i \"{tempVideo}\" -ss {targetOffset} -vframes 1 -q:v 2 \"{outputPath}\" -y";

                using (Process frameProcess = Process.Start(new ProcessStartInfo(ffmpegPath, frameArgs)
                {
                    UseShellExecute = false, CreateNoWindow = true,
                    RedirectStandardOutput = true, RedirectStandardError = true
                }))
                {
                    if (frameProcess == null) return false;
                    frameProcess.WaitForExit(30000);
                    return frameProcess.ExitCode == 0 && File.Exists(outputPath);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "场景切片-截帧异常");
                return false;
            }
            finally
            {
                try { if (tempVideo != null && File.Exists(tempVideo)) File.Delete(tempVideo); } catch { }
            }
        }

    }
}
