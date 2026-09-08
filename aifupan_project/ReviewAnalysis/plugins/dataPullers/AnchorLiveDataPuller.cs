using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.anchorLive;
using ReviewAnalysis.Model;
using douyin.Utils;

namespace ReviewAnalysis.plugins.dataPullers
{
    /// <summary>
    /// 主播后台数据拉取器
    /// 职责：调用主播后台API获取数据，返回原始JSON
    /// </summary>
    public class AnchorLiveDataPuller : BaseDataPuller
    {
        public AnchorLiveDataPuller() : base("anchorLive", "主播后台") { }

        /// <summary>
        /// 拉取主播后台数据
        /// </summary>
        public override async Task<string> PullDataAsync(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("roomId为空，无法拉取主播后台数据", _platformName);
                    return null;
                }

                var cookies = GetCookies(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"Cookie获取失败，主播【{anchorInfo?.AnchorName}】", _platformName);
                    return null;
                }

                FileUtils.LogRpa($"开始拉取主播后台数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", _platformName);

                // 并行拉取概览数据和流量转化数据
                var overviewTask = CallApiWithRetry(async () =>
                {
                    var result = await AnchorLiveDataApi.LiveRoomOverviewV3Async(roomId, cookies);
                    return result?.ToString();
                });

                var funnelTask = CallApiWithRetry(async () =>
                {
                    var result = await AnchorLiveDataApi.LiveRoomFunnelAsync(roomId, cookies);
                    return result?.ToString();
                });

                await Task.WhenAll(overviewTask, funnelTask);

                var overviewData = await overviewTask;
                var funnelData = await funnelTask;

                // 合并结果
                var mergedData = new JObject
                {
                    ["roomId"] = roomId,
                    ["videoId"] = videoId,
                    ["collectTime"] = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                    ["data"] = new JObject()
                };

                if (!string.IsNullOrEmpty(overviewData))
                {
                    mergedData["data"]["overview"] = JObject.Parse(overviewData);
                }

                if (!string.IsNullOrEmpty(funnelData))
                {
                    mergedData["data"]["funnel"] = JObject.Parse(funnelData);
                }

                string json = JsonConvert.SerializeObject(mergedData);

                FileUtils.LogRpa($"主播后台数据拉取完成，roomId={roomId}", _platformName);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取主播后台数据异常: {ex.Message}", _platformName);
                return null;
            }
        }

        /// <summary>
        /// 获取Cookie（内联实现）
        /// </summary>
        protected override Dictionary<string, string> GetCookies(string secUid)
        {
            try
            {
                return GetAnchorLiveCookiesFromLocal(secUid) ?? new Dictionary<string, string>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从本地获取主播后台Cookie
        /// </summary>
        private Dictionary<string, string> GetAnchorLiveCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = GetAnchorLiveCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return new Dictionary<string, string>();
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<AnchorLiveCookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

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
                FileUtils.LogRpa($"从本地获取主播后台Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 获取主播后台Cookie路径
        /// </summary>
        private string GetAnchorLiveCookiePath(string secUid)
        {
            string hash = GetMd5Hash(secUid);
            return Path.Combine(Environment.CurrentDirectory, "dataCollect", "config", $"anchorlive-{hash}");
        }

        /// <summary>
        /// 计算MD5哈希
        /// </summary>
        private string GetMd5Hash(string input)
        {
            using (MD5 md5 = MD5.Create())
            {
                byte[] inputBytes = Encoding.UTF8.GetBytes(input);
                byte[] hashBytes = md5.ComputeHash(inputBytes);
                return BitConverter.ToString(hashBytes).Replace("-", "").ToLower();
            }
        }

        /// <summary>
        /// 主播后台Cookie DTO
        /// </summary>
        private class AnchorLiveCookieDto
        {
            public string Name { get; set; }
            public string Value { get; set; }
            public string Domain { get; set; }
            public string Path { get; set; }
        }
    }
}
