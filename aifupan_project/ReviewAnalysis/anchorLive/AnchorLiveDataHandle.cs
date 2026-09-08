using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台数据处理层
    /// </summary>
    public class AnchorLiveDataHandle
    {
        /// <summary>
        /// Cookie 文件基础路径
        /// </summary>
        private static readonly string CookieBasePath = Path.GetFullPath("dataCollect\\config");

        /// <summary>
        /// 数据保存基础路径（对齐企业号/来客结构）
        /// </summary>
        private const string anchorLiveDataCollectPath = @"dataCollect\AnchorLive";
        private const string realTimeDirectory = "realTime";
        private const string finishDirectory = "finish";

        /// <summary>
        /// 从本地获取 Cookie
        /// </summary>
        public static Dictionary<string, string> GetCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                
                if (!File.Exists(cookiePath))
                {
                    FileUtils.LogRpa($"主播后台 Cookie 文件不存在：{cookiePath}", "主播后台数据");
                    return new Dictionary<string, string>();
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<AnchorLiveCookieDto>>(cookieContent);
                
                if (cookies == null || cookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

                // 转换为 Dictionary<string, string>
                var result = new Dictionary<string, string>();
                foreach (var cookie in cookies)
                {
                    if (!string.IsNullOrEmpty(cookie.Name) && !string.IsNullOrEmpty(cookie.Value))
                    {
                        result[cookie.Name] = cookie.Value;
                    }
                }

                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取 Cookie 异常：{ex.Message}", "主播后台数据");
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 保存 Cookie 到本地
        /// </summary>
        public static void SaveCookiesToLocal(string secUid, Dictionary<string, string> cookies)
        {
            try
            {
                // 确保目录存在
                if (!Directory.Exists(CookieBasePath))
                {
                    Directory.CreateDirectory(CookieBasePath);
                }

                // 转换为 DTO 列表
                var cookieDtos = new List<AnchorLiveCookieDto>();
                foreach (var kvp in cookies)
                {
                    cookieDtos.Add(new AnchorLiveCookieDto
                    {
                        Name = kvp.Key,
                        Value = kvp.Value,
                        Domain = ".douyin.com",
                        Path = "/",
                        Secure = true,
                        HttpOnly = true
                    });
                }

                // 保存到文件
                string cookiePath = getCookiePath(secUid);
                string jsonContent = JsonConvert.SerializeObject(cookieDtos, Formatting.Indented);
                File.WriteAllText(cookiePath, jsonContent);

                FileUtils.LogRpa($"主播后台 Cookie 已保存：{cookiePath}, 数量：{cookies.Count}", "主播后台数据");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存 Cookie 异常：{ex.Message}", "主播后台数据");
            }
        }

        /// <summary>
        /// 清除本地 Cookie
        /// </summary>
        public static void ClearCookiesLocal(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (File.Exists(cookiePath))
                {
                    File.Delete(cookiePath);
                    FileUtils.LogRpa($"主播后台 Cookie 已清除：{secUid}", "主播后台数据");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"清除 Cookie 异常：{ex.Message}", "主播后台数据");
            }
        }

        /// <summary>
        /// 获取 Cookie 文件路径
        /// </summary>
        public static string getCookiePath(string secUid)
        {
            string hash = GetMd5Hash(secUid);
            return Path.Combine(CookieBasePath, $"anchorlive-{hash}");
        }

        /// <summary>
        /// 拉取主播后台数据
        /// </summary>
        public static async Task<bool> PullAnchorData(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                var cookies = GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】Cookie 为空，无法拉取数据", "主播后台数据");
                    return false;
                }

                // 1. 拉取概览数据
                var overviewData = await AnchorLiveDataApi.LiveRoomOverviewV3Async(roomId, cookies);
                if (overviewData != null)
                {
                    SaveDataToFile("overview", roomId, videoId, anchorInfo.SecUid, overviewData.ToString());
                    FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】概览数据已保存，videoId: {videoId}", "主播后台数据");
                }

                // 2. 拉取流量转化数据
                var funnelData = await AnchorLiveDataApi.LiveRoomFunnelAsync(roomId, cookies);
                if (funnelData != null)
                {
                    SaveDataToFile("funnel", roomId, videoId, anchorInfo.SecUid, funnelData.ToString());
                    FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】流量转化数据已保存，videoId: {videoId}", "主播后台数据");
                }

                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取主播后台数据异常：{ex.Message}", "主播后台数据");
                return false;
            }
        }

        /// <summary>
        /// 生成数据文件名（roomId_videoId 或 roomId）
        /// </summary>
        private static string GetDataFileName(string roomId, string videoId)
        {
            return string.IsNullOrEmpty(videoId) ? $"{roomId}.txt" : $"{roomId}_{videoId}.txt";
        }

        /// <summary>
        /// 保存数据到文件（以 roomId_videoId 命名，对齐企业号/来客结构）
        /// </summary>
        private static void SaveDataToFile(string dataType, string roomId, string videoId, string secUid, string jsonData)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("主播后台roomId为空，跳过数据保存", "主播后台数据");
                    return;
                }

                // 实时数据追加写入
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, anchorLiveDataCollectPath, realTimeDirectory, GetDataFileName(roomId, videoId));
                CheckFilePath(realTimeFileFullPath);

                var record = new
                {
                    timestamp = DateTimeOffset.Now.ToUnixTimeSeconds(),
                    time = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                    roomId = roomId,
                    videoId = videoId,
                    secUid = secUid,
                    dataType = dataType,
                    data = jsonData
                };

                string line = JsonConvert.SerializeObject(record);
                File.AppendAllText(realTimeFileFullPath, line + Environment.NewLine, Encoding.UTF8);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存数据到文件异常：{ex.Message}", "主播后台数据");
            }
        }

        /// <summary>
        /// 检查并创建文件所在目录
        /// </summary>
        private static void CheckFilePath(string filePath)
        {
            string directoryPath = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(directoryPath))
            {
                Directory.CreateDirectory(directoryPath);
            }
        }

        /// <summary>
        /// 获取 MD5 哈希
        /// </summary>
        private static string GetMd5Hash(string input)
        {
            using (var md5 = System.Security.Cryptography.MD5.Create())
            {
                var bytes = md5.ComputeHash(System.Text.Encoding.UTF8.GetBytes(input));
                var sb = new StringBuilder();
                foreach (var b in bytes)
                {
                    sb.Append(b.ToString("x2"));
                }
                return sb.ToString();
            }
        }
    }
}
