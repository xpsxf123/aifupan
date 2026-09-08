using Newtonsoft.Json.Linq;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.juliangApi;

namespace ReviewAnalysis.Bll.VideoPull.Juliang
{
    /// <summary>
    /// 巨量罗盘 - 直播回放视频下载
    ///
    /// 流程：罗盘 API 获取 m3u8 回放地址 → 磁盘检查 → ffmpeg 流拷贝下载 → 格式检测 → 重命名
    /// </summary>
    public class JuliangVideoDownloader
    {
        private const string CompassM3u8ApiUrl =
            "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/review_video_link";

        private const int DefaultDownloadTimeoutMs = 30 * 60 * 1000;
        private const int StallTimeoutMs = 60 * 1000;
        private const long MinFreeSpaceBytes = 100L * 1024 * 1024;

        /// <summary>
        /// 下载巨量直播间回放视频到本地
        /// </summary>
        /// <param name="roomId">直播间 room_id（对应 video.batchNumber）</param>
        /// <param name="secUid">主播 sec_uid</param>
        /// <param name="saveDirectory">保存目录</param>
        /// <param name="videoName">视频文件名（不含扩展名）</param>
        /// <param name="startOffsetSec">时间窗口起始偏移（秒），0=从头下载</param>
        /// <param name="segmentDurationSec">时间窗口长度（秒），0=下载整场</param>
        /// <returns>(success, localFilePath, errorReason)</returns>
        public async Task<(bool success, string localPath, string error)> DownloadAsync(
            string roomId, string secUid, string saveDirectory, string videoName,
            long startOffsetSec = 0, long segmentDurationSec = 0)
        {
            try
            {
                // 1. 获取 m3u8 播放地址
                string m3u8Url = await FetchM3u8Url(roomId, secUid);
                if (string.IsNullOrEmpty(m3u8Url))
                    return (false, null, "获取视频下载地址失败");

                // 2. 磁盘空间检查
                string driveLetter = Path.GetPathRoot(saveDirectory);
                if (string.IsNullOrEmpty(driveLetter))
                    driveLetter = saveDirectory.Substring(0, 1);
                DriveInfo drive = new DriveInfo(driveLetter.Substring(0, 1));
                if (!drive.IsReady)
                    return (false, null, "磁盘不存在");
                if (drive.AvailableFreeSpace < MinFreeSpaceBytes)
                    return (false, null, "磁盘空间不足");

                // 3. 确保目录存在
                if (!Directory.Exists(saveDirectory))
                    Directory.CreateDirectory(saveDirectory);

                // 4. 下载视频（优先多线程分段下载，无切片地址则回退 ffmpeg）
                //    segmentedResult:
                //      null  = m3u8 中无切片 → 回退 ffmpeg 单线程下载
                //      true  = 分段下载 + 合并成功
                //      false = 有切片但下载失败 → 直接报错，不再重试
                string tempPath = Path.Combine(saveDirectory, $"{videoName}_temp.ts");
                string referer = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}";
                
                FileUtils.LogAnalysis($"开始下载巨量的直播视频", "DownloadAsync");

                bool? segmentedResult = await DownloadFileUtil.TryDownloadVideoSegmentedAsync(
                    m3u8Url: m3u8Url,
                    outputPath: tempPath,
                    referer: referer,
                    timeoutMs: DefaultDownloadTimeoutMs,
                    startOffsetSec: startOffsetSec,
                    segmentDurationSec: segmentDurationSec,
                    maxConcurrency: 8,       // 最多 8 个分片同时下载
                    segmentRetry: 1);        // 每个分片失败后额外重试 1 次

                bool downloadOk;

                if (segmentedResult == null)
                {
                    // 无切片地址 → 回退 ffmpeg
                    downloadOk = await DownloadFileUtil.DownloadVideoWithFfmpeg(
                        m3u8Url, tempPath, referer, DefaultDownloadTimeoutMs,
                        startOffsetSec, segmentDurationSec, StallTimeoutMs);
                }
                else
                {
                    downloadOk = segmentedResult.Value;
                }
                
                FileUtils.LogAnalysis($"完成下载巨量的直播视频 downloadOk = {segmentedResult}", "DownloadAsync");

                if (!downloadOk)
                {
                    DeleteFileSafe(tempPath);
                    return (false, null, "视频下载失败");
                }

                // 5. 检测实际视频格式
                // string format = await DetectFormat(tempPath);
                string format = "ts";
                if (string.IsNullOrEmpty(format))
                {
                    DeleteFileSafe(tempPath);
                    return (false, null, "视频格式检测失败");
                }

                // 6. 重命名为正确扩展名
                string finalPath = Path.Combine(saveDirectory, $"{videoName}.{format}");
                try
                {
                    if (File.Exists(finalPath))
                        File.Delete(finalPath);
                    File.Move(tempPath, finalPath);
                }
                catch
                {
                    DeleteFileSafe(tempPath);
                    return (false, null, "视频文件重命名失败");
                }

                FileUtils.LogAnalysis($"巨量视频下载成功: {finalPath}", "JuliangVideoDownload");
                return (true, finalPath, null);
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"巨量视频下载异常: {ex}", "JuliangVideoDownload");
                return (false, null, "下载异常");
            }
        }

        /// <summary>
        /// 从巨量罗盘 API 获取直播回放 m3u8 地址
        /// </summary>
        private async Task<string> FetchM3u8Url(string roomId, string secUid)
        {
            try
            {
                string cookieStr = BuildCookieString(secUid);
                if (string.IsNullOrEmpty(cookieStr))
                {
                    FileUtils.LogRpa($"巨量Cookie无效, secUid={secUid}", "FetchM3u8Url");
                    return null;
                }

                var fp = JuliangApiDataApi.GetFp();
                var msToken = JuliangApiDataApi.GetMsToken();
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "_lid", JuliangApiDataApi.GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };
                
                var aBogus = JuliangApiDataApi.GetAB(UA, parameters);
                parameters.Add("a_bogus", aBogus);
                
                var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                string url = $"{CompassM3u8ApiUrl}?{queryString}";
                var headers = BuildCompassHeaders(roomId);

                using (var handler = new HttpClientHandler
                {
                    UseCookies = false,
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                    AllowAutoRedirect = true
                })
                using (var client = new HttpClient(handler))
                {
                    var request = new HttpRequestMessage(HttpMethod.Get, url);
                    foreach (var h in headers)
                        request.Headers.TryAddWithoutValidation(h.Key, h.Value);
                    request.Headers.TryAddWithoutValidation("Cookie", cookieStr);

                    var response = await client.SendAsync(request);
                    var content = await response.Content.ReadAsStringAsync();

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.LogRpa(
                            $"FetchM3u8Url HTTP {(int)response.StatusCode}, roomId={roomId}",
                            "FetchM3u8Url");
                        return null;
                    }

                    JObject json = JObject.Parse(content);
                    int st = json.Value<int?>("st") ?? -1;
                    if (st != 0)
                    {
                        FileUtils.LogRpa(
                            $"FetchM3u8Url API st={st}, msg={json.Value<string>("msg")}, roomId={roomId}",
                            "FetchM3u8Url");
                        return null;
                    }

                    string resultUrl = json["data"]?["url"]?.Value<string>();
                    if (string.IsNullOrEmpty(resultUrl))
                    {
                        FileUtils.LogRpa(
                            $"FetchM3u8Url data.url 为空, roomId={roomId}",
                            "FetchM3u8Url");
                        return null;
                    }

                    return resultUrl;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FetchM3u8Url 异常: {ex.Message}", "FetchM3u8Url");
                return null;
            }
        }

        /// <summary>
        /// 用 ffmpeg 检测视频容器格式
        /// </summary>
        /// <returns>"ts" / "mp4" / null</returns>
        private static async Task<string> DetectFormat(string filePath)
        {
            try
            {
                string ffmpegPath = Path.GetFullPath("tools\\ffmpeg.exe");
                string arguments = $" -i \"{filePath}\" -f null -";

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
                    process.Start();

                    Task<string> outputTask = process.StandardOutput.ReadToEndAsync();
                    Task<string> errorTask = process.StandardError.ReadToEndAsync();
                    Task timeoutTask = Task.Delay(30000);
                    Task ioTask = Task.WhenAll(outputTask, errorTask);

                    if (await Task.WhenAny(ioTask, timeoutTask) == timeoutTask)
                    {
                        try { process.Kill(); } catch { }
                        FileUtils.LogRpa($"DetectFormat 超时: {filePath}", "DetectFormat");
                        return null;
                    }

                    process.WaitForExit();
                    string stderr = errorTask.Result;

                    // 从 stderr 解析容器格式: "Input #0, mpegts, from '...'"
                    int inputIdx = stderr.IndexOf("Input #0,");
                    if (inputIdx < 0)
                        return null;

                    int fromIdx = stderr.IndexOf(", from", inputIdx);
                    if (fromIdx < 0)
                        return null;

                    string formatStr = stderr.Substring(inputIdx + 9, fromIdx - inputIdx - 9).Trim();

                    if (formatStr.Equals("mpegts", StringComparison.OrdinalIgnoreCase))
                        return "ts";
                    if (formatStr.Contains("mp4") || formatStr.Equals("mov,mp4,m4a,3gp,3g2,mj2", StringComparison.OrdinalIgnoreCase))
                        return "mp4";

                    FileUtils.LogRpa($"DetectFormat 未知格式: {formatStr}, file={filePath}", "DetectFormat");
                    return null;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"DetectFormat 异常: {ex.Message}", "DetectFormat");
                return null;
            }
        }

        /// <summary>
        /// 读本地巨量 Cookie 文件，返回 "key1=value1; key2=value2" 格式
        /// </summary>
        internal static string BuildCookieString(string secUid)
        {
            string cookiePath = juliang.JuliangUtils.getCookiePath(secUid);
            if (!File.Exists(cookiePath))
            {
                FileUtils.LogRpa($"巨量Cookie文件不存在: {cookiePath}", "JuliangVideoDownload");
                return null;
            }

            var helper = new juliang.CookiePersistenceHelper(cookiePath);
            var cookies = helper.LoadCefCookiesFromLocal(false);
            if (cookies == null || cookies.Count == 0)
            {
                FileUtils.LogRpa($"巨量Cookie文件为空: {cookiePath}", "JuliangVideoDownload");
                return null;
            }

            return string.Join("; ", cookies.Select(c => $"{c.Name}={c.Value}"));
        }

        public static string UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36";

        /// <summary>
        /// 浏览器模拟 Headers（compass 域名）
        /// </summary>
        internal static Dictionary<string, string> BuildCompassHeaders(string roomId)
        {
            return new Dictionary<string, string>
            {
                ["accept"]            = "application/json, text/plain, */*",
                ["accept-language"]   = "zh-CN,zh;q=0.9,en;q=0.8",
                ["cache-control"]     = "no-cache",
                ["pragma"]            = "no-cache",
                ["priority"]          = "u=1, i",
                ["referer"]           = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}",
                ["sec-ch-ua"]         = "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"",
                ["sec-ch-ua-mobile"]  = "?0",
                ["sec-ch-ua-platform"] = "\"Windows\"",
                ["sec-fetch-dest"]    = "empty",
                ["sec-fetch-mode"]    = "cors",
                ["sec-fetch-site"]    = "same-origin",
                ["user-agent"]        = UA
            };
        }

        private static void DeleteFileSafe(string path)
        {
            try { if (File.Exists(path)) File.Delete(path); } catch { }
        }
    }
}
