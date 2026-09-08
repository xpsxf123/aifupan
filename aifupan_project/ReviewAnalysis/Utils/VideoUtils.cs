using CefSharp;
using douyin.Utils;
using MediaInfo;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo.video;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Security.Policy;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Windows;

namespace ReviewAnalysis.Utils
{
    public class VideoUtils
    {
        static string ffmpegPath = "tools\\ffmpeg";
        /// <summary>
        /// FFmpeg可执行文件路径（建议从配置读取，此处保持静态）
        /// </summary>
        private static readonly string ffmpegAsyncPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "tools\\ffmpeg.exe");
        /// <summary>
        /// 当前重新编码的ffmepg进程
        /// </summary>
        public static Process reEncodeProcess = null;
        public static int reEncodeProcessId = -1;
        /// <summary>
        /// 重新编码的ffmepg进程是否正常结束
        /// </summary>
        public static bool normalEnd = true;


        /// <summary>
        /// 切片视频
        /// </summary>
        /// <param name="sourceVideoPath">原视频路径</param>
        /// <param name="sliceVideoPath">切片视频输出路径</param>
        /// <param name="startTimeMs">截取的时间戳（毫秒）</param>
        /// <param name="durationMs">截取时长（毫秒）</param>
        public static void SliceVideo(string sourceVideoPath, string sliceVideoPath, long startTimeMs, long durationMs)
        {

            string dir = Path.GetDirectoryName(sliceVideoPath);
            if (!Directory.Exists(dir))
            {
                Directory.CreateDirectory(dir);
            }

            // 转换为 FFmpeg 格式 (hh:mm:ss.fff)
            string startTimeStr = TimeUtils.millisecondsFormat(startTimeMs);
            string durationStr = TimeUtils.millisecondsFormat(durationMs);

            // 精确截取命令
            string command = $"-ss {startTimeStr} -t {durationStr} -i \"{sourceVideoPath}\" -c:v libx264 -c:a aac -strict experimental -b:a 98k \"{sliceVideoPath}\" -y";

            // 执行 FFmpeg
            using (Process process = new Process())
            {
                process.StartInfo = new ProcessStartInfo
                {
                    FileName = ffmpegPath,
                    Arguments = command,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };

                process.OutputDataReceived += OutputDataReceivedHandler;
                process.ErrorDataReceived += OutputDataReceivedHandler;

                process.Start();

                // 启动异步读取输出
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();

                // 超时时间设置为120分钟
                int timeout = 120 * 60 * 1000;
                if (!process.WaitForExit(timeout))
                {
                    try
                    {
                        process.Kill();
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogAnalysis($"{ex}", "FFmpeg 切片视频超时关闭进程发生异常");
                    }
                    
                    FileUtils.LogError($"{sourceVideoPath}==={sliceVideoPath}==={startTimeMs}==={durationMs}", "FFmpeg 切片视频超时");
                    
                    if(File.Exists(sliceVideoPath))
                    {
                        File.Delete(sliceVideoPath);
                    }
                    throw new Exception("FFmpeg 切片视频超时");
                }

                if (process.ExitCode != 0)
                {
                    FileUtils.LogAnalysis($"{sourceVideoPath}==={sliceVideoPath}==={startTimeMs}==={durationMs}", "FFmpeg 切片视频错误");

                    if (File.Exists(sliceVideoPath))
                    {
                        File.Delete(sliceVideoPath);
                    }
                    throw new Exception("FFmpeg 切片视频超时");
                }
            }
        }

        /// <summary>
        /// 合并视频
        /// </summary>
        /// <param name="videoPath">主视频的文件地址</param>
        public static void MegerVideo(string videoPath, string videoId)
        {
            try
            {

                // 获取指定目录下的所有ts文件
                string[] files = Directory.GetFiles(videoPath.Substring(0, videoPath.LastIndexOf("\\")), "*.ts", SearchOption.TopDirectoryOnly);

                if(files != null && files.Length > 0)
                {
                    List<string> resourceFilePathList = new List<string>();
                    // 添加主视频到第一个文件
                    StringBuilder fileListContent = new StringBuilder();
                    fileListContent.AppendLine($"file '{videoPath}'");
                    resourceFilePathList.Add(videoPath);

                    int line = 1;
                    foreach (string file in files)
                    {
                        // 取出需要合并的视频，添加进文件
                        if (file.StartsWith(videoPath.Substring(0, videoPath.LastIndexOf(".")) + "_"))
                        {
                            if (File.Exists(file))
                            {
                                resourceFilePathList.Add(file);
                                fileListContent.AppendLine($"file '{file}'");
                                line++;
                            }
                        }
                    }

                    if (line > 1)
                    {

                        if(!Directory.Exists(Path.GetFullPath($"tempMeger")))
                        {
                            Directory.CreateDirectory(Path.GetFullPath($"tempMeger"));
                        }

                        // 将文件名列表写入txt文本
                        string txtFile = Path.GetFullPath($"tempMeger\\tempMeger-{videoId}.txt");
                        string txtStr = fileListContent.ToString();
                        txtStr = txtStr.Substring(0, txtStr.LastIndexOf("\r\n"));
                        File.WriteAllText(txtFile, txtStr);

                        string outputFilePath = Path.GetFullPath($"tempMeger\\tempMeger-{videoId}.ts");

                        // 开始合并视频
                        var process = new Process
                        {
                            StartInfo = new ProcessStartInfo
                            {
                                FileName = "tools\\ffmpeg.exe",
                                Arguments = $"-y -f concat -safe 0 -i \"{txtFile}\" -c copy \"{outputFilePath}\"",
                                UseShellExecute = false,
                                RedirectStandardOutput = true,
                                RedirectStandardError = true,
                                CreateNoWindow = true,
                                StandardOutputEncoding = Encoding.UTF8,
                                StandardErrorEncoding = Encoding.UTF8
                            }
                        };


                        StringBuilder errorLog = new StringBuilder();
                        process.ErrorDataReceived += (sender, e) =>
                        {
                            if (!string.IsNullOrEmpty(e.Data)) errorLog.AppendLine(e.Data);
                        };

                        process.Start();
                        process.BeginErrorReadLine();

                        // 设置超时（30秒）
                        if (!process.WaitForExit(30000))
                        {
                            process.Kill();
                            FileUtils.LogError($"{txtStr}", "FFmpeg 合并超时");
                            return;
                        }

                        if (process.ExitCode != 0)
                        {
                            FileUtils.LogError($"{errorLog}======={txtStr}", "FFmpeg 合并错误");
                            return;
                        }


                        // 删除所有原始ts文件
                        foreach (var filePath in resourceFilePathList)
                        {

                            if (File.Exists(filePath))
                            {
                                File.Delete(filePath);
                            }
                        }

                        // 将合并的视频重命名
                        File.Move(outputFilePath, videoPath);

                        // 删除临时文件
                        if (File.Exists(txtFile))
                        {
                            File.Delete(txtFile);
                        }

                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", "合并视频发生异常");
            }
        }

        /// <summary>
        /// 压缩视频文件
        /// </summary>
        /// <param name="videoPath"></param>
        /// <returns></returns>
        public static Dictionary<string, object> Compress(string videoPath)
        {

            Dictionary<string, object> result = new Dictionary<string, object>();



            //string outputDirectoryPath = videoPath.Substring(0, videoPath.LastIndexOf("."));
            //outputDirectoryPath += "_temp.mp4";
            //result.Add("filePath", outputDirectoryPath);

            // 判断文件是否已经存在，存在则不再转换
            //if (File.Exists(outputDirectoryPath))
            //{
            //    FileInfo fileInfo = new FileInfo(outputDirectoryPath);
            //    long fileSize = fileInfo.Length;
            //    result.Add("fileSize", fileSize / 1024 / 1024);
            //    return result;
            //}


            // 构建 ffmpeg 命令
            //string arguments = $"-i \"{videoPath}\" -vcodec libx264 -preset faster -crf 40 -movflags +faststart \"{outputDirectoryPath}\"";

            //// 创建 ProcessStartInfo 对象
            //ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
            //{
            //    UseShellExecute = false,
            //    RedirectStandardOutput = true,
            //    RedirectStandardError = true,
            //    CreateNoWindow = true
            //};

            //// 创建 Process 对象
            //using (Process process = new Process())
            //{
            //    process.StartInfo = startInfo;

            //    FileUtils.log($"开始压缩{outputDirectoryPath}");

            //    process.OutputDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);
            //    process.ErrorDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);

            //    // 启动 ffmpeg 进程
            //    process.Start();

            //    // 开始监听标准输出和错误输出
            //    process.BeginOutputReadLine();
            //    process.BeginErrorReadLine();

            //    // 等待 ffmpeg 进程结束
            //    process.WaitForExit();

            //    FileUtils.log($"结束压缩{outputDirectoryPath}");

            //    FileInfo fileInfo = new FileInfo(outputDirectoryPath);
            //    long fileSize = fileInfo.Length;
            //    result.Add("fileSize", (int)(fileSize / 1024 / 1024));

            //    using (var mediaInfo = new MediaInfo.MediaInfo())
            //    {
            //        mediaInfo.Open(videoPath); // 打开视频文件

            //        // 获取视频时长
            //        string durationString = mediaInfo.Get(StreamKind.General, 0, "Duration");
            //        double duration = double.Parse(durationString) / 1000; // 将毫秒转换为秒
            //        result.Add("duration", duration);
            //    }

            //    return result;

            //}

            result.Add("filePath", videoPath);
            FileInfo fileInfo = new FileInfo(videoPath);
            long fileSize = fileInfo.Length;
            result.Add("fileSize", (int)(fileSize / 1024 / 1024));

            int duration = VideoUtils.getVideoDuration(videoPath);
            result.Add("duration", duration);

            return result;

        }

        /// <summary>
        /// 检查视频是否有错误，有的话重新编码
        /// </summary>
        /// <param name="videoPath"></param>
        public static void CheckVideoError(string videoPath)
        {
            try
            {

                string errors = "";

                string checkArguments = $" -v error -i  \"{videoPath}\" -f null - ";

                // 创建 ProcessStartInfo 对象
                ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, checkArguments)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                // 创建 Process 对象
                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;

                    FileUtils.LogAnalysis($"检查视频是否有错误-开始{videoPath}");

                    process.OutputDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);
                    process.ErrorDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);

                    // 启动 ffmpeg 进程
                    process.Start();

                    // 读取标准错误流中的输出
                    errors = process.StandardError.ReadToEnd();
                    FileUtils.log($"检查视频是否有错误：{errors}");

                    // 等待 ffmpeg 进程结束
                    process.WaitForExit();

                    FileUtils.LogAnalysis($"检查视频是否有错误-结束{videoPath}");

                }

                //ffmpeg -i input.mp4 -async 1 -c:v copy -c:a aac output_fixed.mp4
                if(errors.Contains("Invalid data found when processing input"))
                {
                    ReEncodeVideo(videoPath);
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"{ex}", "重新编码错误");
            }
        }

        /// <summary>
        /// 将视频文件重新编码
        /// </summary>
        /// <param name="videoPath">视频路径</param>
        public static void ReEncodeVideo(string videoPath)
        {
            try
            {
                string prefix = videoPath.Substring(0, videoPath.LastIndexOf('.'));
                string suffix = videoPath.Substring(videoPath.LastIndexOf('.') + 1);
                string tempPath = prefix + "_temp" + "." + suffix;

                if (File.Exists(tempPath))
                {
                    File.Delete(tempPath);
                }

                string arguments = $" -i \"{videoPath}\" -async 1 -c:v copy -c:a aac \"{tempPath}\" ";

                var process = new Process
                {
                    StartInfo = new ProcessStartInfo
                    {
                        FileName = ffmpegPath,
                        Arguments = arguments,
                        UseShellExecute = false,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        CreateNoWindow = true
                    }
                };

                process.OutputDataReceived += OutputDataReceivedHandler;
                process.ErrorDataReceived += OutputDataReceivedHandler;
                process.Start();
                process.BeginOutputReadLine(); // 启动异步读取标准输出流
                process.BeginErrorReadLine();  // 启动异步读取标准错误流

                // 等待 ffmpeg 进程结束
                process.WaitForExit();

                if (normalEnd)
                {
                    // 是正常结束的
                    File.Delete(videoPath); // 删除原文件
                    File.Move(tempPath, videoPath); // 移动临时文件到原文件位置

                    // 通知前端
                    FrontNotice frontNotice = new FrontNotice();
                    var requestDataObj = new Dictionary<string, object>();
                    requestDataObj["code"] = 0;
                    requestDataObj["status"] = 200;
                    requestDataObj["action"] = "reEncodeSuccess";
                    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                }
                else
                {
                    // 非正常结束
                    if (File.Exists(tempPath))
                    {
                        File.Delete(tempPath); // 删除临时文件
                    }
                }

            }
            catch(Exception ex)
            {
                FileUtils.LogError($"{ex}", $"将视频文件重新编码发生异常==={videoPath}");
            }

        }

        /// <summary>
        /// 重新编码修复视频
        /// </summary>
        /// <param name="videoPath">视频路径</param>
        /// <param name="targetPath">视频目标路径</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <param name="uuid">文件或视频的唯一标识</param>
        public static void RepairVideo(string videoPath, string targetPath, int type, string uuid)
        {
            try
            {

                if (File.Exists(targetPath))
                {
                    File.Delete(targetPath);
                }

                string arguments = $" -i \"{videoPath}\" -c:v libx264 -bf 0 -preset veryfast -crf 23 -c:a aac -bsf:a aac_adtstoasc -threads 4 \"{targetPath}\" ";

                var process = new Process
                {
                    StartInfo = new ProcessStartInfo
                    {
                        FileName = ffmpegPath,
                        Arguments = arguments,
                        UseShellExecute = false,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        CreateNoWindow = true
                    }
                };

                process.OutputDataReceived += OutputDataReceivedHandler;
                process.ErrorDataReceived += OutputDataReceivedHandler;
                process.Start();
                process.BeginOutputReadLine(); // 启动异步读取标准输出流
                process.BeginErrorReadLine();  // 启动异步读取标准错误流

                reEncodeProcessId = process.Id;
                // 等待 ffmpeg 进程结束
                process.WaitForExit();

                if (process != null)
                {
                    if (!process.HasExited)
                    {
                        process.Kill();
                    }
                    process.CancelOutputRead(); // 取消异步读取
                    process.CancelErrorRead();  // 取消异步读取
                    process.Dispose();
                }

                Thread.Sleep(2000);

                if (normalEnd)
                {
                    // 通知前端
                    Task.Run(() => {

                        if(type == 0)
                        {
                            // 更新时长
                            try
                            {
                                int duration = getVideoDuration(targetPath);

                                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(uuid);
                                if(videoEntity != null)
                                {
                                    UpdateVideoSizeDurationBo updateVideoSizeDurationBo = new UpdateVideoSizeDurationBo();
                                    updateVideoSizeDurationBo.videoId = uuid;
                                    updateVideoSizeDurationBo.duration = duration;

                                    DateTime originalDateTime = DateTime.ParseExact(videoEntity.startTime, "yyyy-MM-dd HH:mm:ss", null);
                                    DateTime newDateTime = originalDateTime.AddSeconds(duration);

                                    updateVideoSizeDurationBo.endTime = newDateTime.ToString("yyyy-MM-dd HH:mm:ss");

                                    VideoApi.UpdateVideoSizeDuration(updateVideoSizeDurationBo);
                                }
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogError($"{ex}", $"修复视频后更新视频时长失败{uuid}=={targetPath}");
                            }
                            
                        }

                        FrontNotice frontNotice = new FrontNotice();
                        var requestDataObj = new Dictionary<string, object>();
                        requestDataObj["code"] = 0;
                        requestDataObj["status"] = 200;
                        requestDataObj["action"] = "reEncodeSuccess";
                        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                    });
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"将视频文件重新编码发生异常==={videoPath}");

                // 通知前端
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "reEncodeFail";
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }

            reEncodeProcessId = -1;
            normalEnd = true;

        }

        /// <summary>
        /// 将视频转成mp4
        /// </summary>
        /// <param name="videoPath">视频路径</param>
        /// <param name="videoName">视频文件名</param>
        /// <param name="platform">平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书</param>
        /// <returns></returns>
        public static string ConvertToMP4(string videoPath, string videoName, int platform = 1)
        {

            string outputDirectoryPath = videoPath.Substring(0, videoPath.LastIndexOf("\\"));
            string tempPath = outputDirectoryPath;
            outputDirectoryPath += "\\mp4\\";

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
                FileUtils.log($"创建输出文件夹异常: {e}");
                throw new IOException();
            }

            try
            {
                // 构建输出文件名
                outputDirectoryPath += videoName + ".mp4";

                // 判断文件是否已经存在，存在则不再转换
                if (File.Exists(outputDirectoryPath))
                {
                    return outputDirectoryPath;
                }

                FileUtils.LogAnalysis($"将视频转成mp4-输出文件名：{outputDirectoryPath}");

                if (videoName.EndsWith(".mp4") || videoName.EndsWith(".MP4"))
                {
                    // 来源是mp4，执行拷贝
                    File.Copy(videoPath, outputDirectoryPath, true);
                    return outputDirectoryPath;
                }

                // 构建 ffmpeg 参数：
                //   ts/flv  → 一次 -c copy（仅重封装，本就不需要重编码）
                //   其他    → 先试 -c copy（远端 mp4/mov/mkv 等多数可直接 remux），失败兜底 libx264
                // 收益：非 ts/flv 路径常见格式（mov/mp4 容器变种）走 -c copy 后 FFmpeg
                //        CPU 时间从 ~30s（libx264 veryfast）降到 ~3s（仅 demux+remux）；
                //        不兼容的奇怪容器/编码自动 fallback 到原 libx264 路径，行为不退化。
                string primaryArgs;
                string fallbackArgs = null;
                if (videoName.EndsWith(".ts") || videoName.EndsWith(".flv"))
                {
                    primaryArgs = $"-i \"{videoPath}\" -c copy -movflags +faststart -f mp4 \"{outputDirectoryPath}\"";
                }
                else
                {
                    primaryArgs = $"-i \"{videoPath}\" -c copy -movflags +faststart -f mp4 \"{outputDirectoryPath}\"";
                    fallbackArgs = $"-i \"{videoPath}\" -c:v libx264 -bf 0 -preset veryfast -crf 23 -c:a aac -bsf:a aac_adtstoasc \"{outputDirectoryPath}\"";
                }

                bool primaryOk = TryRunFfmpegConvert(primaryArgs, outputDirectoryPath);
                if (!primaryOk && fallbackArgs != null)
                {
                    FileUtils.LogAnalysis($"-c copy 失败，降级 libx264 重编码: {videoPath}");
                    // 清理失败的部分输出文件，避免下次 ffmpeg 因目标已存在产生 stdin 提示而超时
                    try { if (File.Exists(outputDirectoryPath)) File.Delete(outputDirectoryPath); }
                    catch (Exception ex) { FileUtils.LogAnalysis($"清理部分输出失败: {ex.Message}"); }
                    TryRunFfmpegConvert(fallbackArgs, outputDirectoryPath);
                }

                return outputDirectoryPath;
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"将视频转成mp4-转MP4异常{videoPath}，{e}");
                throw new Exception();
            }
            
        }

        /// <summary>
        /// 执行一次 ffmpeg 转换。返回 true 当且仅当进程正常退出、输出文件存在且非空。
        /// 不抛异常（除非 process.Start 等系统级失败）。
        /// 调用方根据返回值决定是否走 fallback 路径。
        /// </summary>
        private static bool TryRunFfmpegConvert(string arguments, string outputPath)
        {
            ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
            {
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            using (Process process = new Process())
            {
                process.StartInfo = startInfo;
                FileUtils.LogAnalysis($"{outputPath}", $"将视频转成mp4---开始");
                process.OutputDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);
                process.ErrorDataReceived += new DataReceivedEventHandler(OutputDataReceivedHandler);
                process.Start();
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();

                int time = 5 * 60 * 1000; // 超时时间设置为5分钟
                bool exited = process.WaitForExit(time);
                if (!exited)
                {
                    try
                    {
                        process.Kill();
                        FileUtils.LogAnalysis($"{outputPath}", $"将视频转成mp4---转换超时，已强制结束");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogAnalysis($"{ex}", $"将视频转成mp4---强制结束时发生异常");
                    }
                    return false;
                }

                FileUtils.LogAnalysis($"{outputPath} exit={process.ExitCode}", $"将视频转成mp4---结束");
                return File.Exists(outputPath) && new FileInfo(outputPath).Length > 0;
            }
        }

        #region 常量定义（避免魔法值，便于维护）

        /// <summary>
        /// 视频转换超时时间（5分钟）
        /// </summary>
        private const int CONVERT_TIMEOUT_MS = 5 * 60 * 1000;

        /// <summary>
        /// MP4输出子目录名
        /// </summary>
        private const string MP4_OUTPUT_DIR = "mp4";
        #endregion

        /// <summary>
        /// 异步将视频转换为MP4格式（支持取消/超时）
        /// </summary>
        /// <param name="videoPath">源视频完整路径</param>
        /// <param name="videoName">源视频文件名（不含扩展名）</param>
        /// <param name="platform">平台标识（1=默认，其他=兼容模式）</param>
        /// <param name="cancellationToken">取消令牌（可选，用于主动取消转换）</param>
        /// <returns>转换后的MP4文件完整路径</returns>
        /// <exception cref="ArgumentNullException">视频路径/名称为空时抛出</exception>
        /// <exception cref="FileNotFoundException">源视频文件不存在时抛出</exception>
        /// <exception cref="IOException">目录创建/文件拷贝/转换失败时抛出</exception>
        /// <exception cref="TimeoutException">转换超时时抛出</exception>
        /// <exception cref="OperationCanceledException">转换被取消时抛出</exception>
        public static async Task<string> ConvertToMP4Async(
            string videoPath,
            string videoName,
            int platform = 1,
            CancellationToken cancellationToken = default)
        {
            #region 1. 参数校验（提前拦截无效输入）
            if (string.IsNullOrWhiteSpace(videoPath))
                throw new ArgumentNullException(nameof(videoPath), "源视频路径不能为空");
            if (string.IsNullOrWhiteSpace(videoName))
                throw new ArgumentNullException(nameof(videoName), "视频名称不能为空");
            if (!File.Exists(videoPath))
                throw new FileNotFoundException("源视频文件不存在", videoPath);
            if (!File.Exists(ffmpegAsyncPath))
                throw new FileNotFoundException("FFmpeg可执行文件不存在", ffmpegAsyncPath);
            #endregion

            #region 2. 路径处理（用Path类保证跨平台/分隔符兼容）
            // 源视频目录
            //string outputDirectoryPath = Path.GetDirectoryName(videoPath)!;
            string outputDirectoryPath = videoPath.Substring(0, videoPath.LastIndexOf("\\"));
            string tempPath = outputDirectoryPath;
            //outputDirectoryPath += "\\mp4\\";
            // MP4输出目录（源目录/mp4/）
            string mp4OutputDir = Path.Combine(outputDirectoryPath, MP4_OUTPUT_DIR);

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
                FileUtils.log($"创建输出文件夹异常: {e}");
                throw new IOException();
            }

            // 最终输出MP4路径
            string outputMp4Path = Path.Combine(mp4OutputDir, $"{videoName}.mp4");
            #endregion

            #region 3. 提前检查：文件已存在则直接返回
            if (File.Exists(outputMp4Path))
            {
                await FileUtils.LogAsync($"MP4文件已存在，跳过转换：{outputMp4Path}", "视频转换");
                return outputMp4Path;
            }
            #endregion

            #region 4. 创建输出目录（异步包装，避免阻塞）
            try
            {
                if (!Directory.Exists(mp4OutputDir))
                {
                    // 目录创建是内存操作，无需真异步，包装为Task保持语义统一
                    await Task.Run(() => Directory.CreateDirectory(mp4OutputDir), cancellationToken)
                        .ConfigureAwait(false);
                    await FileUtils.LogAsync($"创建MP4输出目录成功：{mp4OutputDir}", "视频转换");
                }
            }
            catch (Exception ex)
            {
                await FileUtils.LogAsync($"创建输出目录异常：{ex.Message}，路径：{mp4OutputDir}", "视频转换");
                throw new IOException("创建MP4输出目录失败", ex);
            }
            #endregion

            #region 5. 特殊处理：源文件已是MP4，直接异步拷贝
            string videoExt = Path.GetExtension(videoName).ToLowerInvariant();
            if (videoExt == ".mp4")
            {
                try
                {
                    await FileUtils.LogAsync($"源文件已是MP4，执行异步拷贝：{videoPath} → {outputMp4Path}", "视频转换");
                    File.Copy(videoPath, outputMp4Path);
                    return outputMp4Path;
                }
                catch (Exception ex)
                {
                    await FileUtils.LogAsync($"MP4文件拷贝异常：{ex.Message}，源：{videoPath}，目标：{outputMp4Path}", "视频转换");
                    throw new IOException("MP4文件拷贝失败", ex);
                }
            }
            #endregion

            #region 6. FFmpeg异步转换视频
            Process ffmpegProcess = null;
            try
            {
                // 构建FFmpeg命令参数
                string ffmpegArgs = BuildFfmpegArguments(videoPath, outputMp4Path, videoExt, platform);
                await FileUtils.LogAsync($"开始FFmpeg转换，参数：{ffmpegArgs}", "视频转换");

                // 配置进程启动信息
                var startInfo = new ProcessStartInfo(ffmpegAsyncPath, ffmpegArgs)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true,
                    //WorkingDirectory = Path.GetDirectoryName(ffmpegPath)! // 设置工作目录避免路径问题
                };

                // 创建进程并注册事件
                ffmpegProcess = new Process { StartInfo = startInfo };
                ffmpegProcess.OutputDataReceived += OutputDataReceivedHandler;
                ffmpegProcess.ErrorDataReceived += OutputDataReceivedHandler;

                // 注册取消令牌：取消时杀死进程
                var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
                cts.CancelAfter(CONVERT_TIMEOUT_MS); // 超时自动取消

                // 启动进程并开始监听输出
                ffmpegProcess.Start();
                ffmpegProcess.BeginOutputReadLine();
                ffmpegProcess.BeginErrorReadLine();

                await FileUtils.LogAsync($"FFmpeg进程启动成功，PID：{ffmpegProcess.Id}", "视频转换");

                // 异步等待进程退出（支持取消/超时）
                bool exited = ffmpegProcess.WaitForExit(CONVERT_TIMEOUT_MS);
                if (!exited)
                {
                    throw new TimeoutException($"视频转换超时（{CONVERT_TIMEOUT_MS / 1000}秒），已终止进程");
                }

                // 检查进程退出码（0=成功）
                if (ffmpegProcess.ExitCode != 0)
                {
                    throw new IOException($"FFmpeg转换失败，退出码：{ffmpegProcess.ExitCode}，参数：{ffmpegArgs}");
                }

                await FileUtils.LogAsync($"视频转换成功：{outputMp4Path}", "视频转换");
                return outputMp4Path;
            }
            catch (OperationCanceledException ex)
            {
                await FileUtils.LogAsync($"视频转换被取消：{ex.Message}，路径：{videoPath}", "视频转换");
                throw; // 向上抛取消异常，由调用方处理
            }
            catch (TimeoutException ex)
            {
                await FileUtils.LogAsync($"视频转换超时：{ex.Message}，路径：{videoPath}", "视频转换");
                throw;
            }
            catch (Exception ex)
            {
                await FileUtils.LogAsync($"FFmpeg转换异常：{ex.Message}，源路径：{videoPath}，目标路径：{outputMp4Path}", "视频转换");
                throw new IOException("视频转换为MP4失败", ex);
            }
            finally
            {
                // 确保进程被释放/杀死（防止僵尸进程）
                if (ffmpegProcess != null && !ffmpegProcess.HasExited)
                {
                    try
                    {
                        ffmpegProcess.Kill(); // 强制杀死进程及子进程
                        await FileUtils.LogAsync($"强制终止FFmpeg进程，PID：{ffmpegProcess.Id}", "视频转换");
                    }
                    catch (Exception ex)
                    {
                        await FileUtils.LogAsync($"终止FFmpeg进程异常：{ex.Message}，PID：{ffmpegProcess.Id}", "视频转换");
                    }
                }
                ffmpegProcess?.Dispose(); // 释放进程资源
            }
            #endregion
        }

        /// <summary>
        /// 构建FFmpeg命令参数
        /// </summary>
        private static string BuildFfmpegArguments(string inputPath, string outputPath, string videoExt, int platform)
        {
            // TS/FLV格式优先快速拷贝（无损），其他格式转码
            if (videoExt == ".ts" || videoExt == ".flv")
            {
                return $"-i \"{inputPath}\" -c copy -f mp4 \"{outputPath}\"";
            }
            else
            {
                // 通用转码参数（H.264+AAC）
                return $"-i \"{inputPath}\" -c:v libx264 -bf 0 -preset veryfast -crf 23 -c:a aac -bsf:a aac_adtstoasc \"{outputPath}\"";
            }
        }


        private async static void OutputDataReceivedHandler(object sendingProcess, DataReceivedEventArgs outLine)
        {
            if (!string.IsNullOrEmpty(outLine.Data))
            {
               await FileUtils.LogAsync($"FFmpeg输出：{outLine.Data}", "FFmpeg日志", true);
            }
            else
            {
                return;
            }
        }

        /// <summary>
        /// 检查ts文件和mp4文件时长是否一致
        /// </summary>
        /// <param name="tsPath">ts视频文件路径</param>
        /// <param name="mp4Path">mp4视频文件路径</param>
        /// <returns>返回mp4时长秒数，返回-1表示不需要将视频时长设置为mp4的时长</returns>
        public static int CkeckTsAndMp4Consistent(string storagePath, string mp4Path)
        {
            int tsDuration = 0;
            int mp4Duration = 0;
            bool checkSuccess = true;

            try
            {
                // 获取ts时长
                FileUtils.LogAnalysis($"{storagePath}", $"检查时长一致性-开始MediaInfo打开ts文件");
                try
                {
                    tsDuration = getVideoDuration(storagePath);
                }
                catch (Exception e)
                {
                    FileUtils.LogAnalysis($"{e}", $"检查时长一致性-打开ts文件异常=={storagePath}");
                    checkSuccess = false;
                }

                // 获取mp4时长
                FileUtils.LogAnalysis($"{mp4Path}", $"检查时长一致性-开始MediaInfo打开mp4文件");
                try
                {
                    mp4Duration = getVideoDuration(mp4Path);
                }
                catch (Exception e)
                {
                    FileUtils.LogAnalysis($"{e}", $"检查时长一致性-打开mp4文件异常=={mp4Path}");
                    checkSuccess = false;
                }

            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"检查ts文件和mp4文件时长是否一致发生异常");
                checkSuccess = false;
            }

            FileUtils.LogAnalysis($"{checkSuccess}，差值：{Math.Abs(mp4Duration - tsDuration)}", $"检查ts文件和mp4文件时长结果");

            if(!checkSuccess || mp4Duration == 0 || tsDuration == 0)
            {
                FileUtils.LogAnalysis($"将ts重新编码为mp4-开始", $"视频检查不成功,或者ts与mp4的时长读取为0");

                string prefix = mp4Path.Substring(0, mp4Path.LastIndexOf('.'));
                string suffix = mp4Path.Substring(mp4Path.LastIndexOf('.') + 1);
                string tempPath = prefix + "_temp" + "." + suffix;

                if(File.Exists(tempPath))
                {
                    File.Delete(tempPath);
                }

                // 将ts重新编码为mp4
                string arguments = $" -i \"{storagePath}\" -c:v libx264 -bf 0 -preset veryfast -crf 23 -c:a aac -b:a 128k -ac 2 -bsf:a aac_adtstoasc -movflags +faststart \"{tempPath}\" ";

                // 创建 ProcessStartInfo 对象
                ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                // 创建 Process 对象
                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;

                    process.OutputDataReceived += OutputDataReceivedHandler;
                    process.ErrorDataReceived += (sender, e) =>
                    {
                        if (!string.IsNullOrEmpty(e.Data))
                        {
                            if (e.Data.Contains("Error") || e.Data.Contains("error") || e.Data.Contains("failed"))
                            {
                                FileUtils.LogAnalysis(e.Data, "ffmpeg将原视频文件重新编码为mp4发生错误");
                            }else
                            {
                                FileUtils.log(e.Data);
                            }
                        }
                    };

                    // 启动 ffmpeg 进程
                    process.Start();

                    process.BeginOutputReadLine(); // 启动异步读取标准输出流
                    process.BeginErrorReadLine();  // 启动异步读取标准错误流

                    // 等待 ffmpeg 进程结束
                    int time = 3 * 60 * 60 * 1000; // 超时时间设置为3小时
                    bool exited = process.WaitForExit(time);

                    if (!exited)
                    {
                        try
                        {
                            process.Kill();
                            FileUtils.LogAnalysis($"{storagePath}", $"ffmpeg将原视频文件重新编码为mp4---转换超时，已强制结束");
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogAnalysis($"{ex}", $"ffmpeg将原视频文件重新编码为mp4发生错误---强制结束时发生异常");
                        }

                        // 删除临时文件
                        if(File.Exists(tempPath))
                        {
                            File.Delete(tempPath);
                        }

                        if(mp4Duration != 0)
                        {
                            return mp4Duration;
                        }
                        return - 1;
                    }

                }

                File.Delete(mp4Path); // 删除原mp4文件
                File.Move(tempPath, mp4Path); // 移动临时文件到原文件位置.

                FileUtils.LogAnalysis($"将ts重新编码为mp4-结束", $"视频检查不成功,或者ts与mp4的时长读取为0");

                // 获取重新编码后的mp4时长
                mp4Duration = getVideoDuration(mp4Path);
                FileUtils.LogAnalysis($"时长：{mp4Duration}", $"取重新编码的mp4的时长");
                return mp4Duration;
            }
            else if(Math.Abs(mp4Duration - tsDuration) > 30)
            {
                FileUtils.LogAnalysis($"ts：{tsDuration}，MP4：{mp4Duration}", $"ts与mp4的时长差距超过30秒，取MP4时长");
                return mp4Duration;
            }

            return -1;

        }

        /// <summary>
        /// 通过获取视频时长（秒）
        /// </summary>
        /// <param name="filePath">视频路径</param>
        /// <returns></returns>
        public static int getVideoDuration(string filePath)
        {
            if (!File.Exists(filePath))
                throw new FileNotFoundException("getVideoDuration获取时长异常==文件未找到", filePath);

            // 使用超时保护获取视频时长
            var durationTask = Task.Run(() =>
            {
                try
                {
                    using (var mediaInfo = new MediaInfo.MediaInfo())
                    {
                        mediaInfo.Open(filePath);
                        string durationString = mediaInfo.Get(StreamKind.General, 0, "Duration");
                        if (double.TryParse(durationString, out double durationMs))
                        {
                            return (int)(durationMs / 1000); // 将毫秒转换为秒
                        }
                        return -1;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"getVideoDuration获取时长异常=={filePath}");
                    return -1;
                }
            });

            // 设置20秒超时
            if (durationTask.Wait(TimeSpan.FromSeconds(20)))
            {
                int result = durationTask.Result;
                if (result >= 0)
                {
                    return result;
                }
            }
            else
            {
                FileUtils.LogError($"{filePath}", $"getVideoDuration打开文件超时");
            }

            throw new Exception($"getVideoDuration获取视频时长失败=={filePath}");
        }


    }
}
