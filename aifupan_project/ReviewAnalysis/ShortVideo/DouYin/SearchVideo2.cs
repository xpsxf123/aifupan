using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Web;
using douyin.Utils;
using Jint;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class SearchVideo2
    {
        private static int chromeVersion = 130;

        /// <summary>
        /// 调用抖音搜索视频接口，手动释放资源
        /// </summary>
        /// <param name="keyword"></param>
        /// <param name="videoNum"></param>
        /// <returns></returns>
        public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] next = null)
        {
            SearchVideo2 searchVideo = null;
            try
            {
                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
                next = next ?? new int[] { 50, 100 };
                searchVideo = new SearchVideo2();
                List<DouYinVideoInfo> list = null;
                for (int i = 0; i < 3; i++)
                {
                    list = searchVideo.startSearchVideo(keyword, videoNum, videoNum * 2, next, true);
                    if (list != null && list.Count > 0)
                    {
                        break;
                    }
                    FileUtils.log("----------没有查询到数据---------");
                }
                if (list == null || list.Count == 0)
                {
                    FileUtils.log("爆款请求三次还是没有数据");
                }
                return list;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"Message = {ex.Message}, StackTrace = {ex.StackTrace}", "执行抖音搜索视频报错");
            }
            return new List<DouYinVideoInfo>();
        }


        /// <summary>
        /// 开始搜索视频
        /// </summary>
        /// <param name="_keyword"></param>
        /// <param name="videoCount"></param>
        /// <param name="maxCount"></param>
        /// <param name="nextTime"></param>
        /// <param name="versionAdd"></param>
        /// <returns></returns>
        public List<DouYinVideoInfo> startSearchVideo(string _keyword, int videoCount, int maxCount = 300, int[] nextTime = null, bool versionAdd = false)
        {
            List<DouYinVideoInfo> items = new List<DouYinVideoInfo>();
            var currentNow = DateTime.Now;
            var random = new Random();
            string RandChoice(params string[] options) => options[random.Next(options.Length)];
            int RandInt(int min, int max) => random.Next(min, max + 1);
            List<int> resolution = getResolution();
            int currentPage = 1;
            bool shouldExit = false;
            var apiUrl = "https://www.douyin.com/aweme/v1/web/search/item/";
            int countProxy = 0;
            int maxProxy = 10;
            int count = 0;

            string version = getChromeVersion(versionAdd);

            string ua = $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version}.0.0.0 Safari/537.36";

            FileUtils.log($"chromeVersion = {version}");

            string searchId = null;
            var sortType = "0"; // 排序方式 0:综合排序, 1:最多点赞, 2:最新发布
            var publishTime = "180"; // 发布时间 0:不限, 1:一天内, 7:一周内, 180:半年内
            var filterDuration = ""; // 视频时长

            var cookiesRes = getCookieContainer(_keyword, ua, null);

            while (!shouldExit && currentPage < 20)
            {
                Dictionary<string, string> headers = new Dictionary<string, string>()
                {
                    { "User-Agent", ua},
                    { "Accept", "application/json, text/plain, */*"},
                    { "referer", $"https://www.douyin.com/jingxuan/search/{HttpUtility.UrlEncode(_keyword)}?aid={Guid.NewGuid().ToString()}&type=general"}
                };

                Dictionary<string, string> parameters = new Dictionary<string, string>()
                {
                    { "device_platform", "webapp" },
                    { "aid", "6383" },
                    { "channel", "channel_pc_web" },
                    { "search_channel", "aweme_video_web" },
                    { "enable_history", "1" },
                    { "keyword", _keyword },
                    { "search_source", "switch_tab" },
                    { "query_correct_type", "1" },
                    { "is_filter_search", "0" },
                    { "from_group_id", "" },
                    { "disable_rs", "1" },
                    { "offset", ((currentPage - 1) * 25).ToString() },
                    { "count", "25" },
                    { "need_filter_settings", currentPage == 1 ? "1" : "0" },
                    { "list_type", "" },
                    { "update_version_code", "170400" },
                    { "pc_client_type", "1" },
                    { "pc_libra_divert", "Windows" },
                    { "support_h265", "1" },
                    { "support_dash", "1" },
                    { "cpu_core_num", RandChoice("4", "8", "16") },
                    { "version_code", "170400" },
                    { "version_name", "17.4.0" },
                    { "cookie_enabled", "true" },
                    { "screen_width", resolution[0].ToString() },
                    { "screen_height", resolution[1].ToString() },
                    { "browser_language", RandChoice("zh-CN", "en") },
                    { "browser_platform", "Win32" },
                    { "browser_name", "Chrome" },
                    { "browser_version", getBrowserVersion(ua) },
                    { "browser_online", "true" },
                    { "engine_name", "Blink" },
                    { "engine_version", getBrowserVersion(ua) },
                    { "os_name", "Windows" },
                    { "os_version", "10" },
                    { "device_memory", RandChoice("4", "8", "16") },
                    { "platform", "PC" },
                    { "downlink", RandInt(5, 50).ToString() },
                    { "effective_type", RandChoice("4g", "wifi") },
                    { "round_trip_time", RandInt(50, 200).ToString() }
                };

                // search_id
                if (currentPage > 1 && !string.IsNullOrEmpty(searchId))
                {
                    parameters.Add("search_id", searchId);
                }
                // 设置筛选条件
                parameters["publish_time"] = publishTime;
                parameters["is_filter_search"] = sortType;

                // 设置
                string a_bogus = getAb(ua, parameters);
                if (!string.IsNullOrEmpty(a_bogus))
                {
                    parameters["a_bogus"] = a_bogus;
                }

                var queryString = string.Join("&", parameters.Select(kv => $"{kv.Key}={HttpUtility.UrlEncode(kv.Value)}"));
                var requestUrl = $"{apiUrl}?{queryString}";


                using (HttpClientHandler handler = new HttpClientHandler())
                using (HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Get, requestUrl))
                using (HttpClient httpClient = new HttpClient(handler))
                {
                    if (cookiesRes.cookieContainer != null)
                    {
                        //CookieContainer cookieContainer = new CookieContainer();
                        //foreach (var item in cookies)
                        //{
                        //    cookieContainer.Add(new Cookie(item.Key, item.Value));
                        //}
                        handler.CookieContainer = cookiesRes.cookieContainer;

                    }

                    foreach (var header in headers)
                    {
                        request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                    }

                    try
                    {
                        // 发送请求并确保成功
                        HttpResponseMessage response = httpClient.SendAsync(request).Result;
                        response.EnsureSuccessStatusCode();

                        string responseContent = response.Content.ReadAsStringAsync().Result;
                        if (responseContent.Length < 10)
                        {
                            FileUtils.log($"出现风控 videoCount = {videoCount}, currentPage = {currentPage}, responseContent.Length < 10， responseContent = {responseContent}", "搜爆款-被风控");
                            shouldExit = true;
                            continue;
                        }

                        //FileUtils.log(responseContent);
                        // 读取JSON内容并解析
                        //using var jsonDoc = JsonDocument.Parse(responseContent);
                        //var root = jsonDoc.RootElement;
                        dynamic root = JsonConvert.DeserializeObject(responseContent); // 替换 JsonDocument.Parse 

                        if (root.search_nil_info != null)
                        {
                            FileUtils.log($"出现风控 {(currentPage == 1 ? "准备使用代理" : "不是第一页不使用代理")} videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款-被风控-使用代理{countProxy}次数");
                            FileUtils.log($"出现风控 - {root?.search_nil_info?.search_nil_type ?? ""}");
                            shouldExit = true;
                            continue;
                        }
                        var data = root.data;
                        if ((root?.status_code ?? -1) == 0 && data.Type == JTokenType.Array)
                        {
                            // 处理 search_id（动态属性访问）
                            if (currentPage == 1 && root.extra?.logid != null)
                            {
                                searchId = root.extra.logid.ToString();  // 自动处理类型转换 
                                FileUtils.log($"search_id: {searchId}");
                            }

                            // 处理 has_more
                            bool hasMore = IsTrueOrOnePerf(root?.has_more?.ToString() ?? null);
                            FileUtils.log(hasMore ? "还有下一页" : "没有下一页");

                            // 处理数据
                            foreach (dynamic item in root.data)
                            {
                                count++;
                                DouYinVideoInfo temp = item?.aweme_info?.ToObject<DouYinVideoInfo>() ?? null;
                                if (!string.IsNullOrEmpty(temp?.video?.play_addr?.file_hash ?? ""))
                                {
                                    temp.desc = ShortVideoUtils.formatDesc(temp.desc);
                                    if ((temp?.video?.duration ?? 0) > 0)
                                    {
                                        temp.video.duration = ShortVideoUtils.formatDuration(temp?.video?.duration ?? 0);
                                    }

                                    items.Add(temp);
                                }

                                if (items.Count >= videoCount || count >= maxCount)
                                {
                                    shouldExit = true;
                                    break;
                                }
                            }

                            if (!hasMore)
                            {
                                shouldExit = true;
                            }
                        }
                        else
                        {
                            FileUtils.log($"API返回状态码异常或数据无效 videoCount = {videoCount}， responseContent = {responseContent}", "搜爆款-code或者data数据问题");
                            shouldExit = true;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.log($"请求错误： videoCount = {videoCount}， Message = {ex.Message}", "搜爆款-请求错误");

                        shouldExit = true;
                    }
                }


                int next = random.Next(nextTime[0], nextTime[1]);
                Thread.Sleep(next);
                currentPage++;
            }
            // 去重
            List<DouYinVideoInfo> result = items.GroupBy(item => item.aweme_id).Select(group => group.First()).ToList();

            FileUtils.log($"搜爆款共爬取{currentPage - 1}页，总计{count}个作品, 总视频{items.Count}个，总图文{count - items.Count}个，去重后视频{result?.Count ?? 0}个, 一共用时{(DateTime.Now - currentNow).TotalMilliseconds}ms", "搜爆款-统计");
            FileUtils.log($"搜爆款共爬取{currentPage - 1}页，总计{count}个作品, 总视频{items.Count}个，总图文{count - items.Count}个，去重后视频{result?.Count ?? 0}个, 一共用时{(DateTime.Now - currentNow).TotalMilliseconds}ms");
            return result;

        }


        /// <summary>
        /// 获取cookies
        /// </summary>
        /// <param name="_keyword"></param>
        /// <param name="ua"></param>
        /// <param name="proxyIpVo"></param>
        /// <returns></returns>
        public (Dictionary<string, string> cookiesMap, CookieContainer cookieContainer) getCookieContainer(string _keyword, string ua, ProxyIpVo proxyIpVo)
        {
            CookieContainer cookieContainer = new CookieContainer();
            var cookies = new Dictionary<string, string>();
            var encodedKeyword = HttpUtility.UrlEncode(_keyword);
            var url = $"https://www.douyin.com/search/{encodedKeyword}?type=video";

            HttpClientHandler handler = null;
            HttpClient httpClient = null;
            try
            {
                handler = new HttpClientHandler
                {
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                    UseCookies = true,
                    CookieContainer = cookieContainer,
                    AllowAutoRedirect = true,
                    UseDefaultCredentials = false,
                    Proxy = null,
                    UseProxy = false
                };

                // 设置代理
                if (proxyIpVo != null && !string.IsNullOrEmpty(proxyIpVo.ip) && !string.IsNullOrEmpty(proxyIpVo.port))
                {
                    // 设置代理
                    WebProxy webProxy = new WebProxy($"{proxyIpVo.ip}:{proxyIpVo.port}");
                    if (!string.IsNullOrEmpty(proxyIpVo.proxyUsername) && !string.IsNullOrEmpty(proxyIpVo.proxyPassword))
                    {
                        webProxy.Credentials = new NetworkCredential(proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);
                    }

                    handler.Proxy = webProxy;
                    handler.UseProxy = true;
                }

                httpClient = new HttpClient(handler);
                // 第一次请求 - 获取初始Cookie，包括__ac_nonce
                FileUtils.log("第一次请求获取初始Cookie...");
                httpClient.DefaultRequestHeaders.Add("User-Agent", ua);
                httpClient.DefaultRequestHeaders.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                httpClient.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
                httpClient.DefaultRequestHeaders.Add("sec-fetch-site", "none");
                httpClient.DefaultRequestHeaders.Add("sec-fetch-mode", "navigate");
                httpClient.DefaultRequestHeaders.Add("sec-fetch-user", "?1");
                httpClient.DefaultRequestHeaders.Add("sec-fetch-dest", "document");
                httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
                // 对可能包含非法字符或非标准头，使用 TryAddWithoutValidation
                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("priority", "u=0, i");

                var firstResponse = httpClient.GetAsync(url).Result;
                firstResponse.EnsureSuccessStatusCode();

                // 从CookieContainer中提取所有Cookie
                ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);

                if (!cookies.ContainsKey("__ac_nonce"))
                {
                    FileUtils.log("警告: 未获取到__ac_nonce Cookie，尝试从响应头提取");
                    ExtractCookiesFromHeaders(firstResponse.Headers, cookieContainer, cookies);
                }

                // 如果仍然没有__ac_nonce，重试一次
                if (!cookies.ContainsKey("__ac_nonce"))
                {
                    FileUtils.log("__ac_nonce获取失败，重试一次...");
                    Thread.Sleep(1000);
                    var retryResponse = httpClient.GetAsync(url).Result;
                    ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
                    ExtractCookiesFromHeaders(retryResponse.Headers, cookieContainer, cookies);
                }

                if (!cookies.TryGetValue("__ac_nonce", out var acNonce))
                {
                    FileUtils.log("错误: 无法获取__ac_nonce Cookie");
                    return (cookies, cookieContainer);
                }

                // 生成__ac_signature
                var acSignature = GetAcSignature(ua, acNonce);
                cookies["__ac_signature"] = acSignature;
                cookies["__ac_referer"] = "__ac_blank";

                // 添加生成的签名到CookieContainer
                cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_signature", acSignature));
                cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_referer", "__ac_blank"));

                // 第二次请求 - 尝试获取ttwid
                FileUtils.log("第二次请求尝试获取ttwid...");
                httpClient.DefaultRequestHeaders.Referrer = new Uri(url);

                var secondResponse = httpClient.GetAsync(url).Result;
                secondResponse.EnsureSuccessStatusCode();

                // 再次从容器中提取所有Cookie
                ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);

                // 检查是否获取到ttwid，未获取到则尝试第三次请求
                if (!cookies.ContainsKey("ttwid"))
                {
                    FileUtils.log("警告: 未获取到ttwid Cookie，尝试第三次请求...");
                    Thread.Sleep(1500);
                    var thirdResponse = httpClient.GetAsync(url).Result;
                    ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
                }

                // 添加额外需要的Cookie
                cookies["x-web-secsdk-uid"] = Guid.NewGuid().ToString();
                cookies["csrf_session_id"] = GenerateRandomHexString(16);
                cookies["SEARCH_RESULT_LIST_TYPE"] = "%22single%22";
                cookies["home_can_add_dy_2_desktop"] = "%220%22";
                cookies["hevc_supported"] = "true";

                // 输出最终获取的Cookie信息
                FileUtils.log("\n初始化cookies信息:");
                //foreach (var cookie in cookies)
                //{
                //    FileUtils.log($"=================={cookie.Key}={cookie.Value}==================");
                //}
            }
            catch (Exception ex)
            {
                FileUtils.log($"获取Cookie时出错: {ex.Message}");
            }
            finally
            {
                httpClient?.Dispose();
            }

            return (cookies, cookieContainer);
        }

        private string getChromeVersion(bool add = false)
        {
            if (add)
            {
                chromeVersion++;
            }
            if (chromeVersion >= 138)
            {
                chromeVersion = 131;
            }
            return $"{chromeVersion}";
        }

        private string getAb(string ua, Dictionary<string, string> parameters)
        {
            try
            {
                var engine = new Engine();
                engine.Execute(DouYinUtils.getAbJs()); // 编译 JS 代码

                if (parameters == null || parameters.Count == 0)
                    return "";

                // 对每个键值对进行 URL 编码后拼接
                var encodedParts = parameters.Select(pair =>
                    $"{HttpUtility.UrlEncode(pair.Key)}={HttpUtility.UrlEncode(pair.Value)}");

                string paramsStr = string.Join("&", encodedParts);

                // 调用函数并传递参数（ua, paramsStr, dataStr）
                var result = engine.Invoke("get_a_bogus", ua, paramsStr, "");

                // 4. 返回结果（转换为字符串）
                return result?.ToString() ?? string.Empty;
            }
            catch (Exception ex)
            {
                FileUtils.log($"msg = {ex.Message}", "getAb错误");
                throw ex;
            }
        }
        private void ExtractCookiesFromContainer(CookieContainer cookieContainer, Uri uri, Dictionary<string, string> cookies)
        {
            var containerCookies = cookieContainer.GetCookies(uri);
            foreach (Cookie cookie in containerCookies)
            {
                if (!cookies.ContainsKey(cookie.Name))
                {
                    cookies[cookie.Name] = cookie.Value;
                }
            }
        }

        private void ExtractCookiesFromHeaders(HttpResponseHeaders headers, CookieContainer cookieContainer, Dictionary<string, string> cookies)
        {
            if (headers.TryGetValues("Set-Cookie", out var cookieValues))
            {
                foreach (var cookie in cookieValues)
                {
                    var parts = cookie.Split(';');
                    var cookiePart = parts[0].Trim();
                    var equalPos = cookiePart.IndexOf('=');
                    if (equalPos > 0)
                    {
                        var name = cookiePart.Substring(0, equalPos);
                        var value = cookiePart.Substring(equalPos + 1);
                        if (!cookies.ContainsKey(name))
                        {
                            cookies[name] = value;
                        }
                    }
                }
            }
        }

        private string GetAcSignature(string ua, string acNonce)
        {
            string Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-./";
            uint Encrypt(uint hashVal, string str)
            {
                foreach (var ch in str)
                {
                    hashVal = ((hashVal ^ ch) * 65599) & 0xFFFFFFFF;
                }
                return hashVal;
            }

            uint Encrypt2(uint val, string str)
            {
                foreach (var ch in str)
                {
                    val = (val * 65599 + ch) & 0xFFFFFFFF;
                }
                return val;
            }

            string Encrypt3(ulong num)
            {
                var res = new StringBuilder();
                for (var i = 4; i >= 0; i--)
                {
                    res.Append(Chars[(int)((num >> (i * 6)) & 63)]);
                }
                return res.ToString();
            }

            var timestampInt = (int)(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() / 1000);
            var timestampStr = timestampInt.ToString();
            var timestampNumber = Encrypt(0, timestampStr);
            var urlNumber = Encrypt(timestampNumber, "www.douyin.com");
            var a1 = (timestampInt ^ ((urlNumber % 65521) * 65521)) & 0xFFFFFFFF;
            var bA1 = Convert.ToString(a1, 2);
            var bA2 = "10000000110000" + new string('0', 32 - bA1.Length) + bA1;
            var a2 = Convert.ToUInt64(bA2, 2);
            var a2Str = a2.ToString();
            var a2Number = Encrypt(0, a2Str);
            var str1 = Encrypt3(a2 >> 2);
            var r1 = (a2 / 4294967296) & 0xFFFFFFFF;
            var r2 = ((a2 << 28) | (r1 >> 4)) & 0xFFFFFFFF;
            var str2 = Encrypt3(r2);
            var r3 = (2010578131 ^ a2) & 0xFFFFFFFF;
            var r4 = ((r1 << 26) | (r3 >> 6)) & 0xFFFFFFFF;
            var str3 = Encrypt3(r4);
            var str3Last = Chars[(int)(r3 & 63)];
            var uaNumber = Encrypt((uint)a2Number, ua);
            var acNonceNumber = Encrypt((uint)a2Number, acNonce);
            var r5 = ((uaNumber % 65521) << 16) & 0xFFFFFFFF;
            var r6 = (r5 | (acNonceNumber % 65521)) & 0xFFFFFFFF;
            var str4 = Encrypt3(r6 >> 2);
            var r7 = ((r6 << 28) | ((524576 ^ a2) >> 4)) & 0xFFFFFFFF;
            var str5 = Encrypt3(r7);
            var str6 = Encrypt3(urlNumber % 65521);
            var str7 = "_02B4Z6wo00f01" + str1 + str2 + str3 + str3Last + str4 + str5 + str6;
            var str8 = Encrypt2(0, str7).ToString("x8").Substring(6);
            return str7 + str8;
        }

        private static string GenerateRandomHexString(int length)
        {
            using (var rng = RandomNumberGenerator.Create())
            {
                byte[] bytes = new byte[length];
                rng.GetBytes(bytes);
                StringBuilder sb = new StringBuilder();
                foreach (byte b in bytes)
                {
                    sb.Append(b.ToString("x2"));
                }
                return sb.ToString();
            }
        }

        /// <summary>
        /// 获取分辨率
        /// </summary>
        /// <returns></returns>
        private List<int> getResolution()
        {
            List<List<int>> resolutions = new List<List<int>>()
            {
                new List<int> { 750, 1334 },
                new List<int> { 800, 600 },
                new List<int> { 1024, 600 },
                new List<int> { 1024, 640 },
                new List<int> { 1024, 768 },
                new List<int> { 1152, 864 },
                new List<int> { 1280, 720 },
                new List<int> { 1280, 800 },
                new List<int> { 1280, 960 },
                new List<int> { 1280, 1024 },
                new List<int> { 1360, 768 },
                new List<int> { 808, 1792 },
                new List<int> { 828, 1792 },
                new List<int> { 1080, 2340 },
                new List<int> { 1125, 2436 },
                new List<int> { 1242, 2208 },
                new List<int> { 1170, 2532 },
                new List<int> { 1284, 2778 },
                new List<int> { 1366, 768 },
                new List<int> { 1400, 1050 },
                new List<int> { 1440, 900 },
                new List<int> { 1536, 864 },
                new List<int> { 1600, 900 },
                new List<int> { 1680, 1300 },
                new List<int> { 1920, 1080 },
                new List<int> { 1920, 1200 },
                new List<int> { 2048, 1152 },
                new List<int> { 2304, 1440 },
                new List<int> { 2560, 1440 },
                new List<int> { 2560, 1600 },
                new List<int> { 2880, 1800 },
                new List<int> { 4096, 2304 },
                new List<int> { 5120, 2880 }

            };
            // 创建随机数实例（推荐将 Random 定义为类成员，避免短时间多次调用导致重复）
            Random random = new Random();

            // 生成随机索引（范围：0 到 列表长度-1）
            int randomIndex = random.Next(resolutions.Count);

            // 返回随机选中的分辨率
            return resolutions[randomIndex];
        }

        private string getBrowserVersion(string ua)
        {
            string browserVersion = null;
            var match = Regex.Match(ua, @"Chrome/(.*?)\s+Safari");
            if (match.Success)
            {
                browserVersion = match.Groups[1].Value;
            }
            return browserVersion;
        }

        /// <summary>
        /// 判断是否为true
        /// </summary>
        /// <param name="value"></param>
        /// <returns></returns>
        private bool IsTrueOrOnePerf(dynamic value)
        {
            if (value == null)
            {
                return false;
            }
            switch (value)
            {
                case bool b: return b;
                case int i when i == 1: return true;
                case string s when s == "1" || s.Equals("true", StringComparison.OrdinalIgnoreCase): return true;
                default: return false;
            }
        }
    }
}
