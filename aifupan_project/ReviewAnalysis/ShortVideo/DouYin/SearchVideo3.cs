//using System;
//using System.Collections.Generic;
//using System.Linq;
//using System.Net;
//using System.Net.Http;
//using System.Net.Http.Headers;
//using System.Security.Cryptography;
//using System.Text;
//using System.Text.RegularExpressions;
//using System.Threading;
//using System.Web;
//using douyin.Utils;
//using Jint;
//using Newtonsoft.Json;
//using Newtonsoft.Json.Linq;
//using ReviewAnalysis.api;
//using ReviewAnalysis.ShortVideo.DouYin.dto;
//using ReviewAnalysis.Utils;
//using ReviewAnalysis.vo.proxyIP;

//namespace ReviewAnalysis.ShortVideo.DouYin
//{
//    public class SearchVideo3
//    {
//        private static int chromeVersion = 130;

//        // 代理相关属性
//        /// <summary>
//        /// 当前代理IP信息
//        /// </summary>
//        private static volatile ProxyIpVo _currentProxyIp;
//        /// <summary>
//        /// 是否正在使用代理
//        /// </summary>
//        private static volatile bool _isUseProxy;
//        /// <summary>
//        /// 使用代理的过期时间，超过这个时间则切回正常IP
//        /// </summary>
//        private static long _proxyExpireTime = 0;
//        private static readonly object _proxyIpLock = new object();

//        /// <summary>
//        /// PC端浏览器User-Agent列表（覆盖Chrome/Edge/Firefox/Safari/360/QQ浏览器，Windows/macOS）
//        /// </summary>
//        private static readonly List<string> _pcUaList = new List<string>
//{
//    #region Chrome（Windows 10/11，多版本）
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
//    "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0", // Chrome内核混编
//    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
//    #endregion

//    #region Chrome（macOS，多版本）
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
//    "Mozilla/5.0 (Macintosh; Apple Silicon) Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36", // M芯片Mac
//    #endregion

//    #region Microsoft Edge（Chromium内核，Windows/macOS）
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0",
//    "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0",
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/129.0.0.0 Safari/537.36",
//    #endregion

//    #region Firefox（火狐，Windows/macOS）
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0",
//    "Mozilla/5.0 (Windows NT 11.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5; rv:136.0) Gecko/20100101 Firefox/136.0",
//    "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:134.0) Gecko/20100101 Firefox/134.0",
//    #endregion

//    #region Safari（macOS/iPadOS）
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
//    "Mozilla/5.0 (Macintosh; Apple Silicon) Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
//    "Mozilla/5.0 (iPad; CPU OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Safari/605.1.15", // iPad端PC模式
//    #endregion

//    #region 国内浏览器（360/QQ/搜狗）
//    // 360安全浏览器（Chrome内核）
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 360SE/15.0.0.0",
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 360EE/13.0.0.0", // 360极速浏览器
//    // QQ浏览器
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 QQBrowser/11.1.5000.0",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 QQBrowser/11.1.5000.0",
//    // 搜狗浏览器
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 SogouExplorer/12.0.0.0",
//    #endregion

//    #region 小众浏览器（Opera/Vivaldi）
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 OPR/121.0.0.0", // Opera
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Vivaldi/6.8.0.0", // Vivaldi
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 OPR/121.0.0.0",
//    #endregion

//    #region 低版本兼容（适配老旧网站）
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36",
//    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/102.0",
//    "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Safari/605.1.15",
//    #endregion
//};
//        private static readonly Random _random = new Random();
//        /// <summary>
//        /// 随机获取一个PC端UA（线程安全）
//        /// </summary>
//        /// <returns>随机UA字符串</returns>
//        public static string GetRandomPcUa()
//        {
//            lock (_random) // 保证多线程下Random的安全性
//            {
//                int index = _random.Next(0, _pcUaList.Count);
//                return _pcUaList[index];
//            }
//        }

//        /// <summary>
//        /// 调用抖音搜索视频接口，手动释放资源
//        /// </summary>
//        /// <param name="keyword"></param>
//        /// <param name="videoNum"></param>
//        /// <returns></returns>
//        public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] next = null)
//        {
//            SearchVideo3 searchVideo = null;
//            try
//            {
//                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
//                next = next ?? new int[] { 50, 100 };
//                searchVideo = new SearchVideo3();
//                List<DouYinVideoInfo> list = null;
//                for (int i = 0; i < 3; i++)
//                {
//                    list = searchVideo.startSearchVideo(keyword, videoNum, videoNum * 2, next, true);
//                    if (list != null && list.Count > 0)
//                    {
//                        break;
//                    }
//                    FileUtils.log("----------没有查询到数据---------");
//                }
//                if (list == null || list.Count == 0)
//                {
//                    FileUtils.log("爆款请求三次还是没有数据");
//                }
//                return list;
//            }
//            catch (Exception ex)
//            {
//                FileUtils.LogError($"Message = {ex.Message}, StackTrace = {ex.StackTrace}", "执行抖音搜索视频报错");
//            }
//            return new List<DouYinVideoInfo>();
//        }


//        /// <summary>
//        /// 开始搜索视频
//        /// </summary>
//        /// <param name="_keyword"></param>
//        /// <param name="videoCount"></param>
//        /// <param name="maxCount"></param>
//        /// <param name="nextTime"></param>
//        /// <param name="versionAdd"></param>
//        /// <param name="allowRetry">是否允许重试</param>
//        /// <returns></returns>
//        public List<DouYinVideoInfo> startSearchVideo(string _keyword, int videoCount, int maxCount = 300, int[] nextTime = null, bool versionAdd = false)
//        {
//            List<DouYinVideoInfo> items = new List<DouYinVideoInfo>();
//            var currentNow = DateTime.Now;
//            var random = new Random();
//            string RandChoice(params string[] options) => options[random.Next(options.Length)];
//            int RandInt(int min, int max) => random.Next(min, max + 1);
//            List<int> resolution = getResolution();
//            int currentPage = 1;
//            bool shouldExit = false;
//            var apiUrl = "https://www.douyin.com/aweme/v1/web/search/item/";
//            int countProxy = 0;
//            int maxProxy = 10;
//            int count = 0;
//            bool allowRetry = true;
//            int loginRetryCount = 0;

//            string searchId = null;
//            var sortType = "0"; // 排序方式 0:综合排序, 1:最多点赞, 2:最新发布
//            var publishTime = "180"; // 发布时间 0:不限, 1:一天内, 7:一周内, 180:半年内
//            var filterDuration = ""; // 视频时长

//            (Dictionary<string, string> cookiesMap, CookieContainer cookieContainer)? cookiesRes = null;
//            string ua = "";

//            while (!shouldExit)
//            {
//                if (cookiesRes == null || !cookiesRes.HasValue)
//                {
//                    string version = getChromeVersion(versionAdd);
//                    ua = GetRandomPcUa();// $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version}.0.0.0 Safari/537.36";
//                    cookiesRes = getCookieContainer(_keyword, ua);
//                }

//                Dictionary<string, string> headers = new Dictionary<string, string>()
//                {
//                    { "User-Agent", ua},
//                    { "Accept", "application/json, text/plain, */*"},
//                    { "referer", $"https://www.douyin.com/jingxuan/search/{HttpUtility.UrlEncode(_keyword)}?aid={Guid.NewGuid().ToString()}&type=general"}
//                };

//                Dictionary<string, string> parameters = new Dictionary<string, string>()
//                {
//                    { "device_platform", "webapp" },
//                    { "aid", "6383" },
//                    { "channel", "channel_pc_web" },
//                    { "search_channel", "aweme_video_web" },
//                    { "enable_history", "1" },
//                    { "keyword", _keyword },
//                    { "search_source", "switch_tab" },
//                    { "query_correct_type", "1" },
//                    { "is_filter_search", "0" },
//                    { "from_group_id", "" },
//                    { "disable_rs", "1" },
//                    { "offset", ((currentPage - 1) * 25).ToString() },
//                    { "count", "25" },
//                    { "need_filter_settings", currentPage == 1 ? "1" : "0" },
//                    { "list_type", "" },
//                    { "update_version_code", "170400" },
//                    { "pc_client_type", "1" },
//                    { "pc_libra_divert", "Windows" },
//                    { "support_h265", "1" },
//                    { "support_dash", "1" },
//                    { "cpu_core_num", RandChoice("4", "8", "16") },
//                    { "version_code", "170400" },
//                    { "version_name", "17.4.0" },
//                    { "cookie_enabled", "true" },
//                    { "screen_width", resolution[0].ToString() },
//                    { "screen_height", resolution[1].ToString() },
//                    { "browser_language", RandChoice("zh-CN", "en") },
//                    { "browser_platform", "Win32" },
//                    { "browser_name", "Chrome" },
//                    //{ "browser_version", getBrowserVersion(ua) },
//                    { "browser_online", "true" },
//                    { "engine_name", "Blink" },
//                    //{ "engine_version", getBrowserVersion(ua) },
//                    { "os_name", "Windows" },
//                    { "os_version", "10" },
//                    { "device_memory", RandChoice("4", "8", "16") },
//                    { "platform", "PC" },
//                    { "downlink", RandInt(5, 50).ToString() },
//                    { "effective_type", RandChoice("4g", "wifi") },
//                    { "round_trip_time", RandInt(50, 200).ToString() }
//                };

//                // search_id
//                if (currentPage > 1 && !string.IsNullOrEmpty(searchId))
//                {
//                    parameters.Add("search_id", searchId);
//                }
//                // 设置筛选条件
//                parameters["publish_time"] = publishTime;
//                parameters["is_filter_search"] = sortType;

//                // 设置
//                string a_bogus = getAb(ua, parameters);
//                if (!string.IsNullOrEmpty(a_bogus))
//                {
//                    parameters["a_bogus"] = a_bogus;
//                }

//                var queryString = string.Join("&", parameters.Select(kv => $"{kv.Key}={HttpUtility.UrlEncode(kv.Value)}"));
//                var requestUrl = $"{apiUrl}?{queryString}";

//                // 开始就使用代理
//                UseProxyIP();

//                using (HttpClientHandler handler = new HttpClientHandler())
//                {
//                    // 设置代理
//                    CheckProxyExpire();
//                    if (_currentProxyIp != null && !string.IsNullOrEmpty(_currentProxyIp.ip) && !string.IsNullOrEmpty(_currentProxyIp.port))
//                    {
//                        // 设置代理
//                        WebProxy webProxy = new WebProxy($"{_currentProxyIp.ip}:{_currentProxyIp.port}");
//                        if (!string.IsNullOrEmpty(_currentProxyIp.proxyUsername) && !string.IsNullOrEmpty(_currentProxyIp.proxyPassword))
//                        {
//                            webProxy.Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword);
//                        }

//                        handler.Proxy = webProxy;
//                        handler.UseProxy = true;
//                    }

//                    using (HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Get, requestUrl))
//                    using (HttpClient httpClient = new HttpClient(handler))
//                    {
//                        if (cookiesRes.HasValue && cookiesRes.Value.cookieContainer != null)
//                        {
//                            handler.CookieContainer = cookiesRes.Value.cookieContainer;
//                        }

//                        foreach (var header in headers)
//                        {
//                            request.Headers.TryAddWithoutValidation(header.Key, header.Value);
//                        }

//                        try
//                        {
//                            // 发送请求并确保成功
//                            using (HttpResponseMessage response = httpClient.SendAsync(request).Result)
//                            {
//                                response.EnsureSuccessStatusCode();

//                                string responseContent = response.Content.ReadAsStringAsync().Result;
//                                if (responseContent.Length < 10)
//                                {
//                                    FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, responseContent.Length < 10， responseContent = {responseContent}", "搜爆款出现风控，将使用代理重试");

//                                    if (allowRetry)
//                                    {
//                                        UseProxyIP();
//                                        allowRetry = false;
//                                        continue;
//                                    }
//                                    else
//                                    {
//                                        shouldExit = true;
//                                        continue;
//                                    }

//                                }

//                                //FileUtils.log(responseContent);
//                                // 读取JSON内容并解析
//                                //using var jsonDoc = JsonDocument.Parse(responseContent);
//                                //var root = jsonDoc.RootElement;
//                                dynamic root = JsonConvert.DeserializeObject(responseContent); // 替换 JsonDocument.Parse 

//                                if (root.search_nil_info != null)
//                                {
//                                    string searchNilType = root.search_nil_info.search_nil_type?.ToString();
//                                    if (!string.IsNullOrEmpty(searchNilType))
//                                    {
//                                        cookiesRes = null;
//                                        if (searchNilType.Contains("web_need_login"))
//                                        {
//                                            loginRetryCount++;
//                                            if (loginRetryCount > 10)
//                                            {
//                                                FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款web_need_login，当前是第{loginRetryCount}次，已超出重试次数");
//                                                shouldExit = true;
//                                            }
//                                            else
//                                            {
//                                                FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款web_need_login，将重新请求，当前是第{loginRetryCount}次");
//                                                Thread.Sleep(5000);
//                                            }

//                                            continue;
//                                        }
//                                        else if (allowRetry)
//                                        {
//                                            // 使用代理重试
//                                            FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款出现风控，将使用代理重试");
//                                            UseProxyIP();
//                                            allowRetry = false;
//                                            Thread.Sleep(5000);
//                                            continue;
//                                        }
//                                        else
//                                        {
//                                            shouldExit = true;
//                                            continue;
//                                        }
//                                    }
//                                    else
//                                    {
//                                        shouldExit = true;
//                                        continue;
//                                    }
//                                }

//                                var data = root.data;
//                                if ((root?.status_code ?? -1) == 0 && data.Type == JTokenType.Array)
//                                {
//                                    // 处理 search_id（动态属性访问）
//                                    if (currentPage == 1 && root.extra?.logid != null)
//                                    {
//                                        searchId = root.extra.logid.ToString();  // 自动处理类型转换 
//                                        FileUtils.log($"search_id: {searchId}");
//                                    }

//                                    // 处理 has_more
//                                    bool hasMore = IsTrueOrOnePerf(root?.has_more?.ToString() ?? null);
//                                    FileUtils.log(hasMore ? "还有下一页" : "没有下一页");

//                                    // 处理数据
//                                    foreach (dynamic item in root.data)
//                                    {
//                                        count++;
//                                        DouYinVideoInfo temp = item?.aweme_info?.ToObject<DouYinVideoInfo>() ?? null;
//                                        if (!string.IsNullOrEmpty(temp?.video?.play_addr?.file_hash ?? ""))
//                                        {
//                                            temp.desc = ShortVideoUtils.formatDesc(temp.desc);
//                                            if ((temp?.video?.duration ?? 0) > 0)
//                                            {
//                                                temp.video.duration = ShortVideoUtils.formatDuration(temp?.video?.duration ?? 0);
//                                            }

//                                            items.Add(temp);
//                                        }

//                                        if (items.Count >= videoCount || count >= maxCount)
//                                        {
//                                            shouldExit = true;
//                                            break;
//                                        }
//                                    }

//                                    if (!hasMore)
//                                    {
//                                        shouldExit = true;
//                                    }
//                                }
//                                else
//                                {
//                                    FileUtils.log($"API返回状态码异常或数据无效 videoCount = {videoCount}， responseContent = {responseContent}", "搜爆款-code或者data数据问题");
//                                    shouldExit = true;
//                                }
//                            }

//                        }
//                        catch (Exception ex)
//                        {
//                            FileUtils.log($"请求错误： videoCount = {videoCount}， Message = {ex.Message}", "搜爆款-请求错误");

//                            shouldExit = true;
//                        }
//                    }
//                }



//                int next = random.Next(nextTime[0], nextTime[1]);
//                Thread.Sleep(next);
//                currentPage++;
//            }
//            // 去重
//            List<DouYinVideoInfo> result = items.GroupBy(item => item.aweme_id).Select(group => group.First()).ToList();

//            FileUtils.log($"搜爆款共爬取{currentPage - 1}页，总计{count}个作品, 总视频{items.Count}个，总图文{count - items.Count}个，去重后视频{result?.Count ?? 0}个, 一共用时{(DateTime.Now - currentNow).TotalMilliseconds}ms", "搜爆款-统计");
//            return result;

//        }


//        /// <summary>
//        /// 获取cookies
//        /// </summary>
//        /// <param name="_keyword"></param>
//        /// <param name="ua"></param>
//        /// <param name="proxyIpVo"></param>
//        /// <returns></returns>
//        public (Dictionary<string, string> cookiesMap, CookieContainer cookieContainer) getCookieContainer(string _keyword, string ua)
//        {
//            CookieContainer cookieContainer = new CookieContainer();
//            var cookies = new Dictionary<string, string>();
//            var encodedKeyword = HttpUtility.UrlEncode(_keyword);
//            var url = $"https://www.douyin.com/search/{encodedKeyword}?type=video";

//            HttpClientHandler handler = null;
//            HttpClient httpClient = null;
//            try
//            {
//                CheckProxyExpire();
//                if (_currentProxyIp != null && !string.IsNullOrEmpty(_currentProxyIp.ip) && !string.IsNullOrEmpty(_currentProxyIp.port))
//                {
//                    // 设置代理
//                    WebProxy webProxy = new WebProxy($"{_currentProxyIp.ip}:{_currentProxyIp.port}");
//                    if (!string.IsNullOrEmpty(_currentProxyIp.proxyUsername) && !string.IsNullOrEmpty(_currentProxyIp.proxyPassword))
//                    {
//                        webProxy.Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword);
//                    }

//                    handler = new HttpClientHandler
//                    {
//                        AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
//                        UseCookies = true,
//                        CookieContainer = cookieContainer,
//                        AllowAutoRedirect = true,
//                        UseDefaultCredentials = false,
//                        Proxy = webProxy,
//                        UseProxy = true
//                    };
//                }else
//                {
//                    handler = new HttpClientHandler
//                    {
//                        AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
//                        UseCookies = true,
//                        CookieContainer = cookieContainer,
//                        AllowAutoRedirect = true,
//                        UseDefaultCredentials = false,
//                        UseProxy = false
//                    };
//                }

//                httpClient = new HttpClient(handler);
//                // 第一次请求 - 获取初始Cookie，包括__ac_nonce
//                FileUtils.log("第一次请求获取初始Cookie...");
//                httpClient.DefaultRequestHeaders.Add("User-Agent", ua);
//                httpClient.DefaultRequestHeaders.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
//                httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
//                httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
//                httpClient.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
//                httpClient.DefaultRequestHeaders.Add("sec-fetch-site", "none");
//                httpClient.DefaultRequestHeaders.Add("sec-fetch-mode", "navigate");
//                httpClient.DefaultRequestHeaders.Add("sec-fetch-user", "?1");
//                httpClient.DefaultRequestHeaders.Add("sec-fetch-dest", "document");
//                httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
//                // 对可能包含非法字符或非标准头，使用 TryAddWithoutValidation
//                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
//                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("priority", "u=0, i");

//                using(var firstResponse = httpClient.GetAsync(url).Result)
//                {
//                    firstResponse.EnsureSuccessStatusCode();

//                    // 从CookieContainer中提取所有Cookie
//                    ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);

//                    if (!cookies.ContainsKey("__ac_nonce"))
//                    {
//                        FileUtils.log("警告: 未获取到__ac_nonce Cookie，尝试从响应头提取");
//                        ExtractCookiesFromHeaders(firstResponse.Headers, cookieContainer, cookies);
//                    }

//                    // 如果仍然没有__ac_nonce，重试一次
//                    if (!cookies.ContainsKey("__ac_nonce"))
//                    {
//                        FileUtils.log("__ac_nonce获取失败，重试一次...");
//                        Thread.Sleep(1000);
//                        var retryResponse = httpClient.GetAsync(url).Result;
//                        ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
//                        ExtractCookiesFromHeaders(retryResponse.Headers, cookieContainer, cookies);
//                    }

//                    if (!cookies.TryGetValue("__ac_nonce", out var acNonce))
//                    {
//                        FileUtils.log("错误: 无法获取__ac_nonce Cookie");
//                        return (cookies, cookieContainer);
//                    }

//                    // 生成__ac_signature
//                    var acSignature = GetAcSignature(ua, acNonce);
//                    cookies["__ac_signature"] = acSignature;
//                    cookies["__ac_referer"] = "__ac_blank";

//                    // 添加生成的签名到CookieContainer
//                    cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_signature", acSignature));
//                    cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_referer", "__ac_blank"));

//                    // 第二次请求 - 尝试获取ttwid
//                    FileUtils.log("第二次请求尝试获取ttwid...");
//                    httpClient.DefaultRequestHeaders.Referrer = new Uri(url);

//                    var secondResponse = httpClient.GetAsync(url).Result;
//                    secondResponse.EnsureSuccessStatusCode();

//                    // 再次从容器中提取所有Cookie
//                    ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);

//                    // 检查是否获取到ttwid，未获取到则尝试第三次请求
//                    if (!cookies.ContainsKey("ttwid"))
//                    {
//                        FileUtils.log("警告: 未获取到ttwid Cookie，尝试第三次请求...");
//                        Thread.Sleep(1500);
//                        var thirdResponse = httpClient.GetAsync(url).Result;
//                        ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
//                    }

//                    // 添加额外需要的Cookie
//                    cookies["x-web-secsdk-uid"] = Guid.NewGuid().ToString();
//                    cookies["csrf_session_id"] = GenerateRandomHexString(16);
//                    cookies["SEARCH_RESULT_LIST_TYPE"] = "%22single%22";
//                    cookies["home_can_add_dy_2_desktop"] = "%220%22";
//                    cookies["hevc_supported"] = "true";

//                    // 输出最终获取的Cookie信息
//                    FileUtils.log("\n初始化cookies信息:");
//                    //foreach (var cookie in cookies)
//                    //{
//                    //    FileUtils.log($"=================={cookie.Key}={cookie.Value}==================");
//                    //}
//                }

//            }
//            catch (Exception ex)
//            {
//                FileUtils.log($"获取Cookie时出错: {ex.Message}");
//            }
//            finally
//            {
//                httpClient?.Dispose();
//            }

//            return (cookies, cookieContainer);
//        }

//        private string getChromeVersion(bool add = false)
//        {
//            if (add)
//            {
//                chromeVersion++;
//            }
//            if (chromeVersion >= 138)
//            {
//                chromeVersion = 131;
//            }
//            return $"{chromeVersion}";
//        }

//        private string getAb(string ua, Dictionary<string, string> parameters)
//        {
//            try
//            {
//                var engine = new Engine();
//                engine.Execute(DouYinUtils.getAbJs()); // 编译 JS 代码

//                if (parameters == null || parameters.Count == 0)
//                    return "";

//                // 对每个键值对进行 URL 编码后拼接
//                var encodedParts = parameters.Select(pair =>
//                    $"{HttpUtility.UrlEncode(pair.Key)}={HttpUtility.UrlEncode(pair.Value)}");

//                string paramsStr = string.Join("&", encodedParts);

//                // 调用函数并传递参数（ua, paramsStr, dataStr）
//                var result = engine.Invoke("get_a_bogus", ua, paramsStr, "");

//                // 4. 返回结果（转换为字符串）
//                return result?.ToString() ?? string.Empty;
//            }
//            catch (Exception ex)
//            {
//                FileUtils.log($"msg = {ex.Message}", "getAb错误");
//                throw ex;
//            }
//        }
//        private void ExtractCookiesFromContainer(CookieContainer cookieContainer, Uri uri, Dictionary<string, string> cookies)
//        {
//            var containerCookies = cookieContainer.GetCookies(uri);
//            foreach (Cookie cookie in containerCookies)
//            {
//                if (!cookies.ContainsKey(cookie.Name))
//                {
//                    cookies[cookie.Name] = cookie.Value;
//                }
//            }
//        }

//        private void ExtractCookiesFromHeaders(HttpResponseHeaders headers, CookieContainer cookieContainer, Dictionary<string, string> cookies)
//        {
//            if (headers.TryGetValues("Set-Cookie", out var cookieValues))
//            {
//                foreach (var cookie in cookieValues)
//                {
//                    var parts = cookie.Split(';');
//                    var cookiePart = parts[0].Trim();
//                    var equalPos = cookiePart.IndexOf('=');
//                    if (equalPos > 0)
//                    {
//                        var name = cookiePart.Substring(0, equalPos);
//                        var value = cookiePart.Substring(equalPos + 1);
//                        if (!cookies.ContainsKey(name))
//                        {
//                            cookies[name] = value;
//                        }
//                    }
//                }
//            }
//        }

//        private string GetAcSignature(string ua, string acNonce)
//        {
//            string Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-./";
//            uint Encrypt(uint hashVal, string str)
//            {
//                foreach (var ch in str)
//                {
//                    hashVal = ((hashVal ^ ch) * 65599) & 0xFFFFFFFF;
//                }
//                return hashVal;
//            }

//            uint Encrypt2(uint val, string str)
//            {
//                foreach (var ch in str)
//                {
//                    val = (val * 65599 + ch) & 0xFFFFFFFF;
//                }
//                return val;
//            }

//            string Encrypt3(ulong num)
//            {
//                var res = new StringBuilder();
//                for (var i = 4; i >= 0; i--)
//                {
//                    res.Append(Chars[(int)((num >> (i * 6)) & 63)]);
//                }
//                return res.ToString();
//            }

//            var timestampInt = (int)(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() / 1000);
//            var timestampStr = timestampInt.ToString();
//            var timestampNumber = Encrypt(0, timestampStr);
//            var urlNumber = Encrypt(timestampNumber, "www.douyin.com");
//            var a1 = (timestampInt ^ ((urlNumber % 65521) * 65521)) & 0xFFFFFFFF;
//            var bA1 = Convert.ToString(a1, 2);
//            var bA2 = "10000000110000" + new string('0', 32 - bA1.Length) + bA1;
//            var a2 = Convert.ToUInt64(bA2, 2);
//            var a2Str = a2.ToString();
//            var a2Number = Encrypt(0, a2Str);
//            var str1 = Encrypt3(a2 >> 2);
//            var r1 = (a2 / 4294967296) & 0xFFFFFFFF;
//            var r2 = ((a2 << 28) | (r1 >> 4)) & 0xFFFFFFFF;
//            var str2 = Encrypt3(r2);
//            var r3 = (2010578131 ^ a2) & 0xFFFFFFFF;
//            var r4 = ((r1 << 26) | (r3 >> 6)) & 0xFFFFFFFF;
//            var str3 = Encrypt3(r4);
//            var str3Last = Chars[(int)(r3 & 63)];
//            var uaNumber = Encrypt((uint)a2Number, ua);
//            var acNonceNumber = Encrypt((uint)a2Number, acNonce);
//            var r5 = ((uaNumber % 65521) << 16) & 0xFFFFFFFF;
//            var r6 = (r5 | (acNonceNumber % 65521)) & 0xFFFFFFFF;
//            var str4 = Encrypt3(r6 >> 2);
//            var r7 = ((r6 << 28) | ((524576 ^ a2) >> 4)) & 0xFFFFFFFF;
//            var str5 = Encrypt3(r7);
//            var str6 = Encrypt3(urlNumber % 65521);
//            var str7 = "_02B4Z6wo00f01" + str1 + str2 + str3 + str3Last + str4 + str5 + str6;
//            var str8 = Encrypt2(0, str7).ToString("x8").Substring(6);
//            return str7 + str8;
//        }

//        private static string GenerateRandomHexString(int length)
//        {
//            using (var rng = RandomNumberGenerator.Create())
//            {
//                byte[] bytes = new byte[length];
//                rng.GetBytes(bytes);
//                StringBuilder sb = new StringBuilder();
//                foreach (byte b in bytes)
//                {
//                    sb.Append(b.ToString("x2"));
//                }
//                return sb.ToString();
//            }
//        }

//        /// <summary>
//        /// 获取分辨率
//        /// </summary>
//        /// <returns></returns>
//        private List<int> getResolution()
//        {
//            List<List<int>> resolutions = new List<List<int>>()
//            {
//                new List<int> { 750, 1334 },
//                new List<int> { 800, 600 },
//                new List<int> { 1024, 600 },
//                new List<int> { 1024, 640 },
//                new List<int> { 1024, 768 },
//                new List<int> { 1152, 864 },
//                new List<int> { 1280, 720 },
//                new List<int> { 1280, 800 },
//                new List<int> { 1280, 960 },
//                new List<int> { 1280, 1024 },
//                new List<int> { 1360, 768 },
//                new List<int> { 808, 1792 },
//                new List<int> { 828, 1792 },
//                new List<int> { 1080, 2340 },
//                new List<int> { 1125, 2436 },
//                new List<int> { 1242, 2208 },
//                new List<int> { 1170, 2532 },
//                new List<int> { 1284, 2778 },
//                new List<int> { 1366, 768 },
//                new List<int> { 1400, 1050 },
//                new List<int> { 1440, 900 },
//                new List<int> { 1536, 864 },
//                new List<int> { 1600, 900 },
//                new List<int> { 1680, 1300 },
//                new List<int> { 1920, 1080 },
//                new List<int> { 1920, 1200 },
//                new List<int> { 2048, 1152 },
//                new List<int> { 2304, 1440 },
//                new List<int> { 2560, 1440 },
//                new List<int> { 2560, 1600 },
//                new List<int> { 2880, 1800 },
//                new List<int> { 4096, 2304 },
//                new List<int> { 5120, 2880 }

//            };
//            // 创建随机数实例（推荐将 Random 定义为类成员，避免短时间多次调用导致重复）
//            Random random = new Random();

//            // 生成随机索引（范围：0 到 列表长度-1）
//            int randomIndex = random.Next(resolutions.Count);

//            // 返回随机选中的分辨率
//            return resolutions[randomIndex];
//        }

//        private string getBrowserVersion(string ua)
//        {
//            string browserVersion = null;
//            var match = Regex.Match(ua, @"Chrome/(.*?)\s+Safari");
//            if (match.Success)
//            {
//                browserVersion = match.Groups[1].Value;
//            }
//            return browserVersion;
//        }

//        /// <summary>
//        /// 判断是否为true
//        /// </summary>
//        /// <param name="value"></param>
//        /// <returns></returns>
//        private bool IsTrueOrOnePerf(dynamic value)
//        {
//            if (value == null)
//            {
//                return false;
//            }
//            switch (value)
//            {
//                case bool b: return b;
//                case int i when i == 1: return true;
//                case string s when s == "1" || s.Equals("true", StringComparison.OrdinalIgnoreCase): return true;
//                default: return false;
//            }
//        }












//        /// <summary>
//        /// 检查并代理是否已失效，失效了设置为直连
//        /// </summary>
//        private void CheckProxyExpire()
//        {
//            lock (_proxyIpLock)
//            {

//                if (!_isUseProxy)
//                {
//                    return;
//                }

//                long currentTime = ServerTimeUtils.getCurrentTime();

//                // 检查代理IP是否过期
//                if (currentTime >= _proxyExpireTime)
//                {
//                    FileUtils.log($"搜爆款代理IP已过期", $"当前时间：{currentTime}，过期时间：{_proxyExpireTime}");
//                    _isUseProxy = false;
//                    _currentProxyIp = null;
//                }
//            }
//        }
//        /// <summary>
//        /// 使用代理IP
//        /// </summary>
//        private void UseProxyIP()
//        {
//            lock (_proxyIpLock)
//            {
//                if (_isUseProxy)
//                {
//                    return;
//                }

//                ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(false, 0);
//                if (proxyIpVo != null)
//                {
//                    _isUseProxy = true;
//                    _currentProxyIp = proxyIpVo;
//                    _proxyExpireTime = proxyIpVo.expireTime;
//                    FileUtils.log($"搜爆款代理IP设置成功", $"IP: {proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}");
//                }
//                else
//                {
//                    FileUtils.log($"获取搜爆款代理IP失败", $"从服务器获取代理IP返回null");
//                }
//            }

//        }
//    }
//}



using douyin.Utils;
using Jint;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using Swan;
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
using System.Threading.Tasks;
using System.Web;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class SearchVideo3
    {
        #region 静态配置（适配抖音最新规范）
        // Chrome版本范围（适配抖音风控）
        private static readonly int[] _chromeVersions = { 132, 133, 134, 135, 136, 137 };
        private static readonly Random _globalRandom = new Random(Guid.NewGuid().GetHashCode());
        private static readonly CookieContainer _cookieContainer = new CookieContainer();
        private static readonly HttpClient _httpClient = new HttpClient(new HttpClientHandler
        {
            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
            AllowAutoRedirect = true,
            UseCookies = true,
            CookieContainer = _cookieContainer,
            UseDefaultCredentials = false
        }, true);

        // PC端专属分辨率（移除移动端分辨率）
        private static readonly List<List<int>> _pcResolutions = new List<List<int>>()
        {
            new List<int> { 1366, 768 },
            new List<int> { 1920, 1080 },
            new List<int> { 2560, 1440 },
            new List<int> { 1440, 900 },
            new List<int> { 1680, 1050 },
            new List<int> { 2048, 1152 },
            new List<int> { 2880, 1800 }
        };

        // PC端UA列表（已优化，增加抖音适配性）
        private static readonly List<string> _pcUaList = new List<string>
        {
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36",
            #region Chrome（Windows 10/11，多版本）
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0", // Chrome内核混编
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            #endregion

            #region Chrome（macOS，多版本）
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Apple Silicon) Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36", // M芯片Mac
            #endregion

            #region Microsoft Edge（Chromium内核，Windows/macOS）
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0",
            "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/129.0.0.0 Safari/537.36",
            #endregion

            #region Firefox（火狐，Windows/macOS）
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0",
            "Mozilla/5.0 (Windows NT 11.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5; rv:136.0) Gecko/20100101 Firefox/136.0",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:134.0) Gecko/20100101 Firefox/134.0",
            #endregion

            #region Safari（macOS/iPadOS）
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
            "Mozilla/5.0 (Macintosh; Apple Silicon) Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (iPad; CPU OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Safari/605.1.15", // iPad端PC模式
            #endregion

            #region 国内浏览器（360/QQ/搜狗）
            // 360安全浏览器（Chrome内核）
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 360SE/15.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 360EE/13.0.0.0", // 360极速浏览器
            // QQ浏览器
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 QQBrowser/11.1.5000.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 QQBrowser/11.1.5000.0",
            // 搜狗浏览器
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 SogouExplorer/12.0.0.0",
            #endregion

            #region 小众浏览器（Opera/Vivaldi）
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 OPR/121.0.0.0", // Opera
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Vivaldi/6.8.0.0", // Vivaldi
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 OPR/121.0.0.0",
            #endregion

            #region 低版本兼容（适配老旧网站）
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/102.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Safari/605.1.15",
            #endregion
        };
        #endregion

        #region 代理相关（优化逻辑）
        private static volatile ProxyIpVo _currentProxyIp;
        private static volatile bool _isUseProxy;
        private static long _proxyExpireTime;
        private static readonly object _proxyLock = new object();

        /// <summary>
        /// 检查代理是否过期，过期则切换直连
        /// </summary>
        private void CheckProxyExpire()
        {
            lock (_proxyLock)
            {
                if (!_isUseProxy || _currentProxyIp == null) return;

                long currentTime = ServerTimeUtils.getCurrentTime();
                if (currentTime >= _proxyExpireTime)
                {
                    FileUtils.log($"代理IP {_currentProxyIp.ip}:{_currentProxyIp.port} 已过期", "代理管理");
                    _isUseProxy = false;
                    _currentProxyIp = null;
                    // 重置HttpClient代理
                    var handler = new HttpClientHandler
                    {
                        AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                        UseCookies = true,
                        CookieContainer = _cookieContainer,
                        AllowAutoRedirect = true,
                        UseDefaultCredentials = false,
                        //Proxy = webProxy,
                        //UseProxy = true
                    };
                    handler.Proxy = null;
                    handler.UseProxy = false;
                }
            }
        }

        /// <summary>
        /// 获取有效代理（带重试）
        /// </summary>
        /// <returns>是否获取成功</returns>
        private bool GetValidProxy()
        {
            lock (_proxyLock)
            {
                if (_isUseProxy && _currentProxyIp != null) return true;

                // 最多重试3次获取代理
                for (int i = 0; i < 3; i++)
                {
                    ProxyIpVo proxy = ProxyApi.GetProxyIpSync(false, 0);
                    if (proxy != null && !string.IsNullOrEmpty(proxy.ip) && !string.IsNullOrEmpty(proxy.port))
                    {
                        _currentProxyIp = proxy;
                        _isUseProxy = true;
                        _proxyExpireTime = proxy.expireTime;

                        // 设置HttpClient代理
                        var handler = new HttpClientHandler
                        {
                            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                            UseCookies = true,
                            CookieContainer = _cookieContainer,
                            AllowAutoRedirect = true,
                            UseDefaultCredentials = false,
                            //Proxy = webProxy,
                            UseProxy = true
                        };
                        var webProxy = new WebProxy($"{proxy.ip}:{proxy.port}");
                        if (!string.IsNullOrEmpty(proxy.proxyUsername) && !string.IsNullOrEmpty(proxy.proxyPassword))
                        {
                            webProxy.Credentials = new NetworkCredential(proxy.proxyUsername, proxy.proxyPassword);
                        }
                        handler.Proxy = webProxy;
                        handler.UseProxy = true;

                        FileUtils.log($"成功获取代理IP：{proxy.ip}:{proxy.port}，过期时间：{proxy.expireTime}", "代理管理");
                        return true;
                    }
                    Thread.Sleep(1000);
                }

                FileUtils.log("获取代理IP失败，将使用直连", "代理管理");
                _isUseProxy = false;
                _currentProxyIp = null;
                var defaultHandler = new HttpClientHandler
                {
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                    UseCookies = true,
                    CookieContainer = _cookieContainer,
                    AllowAutoRedirect = true,
                    UseDefaultCredentials = false,
                    //Proxy = webProxy,
                    UseProxy = true
                };
                defaultHandler.Proxy = null;
                defaultHandler.UseProxy = false;
                return false;
            }
        }
        #endregion

        #region 工具方法（适配抖音最新规则）
        /// <summary>
        /// 随机获取PC UA（适配抖音）
        /// </summary>
        private string GetRandomPcUa()
        {
            lock (_globalRandom)
            {
                return _pcUaList[_globalRandom.Next(_pcUaList.Count)];
            }
        }

        /// <summary>
        /// 获取随机Chrome版本
        /// </summary>
        private string GetRandomChromeVersion()
        {
            lock (_globalRandom)
            {
                return _chromeVersions[_globalRandom.Next(_chromeVersions.Length)].ToString();
            }
        }

        /// <summary>
        /// 获取PC端随机分辨率
        /// </summary>
        private List<int> GetRandomPcResolution()
        {
            lock (_globalRandom)
            {
                return _pcResolutions[_globalRandom.Next(_pcResolutions.Count)];
            }
        }

        /// <summary>
        /// 解析UA中的浏览器版本
        /// </summary>
        private string GetBrowserVersionFromUa(string ua)
        {
            var match = Regex.Match(ua, @"Chrome/(\d+)\.\d+\.\d+\.\d+");
            return match.Success ? match.Groups[1].Value : "135";
        }

        /// <summary>
        /// 安全判断has_more（适配抖音1/0/true/false返回）
        /// </summary>
        /// <summary>
        /// 判断是否为true（C#7.3兼容版，替换原switch表达式）
        /// </summary>
        /// <param name="value">JToken值</param>
        /// <returns>是否为有效"有更多"标识</returns>
        private bool IsHasMore(JToken value)
        {
            // 第一步：处理null值，直接返回false
            if (value == null)
            {
                return false;
            }

            // 第二步：声明结果变量，默认false
            bool result = false;

            // 第三步：使用传统switch语句（C#7.3支持）
            switch (value.Type)
            {
                case JTokenType.Boolean:
                    // 布尔类型：直接取bool值
                    result = value.Value<bool>();
                    break;
                case JTokenType.Integer:
                    // 整数类型：判断是否等于1
                    result = value.Value<int>() == 1;
                    break;
                case JTokenType.String:
                    // 字符串类型：判断是否是"1"或"true"（忽略大小写）
                    string strValue = value.Value<string>();
                    result = strValue == "1" || strValue.Equals("true", StringComparison.OrdinalIgnoreCase);
                    break;
                default:
                    // 其他类型：返回false
                    result = false;
                    break;
            }

            // 第四步：返回最终结果
            return result;
        }
        #endregion

        #region 核心：Cookie和签名生成（适配抖音新版）

        /// <summary>
        /// 工具方法：创建抖音请求（每次新建，避免复用）
        /// </summary>
        private HttpRequestMessage CreateDouYinRequest(string url, string ua, string referer = null)
        {
            var request = new HttpRequestMessage(HttpMethod.Get, url);

            // 基础请求头（固定逻辑封装）
            request.Headers.Add("User-Agent", ua);
            request.Headers.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
            request.Headers.Add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            //request.Headers.Add("Accept-Encoding", "gzip, deflate, br");
            request.Headers.Add("sec-ch-ua", $"\"Chromium\";v=\"{GetBrowserVersionFromUa(ua)}\", \"Not=A?Brand\";v=\"99\"");
            request.Headers.Add("sec-ch-ua-mobile", "?0");
            request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
            request.Headers.Add("sec-fetch-site", "none");
            request.Headers.Add("sec-fetch-mode", "navigate");
            request.Headers.Add("sec-fetch-user", "?1");
            request.Headers.Add("sec-fetch-dest", "document");
            request.Headers.Add("upgrade-insecure-requests", "1");

            // 可选Referer头（安全添加，避免Uri构造异常）
            if (!string.IsNullOrEmpty(referer))
            {
                request.Headers.Remove("Referer");
                request.Headers.TryAddWithoutValidation("Referer", referer);
            }

            return request;
        }
        /// <summary>
        /// 获取抖音有效Cookie（异步，带重试）
        /// </summary>
        private async Task<(Dictionary<string, string> Cookies, CookieContainer Container)> GetDouYinCookiesAsync(string keyword, string ua)
        {
            var cookieContainer = new CookieContainer();
            var cookies = new Dictionary<string, string>();
            string encodedKeyword = HttpUtility.UrlEncode(keyword);
            string url = $"https://www.douyin.com/search/{encodedKeyword}?type=video";

            // 第一步：创建新的请求实例 → 第一次请求
            using (var request1 = CreateDouYinRequest(url, ua))
            {
                // 绑定Cookie容器
                var handler = new HttpClientHandler
                {
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                    UseCookies = true,
                    CookieContainer = _cookieContainer,
                    AllowAutoRedirect = true,
                    UseDefaultCredentials = false,
                    //Proxy = webProxy,
                    UseProxy = true
                };
                handler.CookieContainer = cookieContainer;

                using (var firstResponse = await _httpClient.SendAsync(request1))
                {
                    firstResponse.EnsureSuccessStatusCode();
                    ExtractCookies(cookieContainer, new Uri(url), cookies);

                    // 重试获取__ac_nonce（仍新建请求）
                    if (!cookies.ContainsKey("__ac_nonce"))
                    {
                        Thread.Sleep(1000);
                        // 新建request2，而非复用request1
                        using (var request2 = CreateDouYinRequest(url, ua))
                        {
                            using (var retryResponse = await _httpClient.SendAsync(request2))
                            {
                                ExtractCookies(cookieContainer, new Uri(url), cookies);
                            }
                        }
                    }
                }
            }

            // 生成__ac_signature（原有逻辑）
            if (cookies.TryGetValue("__ac_nonce", out string acNonce))
            {
                string acSignature = GenerateAcSignature(ua, acNonce);
                cookies["__ac_signature"] = acSignature;
                cookies["__ac_referer"] = "https://www.douyin.com/";
                cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_signature", acSignature));
                cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_referer", cookies["__ac_referer"]));
            }

            // 第二步：新建request3 → 第二次请求（获取ttwid）
            using (var request3 = CreateDouYinRequest(url, ua, referer: url))
            {
                using (var secondResponse = await _httpClient.SendAsync(request3))
                {
                    secondResponse.EnsureSuccessStatusCode();
                    ExtractCookies(cookieContainer, new Uri(url), cookies);
                }
            }

            // 补充Cookie（原有逻辑）
            cookies["x-web-secsdk-uid"] = Guid.NewGuid().ToString("N");
            cookies["csrf_session_id"] = GenerateRandomHexString(16);
            cookies["ttwid"] = cookies.GetValueOrDefault("ttwid", GenerateTtwid());

            return (cookies, cookieContainer);

        }

        /// <summary>
        /// 生成抖音__ac_signature（适配2026版规则）
        /// </summary>
        private string GenerateAcSignature(string ua, string acNonce)
        {
            try
            {
                var engine = new Engine();
                engine.Execute(DouYinUtils.getAbJs()); // 确保JS是抖音最新版
                return engine.Invoke("get_ac_signature", ua, acNonce, DateTime.Now.Ticks.ToString())?.ToString() ?? "";
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"生成__ac_signature失败：{ex.Message}", "签名生成");
                // 降级生成默认签名（避免请求失败）
                return $"_02B4Z6wo00f01{Guid.NewGuid().ToString("N").Substring(0, 20)}{_globalRandom.Next(1000, 9999)}";
            }
        }

        /// <summary>
        /// 生成a_bogus签名（适配抖音最新参数）
        /// </summary>
        private string GenerateABogus(string ua, Dictionary<string, string> parameters)
        {
            try
            {
                if (parameters == null || parameters.Count == 0) return "";

                // 按抖音要求排序参数（关键！）
                var sortedParams = parameters.OrderBy(kv => kv.Key).ToDictionary(kv => kv.Key, kv => kv.Value);
                string paramStr = string.Join("&", sortedParams.Select(kv => $"{HttpUtility.UrlEncode(kv.Key)}={HttpUtility.UrlEncode(kv.Value)}"));

                var engine = new Engine();
                engine.Execute(DouYinUtils.getAbJs());
                return engine.Invoke("get_a_bogus", ua, paramStr, "web").ToString() ?? "";
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"生成a_bogus失败：{ex.Message}", "签名生成");
                return "";
            }
        }

        /// <summary>
        /// 提取Cookie（容器+响应头）
        /// </summary>
        private void ExtractCookies(CookieContainer container, Uri uri, Dictionary<string, string> cookies)
        {
            foreach (Cookie cookie in container.GetCookies(uri))
            {
                if (!cookies.ContainsKey(cookie.Name))
                {
                    cookies[cookie.Name] = cookie.Value;
                }
            }
        }

        /// <summary>
        /// 生成随机16进制字符串
        /// </summary>
        private string GenerateRandomHexString(int length)
        {
            using (var rng = RandomNumberGenerator.Create())
            {
                byte[] bytes = new byte[length];
                rng.GetBytes(bytes);
                StringBuilder sb = new StringBuilder();
                foreach (byte b in bytes) sb.Append(b.ToString("x2"));
                return sb.ToString();
            }
        }

        /// <summary>
        /// 生成默认ttwid（降级用）
        /// </summary>
        private string GenerateTtwid()
        {
            return $"1%7C{GenerateRandomHexString(16)}%7C{DateTime.Now.Ticks}%7C{GenerateRandomHexString(8)}";
        }
        #endregion

        #region 对外接口（异步优化）
        /// <summary>
        /// 搜索抖音视频（主入口，带重试）
        /// </summary>
        public async Task<List<DouYinVideoInfo>> SearchDouyinVideoAsync(string keyword, int[] delayRange = null)
        {
            delayRange = new[] { 500, 1000 };
            int maxRetry = 3;
            int videoMaxCount = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);

            for (int retry = 0; retry < maxRetry; retry++)
            {
                try
                {
                    var result = await StartSearchVideoAsync(keyword, videoMaxCount, videoMaxCount * 2, delayRange);
                    if (result != null && result.Count > 0)
                    {
                        FileUtils.log($"第{retry + 1}次搜索成功，获取到{result.Count}条视频", "搜索主逻辑");
                        return result;
                    }
                    FileUtils.log($"第{retry + 1}次搜索未获取到数据，准备重试", "搜索主逻辑");
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"第{retry + 1}次搜索异常：{ex.Message}", "搜索主逻辑");
                }
                // 重试间隔递增
                Thread.Sleep(_globalRandom.Next(2000 * (retry + 1), 3000 * (retry + 1)));
            }

            FileUtils.log("三次重试均未获取到数据", "搜索主逻辑");
            return new List<DouYinVideoInfo>();
        }

        // 兼容同步调用
        public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] delayRange = null)
        {
            return new SearchVideo3().SearchDouyinVideoAsync(keyword, delayRange).GetAwaiter().GetResult();
        }
        #endregion

        #region 核心搜索逻辑（完全重写）
        /// <summary>
        /// 开始搜索视频（异步，适配抖音最新接口）
        /// </summary>
        private async Task<List<DouYinVideoInfo>> StartSearchVideoAsync(string keyword, int videoCount, int maxCount = 300, int[] delayRange = null)
        {
            var startTime = DateTime.Now;
            var videoList = new List<DouYinVideoInfo>();
            var videoIdSet = new HashSet<string>(); // 去重用，避免重复添加
            delayRange = new[] { 500, 1000 };

            // 初始化基础参数
            int currentPage = 0; // 抖音offset从0开始（原代码从1开始错误！）
            string searchId = null;
            bool hasMore = true;
            int totalRequestCount = 0;
            int loginRetryCount = 0;
            const int maxLoginRetry = 5;

            // 1. 初始化代理
            GetValidProxy();

            // 2. 获取UA和Cookie
            string ua = GetRandomPcUa();
            var (cookies, cookieContainer) = await GetDouYinCookiesAsync(keyword, ua);
            if (cookies.Count == 0)
            {
                FileUtils.log("Cookie获取为空，无法继续搜索", "搜索逻辑");
                return new List<DouYinVideoInfo>();
            }

            // 3. 循环请求数据
            while (hasMore && videoList.Count < videoCount && totalRequestCount < maxCount / 25)
            {
                try
                {
                    totalRequestCount++;
                    CheckProxyExpire();

                    // 构建请求参数（修正所有错误参数！）
                    var resolution = GetRandomPcResolution();
                    var parameters = new Dictionary<string, string>()
                    {
                        { "device_platform", "webapp" },
                        { "aid", "6383" },
                        { "channel", "channel_pc_web" },
                        { "search_channel", "aweme_video_web" },
                        { "enable_history", "1" },
                        { "keyword", keyword },
                        { "search_source", "switch_tab" },
                        { "query_correct_type", "1" },
                        { "is_filter_search", "0" }, // 过滤开关，不是排序！
                        { "sort_type", "0" }, // 排序：0综合/1最多点赞/2最新
                        { "publish_time", "0" }, // 发布时间：0不限/1一天/7一周/180半年
                        { "from_group_id", "" },
                        { "disable_rs", "1" },
                        { "offset", (currentPage * 25).ToString() }, // offset从0开始
                        { "count", "25" },
                        { "need_filter_settings", currentPage == 0 ? "1" : "0" },
                        { "list_type", "single" },
                        { "update_version_code", "170400" },
                        { "pc_client_type", "1" },
                        { "pc_libra_divert", "Windows" },
                        { "support_h265", "1" },
                        { "support_dash", "1" },
                        { "cpu_core_num", _globalRandom.Next(4, 17).ToString() },
                        { "version_code", "170400" },
                        { "version_name", "17.4.0" },
                        { "cookie_enabled", "true" },
                        { "screen_width", resolution[0].ToString() },
                        { "screen_height", resolution[1].ToString() },
                        { "browser_language", _globalRandom.Next(0, 2) == 0 ? "zh-CN" : "zh" },
                        { "browser_platform", "Win32" },
                        { "browser_name", "Chrome" },
                        { "browser_version", GetBrowserVersionFromUa(ua) },
                        { "browser_online", "true" },
                        { "engine_name", "Blink" },
                        { "engine_version", GetBrowserVersionFromUa(ua) },
                        { "os_name", "Windows" },
                        { "os_version", "10" },
                        { "device_memory", _globalRandom.Next(4, 17).ToString() },
                        { "platform", "PC" },
                        { "downlink", _globalRandom.Next(5, 50).ToString() },
                        { "effective_type", _globalRandom.Next(0, 2) == 0 ? "4g" : "wifi" },
                        { "round_trip_time", _globalRandom.Next(50, 200).ToString() }
                    };

                    // 添加search_id（分页必需）
                    if (!string.IsNullOrEmpty(searchId))
                    {
                        parameters["search_id"] = searchId;
                    }

                    // 生成a_bogus并添加
                    //string aBogus = GenerateABogus(ua, parameters);
                    //if (!string.IsNullOrEmpty(aBogus))
                    //{
                    //    parameters["a_bogus"] = aBogus;
                    //}

                    // 构建请求URL
                    string apiUrl = "https://www.douyin.com/aweme/v1/web/search/item/";
                    string queryString = string.Join("&", parameters.Select(kv => $"{HttpUtility.UrlEncode(kv.Key)}={HttpUtility.UrlEncode(kv.Value)}"));
                    string requestUrl = $"{apiUrl}?{queryString}";

                    // 构建请求头
                    var request = new HttpRequestMessage(HttpMethod.Get, requestUrl);
                    request.Headers.Add("User-Agent", ua);
                    request.Headers.Add("Accept", "application/json, text/plain, */*");
                    request.Headers.Add("Accept-Language", "zh-CN,zh;q=0.9");
                    //request.Headers.Add("Accept-Encoding", "gzip, deflate, br");
                    request.Headers.Add("Referer", $"https://www.douyin.com/search/{HttpUtility.UrlEncode(keyword)}?type=video");
                    request.Headers.Add("sec-ch-ua", $"\"Chromium\";v=\"{GetBrowserVersionFromUa(ua)}\", \"Not=A?Brand\";v=\"99\"");
                    request.Headers.Add("sec-ch-ua-mobile", "?0");
                    request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
                    request.Headers.Add("sec-fetch-site", "same-origin");
                    request.Headers.Add("sec-fetch-mode", "cors");
                    request.Headers.Add("sec-fetch-dest", "empty");
                    request.Headers.Add("x-requested-with", "XMLHttpRequest");
                    request.Headers.Add("x-web-route-path", "/search");

                    // 添加Cookie到请求头
                    string cookieHeader = string.Join("; ", cookies.Select(kv => $"{kv.Key}={kv.Value}"));
                    request.Headers.TryAddWithoutValidation("Cookie", cookieHeader);

                    // 发送请求（异步，非阻塞）
                    using (var response = await _httpClient.SendAsync(request))
                    {
                        string responseContent = await response.Content.ReadAsStringAsync();

                        // 处理风控/空响应
                        if (string.IsNullOrEmpty(responseContent) || responseContent.Length < 10)
                        {
                            FileUtils.log($"响应内容为空，切换代理重试", "响应处理");
                            GetValidProxy(); // 切换代理
                            cookies = new Dictionary<string, string>();
                            (cookies, cookieContainer) = await GetDouYinCookiesAsync(keyword, ua); // 重新获取Cookie
                            Thread.Sleep(_globalRandom.Next(3000, 5000));
                            continue;
                        }

                        // 解析响应（强类型JObject，避免dynamic错误）
                        JObject responseObj = JObject.Parse(responseContent);
                        int statusCode = responseObj["status_code"]?.Value<int>() ?? -1;

                        // 处理登录风控
                        if (responseObj["search_nil_info"] != null)
                        {
                            string nilType = responseObj["search_nil_info"]["search_nil_type"]?.Value<string>() ?? "";
                            if (nilType.Contains("web_need_login"))
                            {
                                loginRetryCount++;
                                if (loginRetryCount >= maxLoginRetry)
                                {
                                    FileUtils.log("登录风控重试次数超限，停止搜索", "响应处理");
                                    break;
                                }
                                FileUtils.log($"登录风控，第{loginRetryCount}次重试", "响应处理");
                                // 清空Cookie重新获取
                                cookies = new Dictionary<string, string>();
                                (cookies, cookieContainer) = await GetDouYinCookiesAsync(keyword, ua);
                                Thread.Sleep(_globalRandom.Next(5000, 8000));
                                continue;
                            }
                            else
                            {
                                FileUtils.log($"未知风控类型：{nilType}", "响应处理");
                                break;
                            }
                        }

                        // 处理正常响应
                        if (statusCode == 0 && responseObj["data"] != null && responseObj["data"].Type == JTokenType.Array)
                        {
                            // 获取search_id（修正！）
                            if (currentPage == 0)
                            {
                                searchId = responseObj["extra"]?["search_id"]?.Value<string>() ?? responseObj["extra"]?["logid"]?.Value<string>() ?? "";
                            }

                            // 判断是否有下一页
                            hasMore = IsHasMore(responseObj["has_more"]);

                            // 解析视频数据
                            foreach (JToken item in responseObj["data"])
                            {
                                JToken awemeInfo = item["aweme_info"];
                                if (awemeInfo == null) continue;

                                // 转换为实体
                                DouYinVideoInfo video = awemeInfo.ToObject<DouYinVideoInfo>();
                                if (video == null || string.IsNullOrEmpty(video.aweme_id) || string.IsNullOrEmpty(video?.video?.play_addr?.file_hash))
                                {
                                    continue;
                                }

                                // 去重并添加
                                if (!videoIdSet.Contains(video.aweme_id))
                                {
                                    // 格式化数据
                                    video.desc = ShortVideoUtils.formatDesc(video.desc);
                                    if (video.video.duration > 0)
                                    {
                                       long duration = ShortVideoUtils.formatDuration(video.video.duration);
                                        video.video.duration = duration;
                                    }

                                    videoList.Add(video);
                                    videoIdSet.Add(video.aweme_id);

                                    // 达到数量上限则退出
                                    if (videoList.Count >= videoCount)
                                    {
                                        hasMore = false;
                                        break;
                                    }
                                }
                            }
                        }
                        else
                        {
                            FileUtils.log($"响应状态码异常：{statusCode}，响应内容：{responseContent.Substring(0, Math.Min(500, responseContent.Length))}", "响应处理");
                            break;
                        }
                    }

                    // 分页延迟（随机化，避免固定间隔）
                    int delay = _globalRandom.Next(delayRange[0], delayRange[1]);
                    Thread.Sleep(delay);
                    currentPage++;
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"第{currentPage + 1}页请求异常：{ex.Message}", "搜索逻辑");
                    // 异常时切换代理+重试
                    GetValidProxy();
                    Thread.Sleep(_globalRandom.Next(3000, 5000));
                    continue;
                }
            }

            // 统计日志
            TimeSpan cost = DateTime.Now - startTime;
            FileUtils.log($"搜索完成：共请求{totalRequestCount}页，获取视频{videoList.Count}条，耗时{cost.TotalMilliseconds:F0}ms", "搜索统计");

            return videoList;
        }
        #endregion
    }
}