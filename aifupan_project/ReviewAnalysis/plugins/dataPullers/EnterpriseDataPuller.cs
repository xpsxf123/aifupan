using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.enterprise;
using ReviewAnalysis.Model;
using douyin.Utils;

namespace ReviewAnalysis.plugins.dataPullers
{
    /// <summary>
    /// 企业号数据拉取器
    /// 职责：调用企业号API获取数据，返回原始JSON
    /// </summary>
    public class EnterpriseDataPuller : BaseDataPuller
    {
        public EnterpriseDataPuller() : base("enterprise", "企业号") { }

        /// <summary>
        /// 拉取企业号数据
        /// </summary>
        public override async Task<string> PullDataAsync(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("roomId为空，无法拉取企业号数据", _platformName);
                    return null;
                }

                var cookies = GetCookies(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"Cookie获取失败，主播【{anchorInfo?.AnchorName}】", _platformName);
                    return null;
                }

                FileUtils.LogRpa($"开始拉取企业号数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", _platformName);

                // 获取直播大屏数据
                var liveData = await CallApiWithRetry(async () =>
                {
                    var result = await EnterpriseDataApi.GetLiveData(cookies, roomId);
                    return result != null && result.Count > 0 ? JsonConvert.SerializeObject(result) : null;
                });

                // 合并结果
                var mergedData = new JObject
                {
                    ["roomId"] = roomId,
                    ["videoId"] = videoId,
                    ["collectTime"] = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                    ["data"] = new JObject()
                };

                if (!string.IsNullOrEmpty(liveData))
                {
                    mergedData["data"]["liveScreenData"] = JObject.Parse(liveData);
                }

                string json = JsonConvert.SerializeObject(mergedData);

                FileUtils.LogRpa($"企业号数据拉取完成，roomId={roomId}", _platformName);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取企业号数据异常: {ex.Message}", _platformName);
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
                return GetEnterpriseCookiesFromLocal(secUid) ?? new Dictionary<string, string>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从本地获取企业号Cookie
        /// </summary>
        private Dictionary<string, string> GetEnterpriseCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = GetEnterpriseCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return new Dictionary<string, string>();
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<EnterpriseCookieDto>>(cookieContent);

                if (cookies == null || cookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

                return cookies.ToDictionary(c => c.name, c => c.value);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取企业号Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 获取企业号Cookie路径
        /// </summary>
        private string GetEnterpriseCookiePath(string secUid)
        {
            string directory = Path.Combine(Environment.CurrentDirectory, "dataCollect", "config", "enterprise");
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
            string hash = GetMd5Hash(secUid);
            return Path.Combine(directory, $"enterprise-{hash}.json");
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
        /// 企业号Cookie DTO
        /// </summary>
        private class EnterpriseCookieDto
        {
            public string name { get; set; }
            public string value { get; set; }
            public string domain { get; set; }
            public string path { get; set; }
            public long expires { get; set; }
            public bool secure { get; set; }
            public bool httpOnly { get; set; }
        }
    }
}
