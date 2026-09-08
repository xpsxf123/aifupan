using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Websocket;

namespace ReviewAnalysis.Utils
{
    public class DownloadFileUtil
    {
        internal static readonly HttpClient SharedClient = new HttpClient(
            new HttpClientHandler
            {
                UseCookies = false,
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate
            },
            disposeHandler: false)
        { Timeout = TimeSpan.FromSeconds(30) };

        public DownloadFileUtil()
        {
        }

        /// <summary>
        /// 下载文件
        /// </summary>
        /// <param name="actionPath">示例：api/downloadFile/test/ai-html/2026/03/02/4548983440677249024.html</param>
        public async Task DownloadFile(HttpListenerRequest request, HttpListenerResponse response, string actionPath)
        {
            // 下载文件的路径
            string filePath = WebsocketDataHandle.savePath;

            try
            {
                // 截取api/downloadFile后的路径部分
                // actionPath示例: api/downloadFile/test/ai-html/2026/03/02/4548983440677249024.html
                string relativePath = actionPath.Replace("api/downloadFile/", "");

                // 拼接filePath，获取完整文件路径
                string fullFilePath = Path.Combine(filePath, relativePath);

                // 安全检查：防止路径遍历攻击
                string fullPath = Path.GetFullPath(fullFilePath);
                string basePath = Path.GetFullPath(filePath);

                if (!fullPath.StartsWith(basePath))
                {
                    throw new Exception("无效的文件路径");
                }

                // 检查文件是否存在
                if (!File.Exists(fullPath))
                {
                    response.StatusCode = 404;
                    response.ContentType = "text/html; charset=utf-8";
                    string errorMsg = "文件不存在";
                    byte[] errorBytes = Encoding.UTF8.GetBytes(errorMsg);
                    response.OutputStream.Write(errorBytes, 0, errorBytes.Length);
                    return;
                }

                // 读取文件
                string conetnt = File.ReadAllText(fullPath, Encoding.UTF8);
                byte[] fileBytes = Encoding.UTF8.GetBytes(conetnt);

                // 设置响应头
                response.StatusCode = 200;
                response.ContentType = "text/html; charset=utf-8";
                response.AddHeader("Content-Disposition", $"attachment; filename=\"{Path.GetFileName(fullPath)}\"");
                response.AddHeader("Content-Length", fileBytes.Length.ToString());


                response.ContentLength64 = fileBytes.Length;
                //返回响应
                using (Stream output = response.OutputStream)
                {
                    await output.WriteAsync(fileBytes, 0, fileBytes.Length);
                    await output.FlushAsync();
                }
            }
            catch (Exception ex)
            {
                response.StatusCode = 400;
                response.ContentType = "text/html; charset=utf-8";
                byte[] errorBytes = Encoding.UTF8.GetBytes(ex.Message);
                using (Stream output = response.OutputStream)
                {
                    await output.WriteAsync(errorBytes, 0, errorBytes.Length);
                    await output.FlushAsync();
                }

                // response.OutputStream.Close();
                // response.Close();
            }
        }

        /// <summary>
        /// 使用 FFmpeg 流拷贝下载视频
        /// </summary>
        /// <param name="videoUrl">视频URL（m3u8/flv/mp4等）</param>
        /// <param name="outputPath">输出文件完整路径</param>
        /// <param name="referer">Referer请求头</param>
        /// <param name="timeoutMs">总超时毫秒数</param>
        /// <param name="startOffsetSec">时间窗口起始偏移（秒），0=从头开始</param>
        /// <param name="segmentDurationSec">时间窗口长度（秒），0=下载全部</param>
        /// <param name="stallTimeoutMs">停滞超时毫秒数：文件大小在此时间内无变化则判定卡死</param>
        /// <returns>true=下载成功</returns>
        public static async Task<bool> DownloadVideoWithFfmpeg(
            string videoUrl, string outputPath, string referer,
            int timeoutMs, long startOffsetSec = 0, long segmentDurationSec = 0,
            int stallTimeoutMs = 60000)
        {
            try
            {
                string ffmpegPath = Path.GetFullPath("tools\\ffmpeg.exe");
                string timeWindow = "";
                if (segmentDurationSec > 0)
                    timeWindow = $" -ss {startOffsetSec} -t {segmentDurationSec}";
                string arguments = $" -headers \"referer:{referer}\" -y -i \"{videoUrl}\"{timeWindow} -c copy -f mpegts \"{outputPath}\"";

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

                    FileUtils.log($"开始执行FFmpeg下载: {arguments}");

                    process.Start();

                    // 异步读取 stdout/stderr，防止缓冲区满导致 ffmpeg 阻塞
                    Task<string> outputTask = process.StandardOutput.ReadToEndAsync();
                    Task<string> errorTask = process.StandardError.ReadToEndAsync();

                    DateTime startTime = DateTime.Now;
                    DateTime lastProgressTime = DateTime.Now;
                    long lastSize = 0;
                    const int pollIntervalMs = 3000;

                    while (!process.HasExited)
                    {
                        await Task.Delay(pollIntervalMs);

                        // 检查下载文件大小是否有变化
                        if (File.Exists(outputPath))
                        {
                            long currentSize = new FileInfo(outputPath).Length;
                            if (currentSize != lastSize)
                            {
                                lastSize = currentSize;
                                lastProgressTime = DateTime.Now;
                            }
                        }

                        double elapsedMs = (DateTime.Now - startTime).TotalMilliseconds;
                        double stallMs = (DateTime.Now - lastProgressTime).TotalMilliseconds;

                        // 停滞检测：超过 stallTimeoutMs 无进度
                        if (stallMs > stallTimeoutMs)
                        {
                            try { process.Kill(); } catch { }
                            FileUtils.LogError(
                                $"FFmpeg下载停滞（{stallTimeoutMs / 1000}秒无进度，已下载{lastSize}字节）: {videoUrl}",
                                "DownloadVideoWithFfmpeg");
                            return false;
                        }

                        // 总超时检测
                        if (elapsedMs > timeoutMs)
                        {
                            try { process.Kill(); } catch { }
                            FileUtils.LogError(
                                $"FFmpeg下载超时（{timeoutMs / 1000}秒）: {videoUrl}",
                                "DownloadVideoWithFfmpeg");
                            return false;
                        }
                    }

                    // 进程已退出，等待流读取完成
                    await Task.WhenAll(outputTask, errorTask);

                    if (process.ExitCode == 0 && File.Exists(outputPath) && new FileInfo(outputPath).Length > 0)
                    {
                        FileUtils.log($"FFmpeg下载成功: {outputPath}");
                        return true;
                    }
                    else
                    {
                        string error = errorTask.Result;
                        FileUtils.LogError($"FFmpeg下载失败，退出码: {process.ExitCode}, 错误信息: {error}", "DownloadVideoWithFfmpeg");
                        return false;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"FFmpeg下载异常: {ex.Message}", "DownloadVideoWithFfmpeg");
                return false;
            }
        }

        /// <summary>
        /// 多线程分段下载视频（m3u8 → 解析 ts 分段 → 时间窗口过滤 → 并行下载 → 合并为一个文件）
        /// </summary>
        /// <param name="m3u8Url">m3u8 播放列表地址</param>
        /// <param name="outputPath">输出文件完整路径（如 D:/video_temp.ts）</param>
        /// <param name="referer">HTTP Referer 请求头，用于通过 CDN 鉴权</param>
        /// <param name="timeoutMs">总超时毫秒数，超时后取消所有下载任务</param>
        /// <param name="startOffsetSec">时间窗口起始偏移（秒），0=从头开始</param>
        /// <param name="segmentDurationSec">时间窗口长度（秒），0=下载全部</param>
        /// <param name="maxConcurrency">最大并行下载数，默认 8</param>
        /// <param name="segmentRetry">每个分片失败后的重试次数，默认 2（即最多尝试 3 次）</param>
        /// <returns>
        /// null  = m3u8 中没有切片地址（非分段流），调用方应回退到 ffmpeg 下载
        /// true  = 全部分段下载并合并成功
        /// false = 有切片地址但下载/合并过程中失败，调用方不应回退，直接报失败
        /// </returns>
        public static async Task<bool?> TryDownloadVideoSegmentedAsync(
            string m3u8Url, string outputPath, string referer,
            int timeoutMs, long startOffsetSec = 0, long segmentDurationSec = 0,
            int maxConcurrency = 8, int segmentRetry = 2)
        {
            string tempDir = outputPath + "_segments";

            try
            {
                ServicePointManager.DefaultConnectionLimit = maxConcurrency;

                var allSegments = await FetchAndParseM3u8Async(m3u8Url, referer);
                if (allSegments.Count == 0)
                    return null;

                // 按时间窗口过滤分片（包含整个边界分片，不做精确裁剪）
                List<M3u8SegmentTimingInfo> filteredSegments;
                if (segmentDurationSec > 0)
                {
                    var endOffsetSec = startOffsetSec + segmentDurationSec;
                    filteredSegments = allSegments
                        .Where(seg => seg.EndOffsetSec > startOffsetSec && seg.StartOffsetSec < endOffsetSec)
                        .ToList();

                    if (filteredSegments.Count == 0)
                    {
                        FileUtils.LogAnalysis(
                            $"时间窗口无重叠分片: [{startOffsetSec}, {endOffsetSec}]",
                            "DownloadVideoSegmented");
                        return false;
                    }
                }
                else
                {
                    filteredSegments = allSegments;
                }

                FileUtils.LogAnalysis(
                    $"分段下载开始: {filteredSegments.Count}/{allSegments.Count} 个分段, maxConcurrency={maxConcurrency}",
                    "DownloadVideoSegmented");

                if (!Directory.Exists(tempDir))
                    Directory.CreateDirectory(tempDir);

                var failedIndices = new ConcurrentBag<int>();
                using (var semaphore = new SemaphoreSlim(maxConcurrency, maxConcurrency))
                {
                    var cts = new CancellationTokenSource();
                    // 按原始 Index 偏移重新编号：过滤后的分片从 0 开始写入 tempDir
                    var indexedSegments = filteredSegments
                        .Select((seg, newIdx) => new { seg, newIdx })
                        .ToList();

                    var tasks = indexedSegments.Select(item =>
                    {
                        var seg = item.seg;
                        var newIdx = item.newIdx;
                        return Task.Run(async () =>
                        {
                            await semaphore.WaitAsync(cts.Token);
                            try
                            {
                                bool ok = await DownloadSegmentWithRetryAsync(
                                    seg.Url, tempDir, newIdx, referer, segmentRetry, cts.Token);
                                if (!ok)
                                    failedIndices.Add(newIdx);
                            }
                            finally
                            {
                                semaphore.Release();
                            }
                        });
                    }).ToArray();

                    var allDone = Task.WhenAll(tasks);
                    var timeout = Task.Delay(timeoutMs, cts.Token);

                    if (await Task.WhenAny(allDone, timeout) == timeout)
                    {
                        cts.Cancel();
                        FileUtils.LogAnalysis(
                            $"分段下载超时 ({timeoutMs}ms), 已取消剩余任务",
                            "DownloadVideoSegmented");
                        return false;
                    }
                }

                if (failedIndices.Count > 0)
                {
                    FileUtils.LogAnalysis(
                        $"分段下载失败: {failedIndices.Count}/{filteredSegments.Count} 个分段失败",
                        "DownloadVideoSegmented");
                    return false;
                }

                FileUtils.LogAnalysis("分段下载完成，开始合并", "DownloadVideoSegmented");
                return MergeTsFiles(tempDir, filteredSegments.Count, outputPath);
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"分段下载异常: {ex.Message}", "DownloadVideoSegmented");
                return false;
            }
            finally
            {
                try { if (Directory.Exists(tempDir)) Directory.Delete(tempDir, true); }
                catch { }
            }
        }

        /// <summary>
        /// 下载单个 ts 分片，失败自动重试
        /// </summary>
        /// <param name="url">ts 分片下载地址</param>
        /// <param name="tempDir">临时目录</param>
        /// <param name="index">分片序号，用于生成文件名 {index:D5}.ts</param>
        /// <param name="referer">HTTP Referer 请求头</param>
        /// <param name="maxRetry">最大重试次数</param>
        /// <param name="ct">取消令牌，外部超时时取消</param>
        /// <returns>true=下载成功，false=重试耗尽仍失败</returns>
        private static async Task<bool> DownloadSegmentWithRetryAsync(
            string url, string tempDir, int index, string referer,
            int maxRetry, CancellationToken ct)
        {
            string filePath = Path.Combine(tempDir, $"{index:D5}.ts");

            for (int attempt = 0; attempt <= maxRetry; attempt++)
            {
                ct.ThrowIfCancellationRequested();

                try
                {
                    var request = new HttpRequestMessage(HttpMethod.Get, url);
                    request.Headers.TryAddWithoutValidation("Referer", referer);

                    // ResponseHeadersRead: 不等 body 收完就流式读
                    using (var response = await SharedClient.SendAsync(
                        request, HttpCompletionOption.ResponseHeadersRead, ct))
                    {
                        if (response.IsSuccessStatusCode)
                        {
                            // 4MB 大块流式写入：减少 I/O 次数，提升磁盘吞吐
                            using (var fileStream = new FileStream(
                                filePath, FileMode.Create, FileAccess.Write, FileShare.None,
                                bufferSize: 4194304, useAsync: true))
                            using (var netStream = await response.Content.ReadAsStreamAsync())
                            {
                                await netStream.CopyToAsync(fileStream, 4194304);
                                return true;
                            }
                        }
                    }
                }
                catch (OperationCanceledException)
                {
                    throw;
                }
                catch (Exception)
                {
                    // retry after delay
                }

                if (attempt < maxRetry)
                    await Task.Delay(2000, ct);
            }

            FileUtils.LogAnalysis($"分段 {index} 下载失败 (已重试 {maxRetry} 次): {url}", "DownloadVideoSegmented");
            return false;
        }

        /// <summary>
        /// 获取 m3u8 内容并解析所有 ts 分片地址及其时长（来自 #EXTINF 标签）
        /// </summary>
        /// <param name="m3u8Url">m3u8 播放列表地址</param>
        /// <param name="referer">HTTP Referer 请求头</param>
        /// <returns>
        /// 分片时间信息列表（含 URL + 累计时长偏移）
        /// 返回空列表 = m3u8 请求失败 / 非 #EXTM3U 格式 / 没有 ts 分片
        /// </returns>
        private static async Task<List<M3u8SegmentTimingInfo>> FetchAndParseM3u8Async(
            string m3u8Url, string referer)
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, m3u8Url);
                request.Headers.TryAddWithoutValidation("Referer", referer);

                using (var response = await SharedClient.SendAsync(request))
                {
                    if (!response.IsSuccessStatusCode)
                        return new List<M3u8SegmentTimingInfo>();

                    string text = await response.Content.ReadAsStringAsync();

                    if (string.IsNullOrEmpty(text))
                        return new List<M3u8SegmentTimingInfo>();

                    if (!text.TrimStart().StartsWith("#EXTM3U"))
                        return new List<M3u8SegmentTimingInfo>();

                    var lines = text.Split('\n');
                    var baseUri = new Uri(m3u8Url);
                    var segments = new List<M3u8SegmentTimingInfo>();
                    double cumulativeTime = 0;
                    double currentDuration = 0;

                    foreach (var rawLine in lines)
                    {
                        string line = rawLine.Trim();

                        if (line.StartsWith("#EXTINF:"))
                        {
                            var match = Regex.Match(line, @"#EXTINF:([\d.]+)");
                            if (match.Success)
                                currentDuration = double.Parse(match.Groups[1].Value,
                                    System.Globalization.CultureInfo.InvariantCulture);
                        }
                        else if (!string.IsNullOrEmpty(line) && !line.StartsWith("#"))
                        {
                            string url;
                            if (line.StartsWith("http://") || line.StartsWith("https://"))
                                url = line;
                            else
                                url = new Uri(baseUri, line).AbsoluteUri;

                            segments.Add(new M3u8SegmentTimingInfo
                            {
                                Index = segments.Count,
                                Url = url,
                                DurationSec = currentDuration,
                                StartOffsetSec = cumulativeTime,
                                EndOffsetSec = cumulativeTime + currentDuration,
                            });
                            cumulativeTime += currentDuration;
                            currentDuration = 0;
                        }
                    }

                    return segments;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"解析m3u8异常: {ex.Message}", "FetchAndParseM3u8");
                return new List<M3u8SegmentTimingInfo>();
            }
        }

        private static bool MergeTsFiles(
            string tempDir, int totalCount, string outputPath)
        {
            try
            {
                using (var outStream = new FileStream(outputPath,
                    FileMode.Create, FileAccess.Write, FileShare.None))
                {
                    for (int i = 0; i < totalCount; i++)
                    {
                        string filePath = Path.Combine(tempDir, $"{i:D5}.ts");

                        if (!File.Exists(filePath))
                        {
                            FileUtils.LogRpa(
                                $"缺失分片 {i:D5}.ts，合并可能不完整",
                                "DownloadVideoSegmented");
                            continue;
                        }

                        byte[] bytes = File.ReadAllBytes(filePath);
                        outStream.Write(bytes, 0, bytes.Length);
                    }
                }

                if (File.Exists(outputPath) && new FileInfo(outputPath).Length > 0)
                    return true;

                FileUtils.LogRpa("合并后文件为空或不存在", "DownloadVideoSegmented");
                return false;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"合并分片异常: {ex.Message}", "DownloadVideoSegmented");
                return false;
            }
        }
    }
}