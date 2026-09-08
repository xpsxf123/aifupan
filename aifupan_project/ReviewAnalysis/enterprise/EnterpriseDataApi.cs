using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using douyin.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号数据API调用层
    /// </summary>
    public class EnterpriseDataApi
    {
        private static readonly Dictionary<string, string> DefaultHeaders = new Dictionary<string, string>
        {
            ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            ["Accept"] = "*/*",
            ["Accept-Language"] = "zh-CN,zh;q=0.9",
            ["Cache-Control"] = "no-cache",
            ["Content-Type"] = "application/json;charset=utf-8",
            ["Origin"] = "https://leads.cluerich.com",
            ["Sec-CH-UA"] = "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"",
            ["Sec-CH-UA-Mobile"] = "?0",
            ["Sec-CH-UA-Platform"] = "\"Windows\"",
            ["Sec-Fetch-Dest"] = "empty",
            ["Sec-Fetch-Mode"] = "cors",
            ["Sec-Fetch-Site"] = "same-origin",
            ["X-Edition"] = "leads",
            ["X-Request-By"] = "fetch",
            ["X-Client-TZ"] = "Asia/Shanghai"
        };

        /// <summary>
        /// 字段名称映射（API字段名 -> 中文名称）
        /// </summary>
        private static readonly Dictionary<string, string> IndexItemMapping = new Dictionary<string, string>
        {
            // 核心数据
            ["lp_screen_live_avg_watch_duration"] = "人均观看时长",
            ["lp_screen_live_fans_avg_watch_duration"] = "粉丝停留",
            ["lp_screen_clue_uv"] = "全场景线索人数",
            ["lp_screen_live_clue_convert_ratio"] = "线索转化率",
            ["lp_screen_msg_conversation_count"] = "私信人数",
            ["lp_screen_longterm_msg_clue_uv"] = "私信长效转化",
            ["lp_screen_live_user_realtime"] = "实时在线人数",
            ["lp_screen_uv_with_preview"] = "看过",
            ["lp_screen_card_clue_uv"] = "表单提交人数",
            ["lp_screen_ad_biz_wechat_add_count"] = "企业微信添加数",
            ["lp_screen_ad_biz_wechat_cost"] = "加微信成本",
            ["lp_screen_ad_form_count"] = "表单提交数",
            ["lp_screen_ad_form_cost"] = "表单成本",
            ["lp_screen_live_watch_uv"] = "场观",
            ["lp_screen_live_fans_watch_ratio"] = "场观粉丝占比",
            ["lp_screen_live_enter_ratio"] = "曝光进入率",
            ["lp_screen_live_fans_enter_rate_by_room"] = "粉丝",
            ["lp_screen_live_max_watch_uv_by_minute"] = "最高在线人数",
            ["lp_screen_live_avg_online_uv_by_room"] = "平均在线人数",
            ["lp_screen_live_stat_cost"] = "广告消耗",
            ["lp_screen_clue_cost"] = "线索成本",
            ["lp_screen_live_icon_click_count"] = "小风车点击次数",
            ["lp_screen_live_icon_click_rate"] = "小风车点击率",
            ["lp_screen_live_follow_uv"] = "涨粉量",
            ["lp_screen_live_follow_ratio"] = "关注率",
            ["lp_screen_live_share_ratio"] = "分享率",
            ["lp_screen_live_share_uv"] = "分享人数",
            ["lp_screen_live_like_ratio"] = "点赞率",
            ["lp_screen_live_like_uv"] = "点赞人数",
            ["lp_screen_live_comment_ratio"] = "评论率",
            ["lp_screen_live_comment_uv"] = "评论人数",
            ["lp_screen_live_interaction_ratio"] = "互动率",
            ["lp_screen_live_interaction_uv_count"] = "互动人数",
            ["lp_screen_live_show_count"] = "曝光次数",
            ["lp_screen_live_fans_show_rate_by_room"] = "粉丝占比",
            ["lp_screen_live_watch_gt_1min_count"] = ">1分钟观看人次",
            ["lp_screen_live_watch_count"] = "观看次数",
            ["lp_screen_live_share_count"] = "分享次数",
            ["lp_screen_live_like_count"] = "点赞次数",
            ["lp_screen_live_comment_count"] = "评论次数",
            ["lp_screen_live_interaction_count"] = "互动次数",
            ["lp_screen_live_fans_club_join_uv"] = "加粉丝团人数",
            ["lp_screen_live_fans_club_join_uv_ratio"] = "加团率",
            ["lp_screen_live_clue_business_card_click_count"] = "卡片点击次数",
            ["lp_screen_live_clue_business_card_click_rate"] = "卡片点击率",
            ["lp_screen_live_gift_count"] = "打赏次数",
            ["lp_screen_live_gift_amount"] = "打赏金额",
            ["live_dislike_count"] = "不感兴趣次数",
            ["live_dislike_uv_by_room"] = "不感兴趣人数",
            ["lp_screen_live_show_uv"] = "曝光人数",
            ["live_duration"] = "直播时长",
            ["lp_screen_live_clue_business_card_show_count"] = "卡片曝光次数"
        };

        /// <summary>
        /// 获取直播大屏核心数据
        /// </summary>
        /// <param name="cookies">Cookie字典（需要sessionid）</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="startTimeMs">开始时间（毫秒时间戳）</param>
        /// <param name="endTimeMs">结束时间（毫秒时间戳）</param>
        public static async Task<Dictionary<string, string>> GetLiveScreenOverview(Dictionary<string, string> cookies, string roomId, long startTimeMs, long endTimeMs)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["Referer"] = $"https://leads.cluerich.com/pc/analysis/live-screen?room_id={roomId}&fullscreen=0"
                };

                var url = "https://leads.cluerich.com/bff/statistic/live-screen/overview";

                var requestData = new
                {
                    startTimeMs = startTimeMs,
                    endTimeMs = endTimeMs,
                    roomId = roomId
                };

                var response = await SendPostRequest(url, requestData, cookies, headers);

                if (string.IsNullOrEmpty(response))
                    return null;

                var jsonData = JObject.Parse(response);
                if (jsonData["data"] == null || jsonData["data"]["statRows"] == null)
                    return null;

                var statRows = jsonData["data"]["statRows"] as JArray;
                if (statRows == null || statRows.Count == 0)
                    return null;

                var firstRow = statRows[0];
                var metrics = firstRow["metrics"] as JObject;
                var fields = firstRow["fields"] as JObject;

                // 合并 metrics 和 fields
                var allData = new Dictionary<string, string>();
                if (metrics != null)
                {
                    foreach (var prop in metrics.Properties())
                    {
                        allData[prop.Name] = prop.Value?.ToString();
                    }
                }
                if (fields != null)
                {
                    foreach (var prop in fields.Properties())
                    {
                        allData[prop.Name] = prop.Value?.ToString();
                    }
                }

                // 转换为中文字段名
                var result = new Dictionary<string, string>();
                foreach (var mapping in IndexItemMapping)
                {
                    if (allData.ContainsKey(mapping.Key))
                    {
                        result[mapping.Value] = allData[mapping.Key];
                    }
                }

                // 同时保留原始字段名
                foreach (var item in allData)
                {
                    if (!result.ContainsKey(item.Key))
                    {
                        result[item.Key] = item.Value;
                    }
                }

                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取企业号直播大屏数据异常: {ex.Message}", "企业号数据API");
                return null;
            }
        }

        /// <summary>
        /// 获取开播时间戳（毫秒）
        /// </summary>
        public static async Task<long> GetLiveCreateTime(Dictionary<string, string> cookies, string roomId)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["Referer"] = "https://leads.cluerich.com/livefe/edouyin/live/current"
                };

                var url = $"https://leads.cluerich.com/live_console/data/convert_data?auth_type=0&room_id={roomId}";
                var response = await SendGetRequest(url, cookies, headers);

                FileUtils.LogRpa($"GetLiveCreateTime 响应: {response}", "企业号数据API");

                if (string.IsNullOrEmpty(response))
                    return 0;

                var jsonData = JObject.Parse(response);
                var createTimeStr = jsonData["data"]?["create_time"]?.ToString();

                if (string.IsNullOrEmpty(createTimeStr))
                {
                    FileUtils.LogRpa($"开播时间字段为空, roomId={roomId}", "企业号数据API");
                    return 0;
                }

                // 兼容 "yyyy.MM.dd HH:mm:ss" 和 "yyyy-MM-dd HH:mm:ss" 两种格式
                DateTime dt;
                if (createTimeStr.Contains("."))
                {
                    dt = DateTime.ParseExact(createTimeStr, "yyyy.MM.dd HH:mm:ss", System.Globalization.CultureInfo.InvariantCulture);
                }
                else
                {
                    dt = DateTime.ParseExact(createTimeStr, "yyyy-MM-dd HH:mm:ss", System.Globalization.CultureInfo.InvariantCulture);
                }

                long timestamp = new DateTimeOffset(dt).ToUnixTimeMilliseconds();
                FileUtils.LogRpa($"获取到开播时间: {createTimeStr} -> {timestamp}", "企业号数据API");
                return timestamp;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取开播时间异常: {ex.Message}", "企业号数据API");
                return 0;
            }
        }

        /// <summary>
        /// 获取直播大屏数据（简化版，使用真实开播时间）
        /// </summary>
        public static async Task<Dictionary<string, string>> GetLiveData(Dictionary<string, string> cookies, string roomId)
        {
            long startTimeMs = await GetLiveCreateTime(cookies, roomId);
            if (startTimeMs <= 0)
            {
                FileUtils.LogRpa("获取开播时间失败，无法拉取直播数据", "企业号数据API");
                return null;
            }
            long endTimeMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            return await GetLiveScreenOverview(cookies, roomId, startTimeMs, endTimeMs);
        }

        /// <summary>
        /// 更新企业号数据到服务器
        /// </summary>
        public static string updateEnterpriseEngine(EnterpriseGatherDataEntity enterpriseGatherDataEntity)
        {
            return HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/enterpriseData/updateEnterpriseEngine", enterpriseGatherDataEntity);
        }

        /// <summary>
        /// 生成安全随机字符串（大小写字母+数字，24位）
        /// </summary>
        private static string GenerateSecureRandomString(int length = 24)
        {
            const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            using (var rng = new RNGCryptoServiceProvider())
            {
                var bytes = new byte[length];
                rng.GetBytes(bytes);
                var result = new char[length];
                for (int i = 0; i < length; i++)
                {
                    result[i] = chars[bytes[i] % chars.Length];
                }
                return new string(result);
            }
        }

        /// <summary>
        /// 确保Cookie中有feiyu_csrf_token，并返回其值用于x-csrftoken header
        /// </summary>
        private static string EnsureCsrfToken(Dictionary<string, string> cookies)
        {
            if (cookies == null) return GenerateSecureRandomString();

            // 优先使用已有的feiyu_csrf_token
            if (cookies.ContainsKey("feiyu_csrf_token") && !string.IsNullOrEmpty(cookies["feiyu_csrf_token"]))
            {
                return cookies["feiyu_csrf_token"];
            }

            // 不存在则生成并注入到cookies
            string token = GenerateSecureRandomString();
            cookies["feiyu_csrf_token"] = token;
            FileUtils.LogRpa($"生成feiyu_csrf_token并注入Cookie", "企业号数据API");
            return token;
        }

        /// <summary>
        /// 发送GET请求
        /// </summary>
        private static async Task<string> SendGetRequest(string url, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler())
                {
                    handler.UseCookies = false;
                    using (var client = new HttpClient(handler))
                    {
                        // 设置x-csrftoken header（从feiyu_csrf_token cookie获取）
                        string csrfToken = EnsureCsrfToken(cookies);
                        headers["x-csrftoken"] = csrfToken;

                        // 添加请求头
                        foreach (var header in headers)
                        {
                            client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                        }

                        // 添加Cookie
                        if (cookies != null && cookies.Count > 0)
                        {
                            var cookieString = string.Join("; ", cookies.Select(c => $"{c.Key}={c.Value}"));
                            client.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", cookieString);
                        }

                        var response = await client.GetAsync(url);
                        response.EnsureSuccessStatusCode();
                        return await response.Content.ReadAsStringAsync();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GET请求异常: {ex.Message}", "企业号数据API");
                return null;
            }
        }

        /// <summary>
        /// 发送POST请求
        /// </summary>
        private static async Task<string> SendPostRequest(string url, object data, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler())
                {
                    handler.UseCookies = false;
                    using (var client = new HttpClient(handler))
                    {
                        // 设置x-csrftoken header（从feiyu_csrf_token cookie获取）
                        string csrfToken = EnsureCsrfToken(cookies);
                        headers["x-csrftoken"] = csrfToken;

                        FileUtils.LogRpa($"企业号POST请求: {url}, x-csrftoken: {csrfToken.Substring(0, Math.Min(8, csrfToken.Length))}...", "企业号数据API");

                        foreach (var header in headers)
                        {
                            client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                        }

                        if (cookies != null && cookies.Count > 0)
                        {
                            var cookieString = string.Join("; ", cookies.Select(c => $"{c.Key}={c.Value}"));
                            client.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", cookieString);
                        }

                        var jsonData = JsonConvert.SerializeObject(data);
                        var content = new StringContent(jsonData, Encoding.UTF8, "application/json");
                        var response = await client.PostAsync(url, content);
                        return await response.Content.ReadAsStringAsync();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"POST请求异常: {ex.Message}", "企业号数据API");
                return null;
            }
        }
    }
}
