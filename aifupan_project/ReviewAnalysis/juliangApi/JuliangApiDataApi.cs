using douyin.Utils;
using Jint;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliangApi
{
    /// <summary>
    /// 巨量百应 API 调用层（无窗体，纯 HttpClient）
    /// </summary>
    public static class JuliangApiDataApi
    {
        private const string UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";

        private static readonly Dictionary<string, string> DefaultHeaders = new Dictionary<string, string>
        {
            ["accept"] = "application/json, text/plain, */*",
            ["accept-language"] = "zh-CN,zh;q=0.9",
            ["cache-control"] = "no-cache",
            ["pragma"] = "no-cache",
            ["priority"] = "u=1, i",
            ["sec-ch-ua"] = "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
            ["sec-ch-ua-mobile"] = "?0",
            ["sec-ch-ua-platform"] = "\"Windows\"",
            ["sec-fetch-dest"] = "empty",
            ["sec-fetch-mode"] = "cors",
            ["sec-fetch-site"] = "same-origin",
            ["user-agent"] = UA
        };

        #region 6 个 API 方法

        /// <summary>
        /// 专业版-直播大屏核心数据（需 a_bogus）
        /// </summary>
        public static async Task<string> LiveTalentAsync(string roomId, string cookieStr)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}"
                };

                var fp = GetFp();
                var msToken = GetMsToken();
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "index_selected", "gpm,pay_ucnt,pay_combo_cnt,watch_pay_ucnt_ratio,product_click_pay_ucnt_ratio,online_user_cnt,live_show_watch_cnt_ratio,avg_watch_duration,watch_interact_ucnt_ratio,follow_anchor_ucnt,live_show_cnt,stat_cost,real_refund_amt,watch_ucnt" },
                    { "_lid", GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };

                var aBogus = GetAB(UA, parameters);
                parameters.Add("a_bogus", aBogus);

                var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                var url = $"https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data?{queryString}";

                return await SendGetRequest(url, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"专业版大屏API异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 基础版-直播大屏数据（需 a_bogus）
        /// </summary>
        public static async Task<string> BasicLiveScreenAsync(string roomId, string cookieStr)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://compass.jinritemai.com/screen/talent/main?live_room_id={roomId}"
                };

                var fp = GetFp();
                var msToken = GetMsToken();
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "_lid", GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };

                var aBogus = GetAB(UA, parameters);
                parameters.Add("a_bogus", aBogus);

                var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                var url = $"https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/base_info?{queryString}";

                return await SendGetRequest(url, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"基础版大屏API异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// {"data":{"app_platform":"抖音","avatar_uri":"https://p3.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-avt-0015_f34d92e30a6d7206572ea28994c2a38c.jpeg?from=3067671334","is_ecom_room":true,"is_touxi_live":false,"live_app_id":2079,"live_end_ts":0,"live_start_time":"2026/04/17 10:52:25","live_start_ts":1776394345,"live_status":2,"nickname":"众航信息咨询","screen_status_info":{"can_enter":true},"server_cur_time":"2026/04/17 12:02:07","server_cur_ts":1776398527},"msg":"","st":0}
        /// </summary>
        /// <param name="roomId"></param>
        /// <param name="cookieStr"></param>
        /// <returns></returns>
        public static async Task<string> LiveBasicScreenAsync(string roomId, string cookieStr)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://compass.jinritemai.com/screen/talent/main?live_room_id={roomId}&live_app_id=1128&source=baiying_home"
                };
                var fp = GetFp();
                var msToken = GetMsToken();
                // 只使用 room_id 和 _lid 两个参数（与Python一致）
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "_lid", GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };
                var aBogus = GetAB(UA, parameters);
                parameters.Add("a_bogus", aBogus);
                var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));

                // 使用 live_base_info 端点（与Python一致）
                var url = $"https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/live_base_info?{queryString}";

                return await SendGetRequest(url, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"基础版大屏API异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 成交用户画像
        /// </summary>
        public static async Task<string> PayUserPortraitAsync(string roomId, string cookieStr)
        {
            return await UserPortraitAsync(roomId, cookieStr, "pay_user");
        }

        /// <summary>
        /// 观看用户画像
        /// </summary>
        public static async Task<string> WatchUserPortraitAsync(string roomId, string cookieStr)
        {
            return await UserPortraitAsync(roomId, cookieStr, "watch_user");
        }

        /// <summary>
        /// 流量结构分析
        /// </summary>
        public static async Task<string> FlowOrderSourceAsync(string roomId, string cookieStr)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}"
                };
                
                var fp = GetFp();
                var msToken = GetMsToken();
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "data_range", "1" },
                    { "_lid", GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };
                
                var aBogus = GetAB(UA, parameters);
                parameters.Add("a_bogus", aBogus);

                var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                var url = $"https://compass.jinritemai.com/compass_api/content_live/author/basic_live_screen/flow_order_source?{queryString}";

                return await SendGetRequest(url, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"流量分析API异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 商品列表数据
        /// </summary>
        public static async Task<string> ProductPaymentDataAsync(string roomId, string cookieStr)
        {
            try
            {
                //// 优先使用直播后接口
                string afterLiveUrl = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list_after_live";
                var result = await FetchProductListAsync(afterLiveUrl, roomId, cookieStr);

                //// 无数据时降级使用直播中接口
                //if (string.IsNullOrEmpty(result))
                //{
                    // string liveUrl = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list";
                // var result = await FetchProductListAsync(liveUrl, roomId, cookieStr);
                //}

                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"商品数据API异常: {ex.Message}", "巨量API-商品");
                return null;
            }
        }

        /// <summary>
        /// 获取商品列表（分页）
        /// </summary>
        private static async Task<string> FetchProductListAsync(string url, string roomId, string cookieStr)
        {
            try
            {
                // 构建指标选择参数
                string indexSelected = string.Join(",", new List<string>
                {
                    "product_bind_time", "market_price", "explain_cnt", "product_show_ucnt",
                    "product_click_ucnt", "product_show_click_ucnt_ratio", "product_show_pay_ucnt_ratio",
                    "product_click_pay_ucnt_ratio", "gpm", "pay_amt", "avg_max_pay_amt_min",
                    "pay_combo_cnt", "pay_cnt", "create_cnt", "create_pay_ucnt_ratio",
                    "pay_deposit_pre_order_cnt", "presale_depay_deamt", "pay_deposit_pre_order_amt",
                    "refund_cnt", "real_refund_amt", "refund_rate"
                });

                var allProducts = new JArray();
                int pageNo = 1;
                int pageSize = 20;
                int totalPages = 1;

                do
                {
                    var headers = new Dictionary<string, string>(DefaultHeaders)
                    {
                        ["referer"] = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}&live_app_id=1128"
                    };
                    
                    var fp = GetFp();
                    var msToken = GetMsToken();
                    var queryParams = new Dictionary<string, string>
                    {
                        ["index_selected"] = indexSelected,
                        ["data_range"] = "0",
                        ["room_id"] = roomId,
                        ["page_no"] = pageNo.ToString(),
                        ["page_size"] = pageSize.ToString(),
                        ["_lid"] = GetLid(),
                        [ "fp" ] = fp,
                        [ "verifyFp" ] = fp,
                        [ "msToken" ] = msToken,
                    };
                    
                    var aBogus = GetAB(UA, queryParams);
                    queryParams.Add("a_bogus", aBogus);

                    var queryString = string.Join("&", queryParams.Select(p => $"{WebUtility.UrlEncode(p.Key)}={WebUtility.UrlEncode(p.Value)}"));
                    var fullUrl = $"{url}?{queryString}";

                    var response = await SendGetRequest(fullUrl, cookieStr, headers);

                    if (string.IsNullOrEmpty(response))
                    {
                        break;
                    }

                    var jsonData = JObject.Parse(response);

                    if (jsonData["data"] is JObject dataObj && dataObj["data_result"] != null)
                    {
                        JArray dataResult = dataObj["data_result"] as JArray;
                        if (dataResult != null)
                        {
                            foreach (var item in dataResult)
                            {
                                allProducts.Add(item);
                            }
                        }

                        // 获取分页信息
                        if (pageNo == 1 && dataObj["page_result"] != null)
                        {
                            var pageResult = dataObj["page_result"];
                            int total = pageResult["total"]?.Value<int>() ?? 0;
                            totalPages = (int)Math.Ceiling((double)total / pageSize);
                        }
                    }
                    else
                    {
                        break;
                    }

                    pageNo++;
                } while (pageNo <= totalPages);

                if (allProducts.Count > 0)
                {
                    return new JObject { ["product_list"] = allProducts }.ToString();
                }

                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品列表异常: {ex.Message}", "巨量API-商品");
                return null;
            }
        }

        #endregion

        #region 千川 API 方法

        /// <summary>
        /// 获取千川账户列表
        /// </summary>
        public static async Task<string> GetQianchuanAccountListAsync(string roomId, string cookieStr)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://compass.jinritemai.com/qianchuan/check-login?roomId={roomId}&source=luopan"
                };

                var url = $"https://compass.jinritemai.com/ad/api/data/compass/get-account-list?roomId={roomId}&gfversion=1.0.1.7967";
                return await SendGetRequest(url, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川账户列表异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 获取千川全域大屏统计数据
        /// </summary>
        public static async Task<string> GetQianchuanStatDataAsync(string roomId, string aavid, string anchorId, string cookieStr)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["Content-Type"] = "application/json",
                    ["x-secsdk-csrf-token"] = "DOWNGRADE",
                    ["Origin"] = "https://qianchuan.jinritemai.com",
                    ["Referer"] = $"https://qianchuan.jinritemai.com/board-next?live_room_id={roomId}&aavid={aavid}&ad_origin=1&fromScene=luopan"
                };

                var requestBody = new
                {
                    DataSetKey = "board_roi2_overview_conf_next",
                    Metrics = new[]
                    {
                        "total_pay_order_count_realtime_for_roi2",
                        "total_live_pay_order_gpm_realtime_for_roi2",
                        "live_watch_to_pay_rate_for_roi2",
                        "total_cost_per_pay_order_realtime_for_roi2",
                        "live_online_user_count",
                        "total_show_to_watch_rate_for_roi2",
                        "live_watch_ucount_for_roi2",
                        "stat_cost_for_roi2",
                        "total_prepay_and_pay_order_realtime_roi2",
                        "total_pay_order_gmv_include_coupon_realtime_for_roi2"
                    },
                    Dimensions = new object[] { },
                    PageParams = new
                    {
                        Offset = 0,
                        Limit = -1
                    },
                    Filters = new
                    {
                        ConditionRelationshipType = 1,
                        Conditions = new[]
                        {
                            new { Field = "advertiser_id", Operator = 7, Values = new[] { aavid } },
                            new { Field = "room_id", Operator = 7, Values = new[] { roomId } },
                            new { Field = "anchor_id", Operator = 7, Values = new[] { anchorId } }
                        }
                    },
                    refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next",
                    Base = new
                    {
                        Extra = new
                        {
                            refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next"
                        }
                    }
                };

                string jsonData = JsonConvert.SerializeObject(requestBody, Formatting.None);
                var url = $"https://qianchuan.jinritemai.com/ad/api/data/v1/common/statQuery?reqFrom=commonMetricCard&aavid={aavid}&gfversion=1.0.0.294";

                return await SendPostRequest(url, jsonData, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川全域大屏数据异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        #endregion

        #region 通用请求方法

        private static async Task<string> UserPortraitAsync(string roomId, string cookieStr, string source)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}"
                };
                
                var fp = GetFp();
                var msToken = GetMsToken();
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "source", source },
                    { "_lid", GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };
                
                var aBogus = GetAB(UA, parameters);
                parameters.Add("a_bogus", aBogus);

                var queryString = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                var url = $"https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/user_portrait?{queryString}";

                return await SendGetRequest(url, cookieStr, headers);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"用户画像API({source})异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        private static async Task<string> SendGetRequest(string url, string cookieStr, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler
                {
                    UseCookies = false,
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                    AllowAutoRedirect = true
                })
                using (var client = new HttpClient(handler))
                {
                    client.Timeout = TimeSpan.FromSeconds(10);
                    var request = new HttpRequestMessage(HttpMethod.Get, url);

                    foreach (var header in headers)
                    {
                        request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                    }

                    if (!string.IsNullOrEmpty(cookieStr))
                    {
                        request.Headers.TryAddWithoutValidation("Cookie", cookieStr);
                    }

                    var response = await client.SendAsync(request);
                    var content = await response.Content.ReadAsStringAsync();

                    if (response.IsSuccessStatusCode)
                    {
                        return content;
                    }
                    else
                    {
                        FileUtils.LogRpa($"GET 请求失败：{response.StatusCode}, URL: {url}", "巨量API");
                        return null;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GET 请求异常：{ex.Message}, URL: {url}", "巨量API");
                return null;
            }
        }

        private static async Task<string> SendPostRequest(string url, string jsonData, string cookieStr, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler { UseCookies = false })
                using (var client = new HttpClient(handler))
                {
                    client.Timeout = TimeSpan.FromSeconds(10);
                    var request = new HttpRequestMessage(HttpMethod.Post, url);

                    foreach (var header in headers)
                    {
                        request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                    }

                    if (!string.IsNullOrEmpty(cookieStr))
                    {
                        request.Headers.TryAddWithoutValidation("Cookie", cookieStr);
                    }

                    request.Content = new StringContent(jsonData, Encoding.UTF8, "application/json");

                    var response = await client.SendAsync(request);
                    var content = await response.Content.ReadAsStringAsync();

                    if (response.IsSuccessStatusCode)
                    {
                        return content;
                    }
                    else
                    {
                        FileUtils.LogRpa($"POST 请求失败：{response.StatusCode}, URL: {url}", "巨量API");
                        return null;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"POST 请求异常：{ex.Message}, URL: {url}", "巨量API");
                return null;
            }
        }

        #endregion

        #region 参数生成工具（从 JuliangReachData 提取）

        /// <summary>
        /// 检查API响应是否为错误
        /// </summary>
        public static bool IsErrorResponse(string responseText)
        {
            if (string.IsNullOrWhiteSpace(responseText)) return true;
            if (responseText.Contains("\"st\":10012")) return true;
            if (responseText.Contains("\"msg\":\"当前网络不稳定，请稍后再试\"")) return true;
            if (responseText.Contains("\"msg\":\"当前视角与页面不匹配\"")) return true;
            if (responseText.Contains("\"msg\":\"当前环境存在风险，请稍后重试\"")) return true;
            if (responseText.Contains("\"msg\":\"服务器错误\"")) return true;
            if (responseText.Contains("\"msg\":\"达人没有直播间权限\",\"st\":625")) return true;
            if (responseText.Contains("\"msg\":\"无权限\"")) return true;
            return false;
        }

        /// <summary>
        /// 检查API响应是否为授权过期
        /// </summary>
        public static bool IsAuthExpired(string responseText)
        {
            if (string.IsNullOrWhiteSpace(responseText)) return false;
            var isAuthExpired = responseText.Contains("\"msg\":\"服务器错误\"") || responseText.Contains("\"msg\":\"达人没有直播间权限\",\"st\":625");
            return isAuthExpired;
        }

        public static string GetAB(string ua, Dictionary<string, string> parameters, string dataStr = "")
        {
            try
            {
                var paramStr = string.Join("&", parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                string jsFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "script\\juliang.js");

                if (!File.Exists(jsFilePath))
                    throw new FileNotFoundException("未找到juliang.js文件", jsFilePath);

                var jsCode = File.ReadAllText(jsFilePath, Encoding.UTF8);
                var engine = new Jint.Engine();
                engine.Execute(jsCode);
                return engine.Invoke("get_a_bogus", ua, paramStr, dataStr).AsString();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"生成a_bogus失败：{ex.Message}", "巨量API");
                return "";
            }
        }

        public static string GetFp()
        {
            const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            var random = new Random();
            var timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;
            var base36Time = ToBase36(timestamp).ToLower();
            var r = new char[36];
            r[8] = r[13] = r[18] = r[23] = '_';
            r[14] = '4';
            for (int o = 0; o < 36; o++)
            {
                if (r[o] == '\0')
                {
                    var i = random.Next(chars.Length);
                    r[o] = o == 19 ? chars[(3 & i) | 8] : chars[i];
                }
            }
            return $"verify_{base36Time}_{new string(r)}";
        }

        private static string ToBase36(long number)
        {
            const string base36Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            if (number == 0) return "0";
            var sb = new StringBuilder();
            while (number > 0)
            {
                sb.Insert(0, base36Chars[(int)(number % 36)]);
                number /= 36;
            }
            return sb.ToString();
        }

        public static string GetMsToken(int randomLength = 184)
        {
            const string baseStr = "ABCDEFGHIGKLMNOPQRSTUVwXYZabcdefghigklmnopqrstuVwxyz0123456789=";
            var random = new Random();
            var sb = new StringBuilder();
            for (int i = 0; i < randomLength; i++)
            {
                sb.Append(baseStr[random.Next(baseStr.Length)]);
            }
            return sb.ToString();
        }

        public static string GetLid()
        {
            var timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;
            var randomPart = new Random().Next(1000, 9999);
            return $"{timestamp.ToString().Substring(0, 5)}{randomPart}";
        }

        #endregion
    }
}
