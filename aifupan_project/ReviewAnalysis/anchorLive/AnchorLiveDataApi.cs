using douyin.Utils;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台数据 API 调用层
    /// </summary>
    public class AnchorLiveDataApi
    {
        private static readonly Dictionary<string, string> DefaultHeaders = new Dictionary<string, string>
        {
            ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36 QQBrowser/21.0.8279.400",
            ["Accept"] = "application/json, text/plain, */*",
            ["sec-ch-ua-platform"] = "\"Windows\"",
            ["sec-ch-ua"] = "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"",
            ["sec-ch-ua-mobile"] = "?0",
            ["x-appid"] = "477650",
            ["x-requested-with"] = "XMLHttpRequest",
            ["x-sub-web-id"] = "1116",
            ["x-use-bpsc"] = "1",
            ["content-type"] = "application/json",
            ["sec-fetch-site"] = "same-origin",
            ["sec-fetch-mode"] = "cors",
            ["sec-fetch-dest"] = "empty",
            ["accept-language"] = "zh-CN,zh;q=0.9"
        };

        /// <summary>
        /// 获取主播账户详情（内部调用 GetLiveStatus）
        /// </summary>
        public static async Task<Dictionary<string, string>> GetAccountDetail(Dictionary<string, string> cookies)
        {
            try
            {
                var liveStatus = await GetLiveStatus(cookies);
                if (liveStatus == null)
                    return null;

                var result = new Dictionary<string, string>
                {
                    ["current_live_room_id"] = liveStatus.ContainsKey("room_id") ? liveStatus["room_id"] : "",
                    ["room_id"] = liveStatus.ContainsKey("room_id") ? liveStatus["room_id"] : ""
                };

                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取主播账户详情异常：{ex.Message}", "主播后台 API");
                return null;
            }
        }

        /// <summary>
        /// 获取直播状态（包含roomId）
        /// </summary>
        public static async Task<Dictionary<string, string>> GetLiveStatus(Dictionary<string, string> cookies)
        {
            try
            {
                // 完全对齐 Python demo headers，不继承 DefaultHeaders
                var headers = new Dictionary<string, string>
                {
                    ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
                    ["Accept"] = "application/json, text/plain, */*",
                    ["method"] = "get",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["x-sub-web-id"] = "1116",
                    ["sec-ch-ua"] = "\"Google Chrome\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["originres"] = "true",
                    ["silentrequest"] = "true",
                    ["url"] = "/webcast/api/platform_following/outside/room/v1/live_status",
                    ["content-type"] = "application/json",
                    ["headers"] = "[object Object]",
                    ["x-appid"] = "3000",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-dest"] = "empty",
                    ["referer"] = "https://anchor.douyin.com/anchor/dashboard",
                    ["accept-language"] = "zh-CN,zh;q=0.9",
                    ["priority"] = "u=1, i"
                };

                var url = "https://anchor.douyin.com/webcast/api/platform_following/outside/room/v1/live_status";

                // 只保留 sessionid 和 sessionid_ss
                var filteredCookies = new Dictionary<string, string>();
                if (cookies != null)
                {
                    if (cookies.ContainsKey("sessionid"))
                        filteredCookies["sessionid"] = cookies["sessionid"];
                    if (cookies.ContainsKey("sessionid_ss"))
                        filteredCookies["sessionid_ss"] = cookies["sessionid_ss"];
                }

                var response = await SendGetRequest(url, filteredCookies, headers);

                FileUtils.LogRpa($"GetLiveStatus 响应: {response}", "主播后台 API");

                if (string.IsNullOrEmpty(response))
                    return null;

                var jsonData = JObject.Parse(response);
                var result = new Dictionary<string, string>();

                // 尝试从多种可能的JSON路径提取room_id
                var data = jsonData["data"];
                if (data != null)
                {
                    // 直接字段
                    if (data["room_id"] != null)
                        result["room_id"] = data["room_id"].ToString();
                    if (data["roomId"] != null)
                        result["room_id"] = data["roomId"].ToString();
                    if (data["live_status"] != null)
                        result["live_status"] = data["live_status"].ToString();
                    if (data["status"] != null)
                        result["status"] = data["status"].ToString();

                    // 可能在嵌套的room对象中
                    var room = data["room"];
                    if (room != null)
                    {
                        if (room["id"] != null)
                            result["room_id"] = room["id"].ToString();
                        if (room["id_str"] != null)
                            result["room_id"] = room["id_str"].ToString();
                        if (room["room_id"] != null)
                            result["room_id"] = room["room_id"].ToString();
                        if (room["status"] != null)
                            result["live_status"] = room["status"].ToString();
                    }
                }

                // 也检查顶层字段
                if (!result.ContainsKey("room_id"))
                {
                    if (jsonData["room_id"] != null)
                        result["room_id"] = jsonData["room_id"].ToString();
                }

                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取直播状态异常：{ex.Message}", "主播后台 API");
                return null;
            }
        }

        /// <summary>
        /// 直播大屏 - 核心数据（概览）
        /// </summary>
        public static async Task<JObject> LiveRoomOverviewV3Async(string roomId, Dictionary<string, string> cookies)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://anchor.douyin.com/anchor/dashboard?type=0&roomId={roomId}"
                };

                var queryParams = $"?roomID={roomId}";
                var url = $"https://anchor.douyin.com/anchor_pc_tinker_proxy/lego/native/webcast_api/room/replay/overview_v3{queryParams}";

                var response = await SendGetRequest(url, cookies, headers);

                if (string.IsNullOrEmpty(response))
                    return null;

                var jsonData = JObject.Parse(response);
                return jsonData;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取直播概览数据异常：{ex.Message}", "主播后台 API");
                return null;
            }
        }

        /// <summary>
        /// 直播大屏 - 流量转化（近 1 分钟停留）
        /// </summary>
        public static async Task<JObject> LiveRoomFunnelAsync(string roomId, Dictionary<string, string> cookies)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["cache-control"] = "no-cache",
                    ["pragma"] = "no-cache",
                    ["referer"] = $"https://anchor.douyin.com/anchor/dashboard?type=0&roomId={roomId}"
                };

                var url = $"https://anchor.douyin.com/webcast/data/api/v1/component/lego/webcast_api/v2/live_room/realtime_flow_data/funnel/?roomID={roomId}";

                var response = await SendGetRequest(url, cookies, headers);

                if (string.IsNullOrEmpty(response))
                    return null;

                var jsonData = JObject.Parse(response);
                return jsonData;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取流量转化数据异常：{ex.Message}", "主播后台 API");
                return null;
            }
        }

        /// <summary>
        /// 发送 GET 请求
        /// </summary>
        private static async Task<string> SendGetRequest(string url, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler { UseCookies = false })
                using (var client = new HttpClient(handler))
                {
                    var request = new HttpRequestMessage(HttpMethod.Get, url);

                    // 添加请求头
                    foreach (var header in headers)
                    {
                        request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                    }

                    // 添加 Cookie
                    if (cookies != null && cookies.Count > 0)
                    {
                        var cookieHeader = string.Join("; ", cookies.Select(kvp => $"{kvp.Key}={kvp.Value}"));
                        request.Headers.TryAddWithoutValidation("Cookie", cookieHeader);
                    }

                    var response = await client.SendAsync(request);
                    var content = await response.Content.ReadAsStringAsync();

                    if (response.IsSuccessStatusCode)
                    {
                        return content;
                    }
                    else
                    {
                        FileUtils.LogRpa($"GET 请求失败：{response.StatusCode}, URL: {url}", "主播后台 API");
                        FileUtils.LogRpa($"响应内容：{content}", "主播后台 API");
                        return null;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GET 请求异常：{ex.Message}, URL: {url}", "主播后台 API");
                return null;
            }
        }

        /// <summary>
        /// 发送 POST 请求
        /// </summary>
        private static async Task<string> SendPostRequest(string url, string data, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler { UseCookies = false })
                using (var client = new HttpClient(handler))
                {
                    var request = new HttpRequestMessage(HttpMethod.Post, url);

                    // 添加请求头
                    foreach (var header in headers)
                    {
                        request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                    }

                    // 添加 Cookie
                    if (cookies != null && cookies.Count > 0)
                    {
                        var cookieHeader = string.Join("; ", cookies.Select(kvp => $"{kvp.Key}={kvp.Value}"));
                        request.Headers.TryAddWithoutValidation("Cookie", cookieHeader);
                    }

                    request.Content = new StringContent(data, Encoding.UTF8, "application/json");
                    var response = await client.SendAsync(request);
                    var responseContent = await response.Content.ReadAsStringAsync();

                    if (response.IsSuccessStatusCode)
                    {
                        return responseContent;
                    }
                    else
                    {
                        FileUtils.LogRpa($"POST 请求失败：{response.StatusCode}, URL: {url}", "主播后台 API");
                        return null;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"POST 请求异常：{ex.Message}, URL: {url}", "主播后台 API");
                return null;
            }
        }
    }
}
