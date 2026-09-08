using douyin.Utils;
using Jint;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
// 新增 RestSharp 引用
using RestSharp;
using RestSharp.Authenticators;
//using RestSharp.Cookies;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using Swan;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Web;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class SearchVideoByPc
    {
        private static int chromeVersion = 130;

        // 代理相关属性
        private static volatile ProxyIpVo _currentProxyIp;
        private static volatile bool _isUseProxy;
        private static long _proxyExpireTime = 0;
        private static readonly object _proxyIpLock = new object();

        /// <summary>
        /// 调用抖音搜索视频接口，手动释放资源
        /// </summary>
        /// <param name="keyword"></param>
        /// <param name="next"></param>
        /// <returns></returns>
        public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] next = null)
        {
            SearchVideoByPc searchVideo = null;
            try
            {
                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
                next = next ?? new int[] { 50, 100 };
                searchVideo = new SearchVideoByPc();
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
        /// PC端浏览器User-Agent列表（覆盖Chrome/Edge/Firefox/Safari/360/QQ浏览器，Windows/macOS）
        /// </summary>
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
        private static readonly Random _random = new Random();
        /// <summary>
        /// 随机获取一个PC端UA（线程安全）
        /// </summary>
        /// <returns>随机UA字符串</returns>
        public static string GetRandomPcUa()
        {
            lock (_random) // 保证多线程下Random的安全性
            {
                int index = _random.Next(0, _pcUaList.Count);
                return _pcUaList[index];
            }
        }
        /// <summary>
        /// 开始搜索视频
        /// </summary>
        /// <param name="_keyword"></param>
        /// <param name="videoCount"></param>
        /// <param name="maxCount"></param>
        /// <param name="nextTime"></param>
        /// <param name="versionAdd"></param>
        /// <param name="allowRetry">是否允许重试</param>
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
            bool allowRetry = true;
            int loginRetryCount = 0;

            string searchId = null;
            var sortType = "0"; // 排序方式 0:综合排序, 1:最多点赞, 2:最新发布
            var publishTime = "180"; // 发布时间 0:不限, 1:一天内, 7:一周内, 180:半年内
            var filterDuration = ""; // 视频时长

            (Dictionary<string, string> cookiesMap, CookieContainer cookieContainer)? cookiesRes = null;
            string ua = "";

            while (!shouldExit)
            {
                if (cookiesRes == null || !cookiesRes.HasValue)
                {
                    string version = getChromeVersion(versionAdd);
                    ua = GetRandomPcUa();// $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version}.0.0.0 Safari/537.36";
                    cookiesRes = getCookieContainer(_keyword, ua);
                }

                // ========== 变更1：构造 RestRequest 参数（替代原 HttpClient 的参数拼接） ==========
                var request = new RestRequest(apiUrl, Method.Get);

                // 添加请求头（替代原 headers 字典）
                request.AddHeader("User-Agent", ua);
                request.AddHeader("Accept", "application/json, text/plain, */*");
                request.AddHeader("referer", $"https://www.douyin.com/jingxuan/search/{HttpUtility.UrlEncode(_keyword)}?aid={Guid.NewGuid().ToString()}&type=general");

                // 添加请求参数（替代原 parameters 字典）
                request.AddParameter("device_platform", "webapp");
                request.AddParameter("aid", "6383");
                request.AddParameter("channel", "channel_pc_web");
                request.AddParameter("search_channel", "aweme_video_web");
                request.AddParameter("enable_history", "1");
                request.AddParameter("keyword", _keyword);
                request.AddParameter("search_source", "switch_tab");
                request.AddParameter("query_correct_type", "1");
                request.AddParameter("is_filter_search", "0");
                request.AddParameter("from_group_id", "");
                request.AddParameter("disable_rs", "1");
                request.AddParameter("offset", ((currentPage - 1) * 25).ToString());
                request.AddParameter("count", "25");
                request.AddParameter("need_filter_settings", currentPage == 1 ? "1" : "0");
                request.AddParameter("list_type", "");
                request.AddParameter("update_version_code", "170400");
                request.AddParameter("pc_client_type", "1");
                request.AddParameter("pc_libra_divert", "Windows");
                request.AddParameter("support_h265", "1");
                request.AddParameter("support_dash", "1");
                request.AddParameter("cpu_core_num", RandChoice("4", "8", "16"));
                request.AddParameter("version_code", "170400");
                request.AddParameter("version_name", "17.4.0");
                request.AddParameter("cookie_enabled", "true");
                request.AddParameter("screen_width", resolution[0].ToString());
                request.AddParameter("screen_height", resolution[1].ToString());
                request.AddParameter("browser_language", RandChoice("zh-CN", "en"));
                request.AddParameter("browser_platform", "Win32");
                request.AddParameter("browser_name", "Chrome");
                //request.AddParameter("browser_version", getBrowserVersion(ua));
                request.AddParameter("browser_online", "true");
                request.AddParameter("engine_name", "Blink");
                //request.AddParameter("engine_version", getBrowserVersion(ua));
                request.AddParameter("os_name", "Windows");
                request.AddParameter("os_version", "10");
                request.AddParameter("device_memory", RandChoice("4", "8", "16"));
                request.AddParameter("platform", "PC");
                request.AddParameter("downlink", RandInt(5, 50).ToString());
                request.AddParameter("effective_type", RandChoice("4g", "wifi"));
                request.AddParameter("round_trip_time", RandInt(50, 200).ToString());

                // search_id
                if (currentPage > 1 && !string.IsNullOrEmpty(searchId))
                {
                    request.AddParameter("search_id", searchId);
                }
                // 设置筛选条件
                request.AddParameter("publish_time", publishTime);
                request.AddParameter("is_filter_search", sortType);

                // 生成 a_bogus（复用原有逻辑）
                // ========== 临时构造参数字典用于生成 a_bogus（原有逻辑不变） ==========
                var parameters = new Dictionary<string, string>();
                foreach (var param in request.Parameters.Where(p => p.Type == ParameterType.GetOrPost))
                {
                    parameters[param.Name] = param.Value.ToString();
                }
                string a_bogus = getAb(ua, parameters);
                if (!string.IsNullOrEmpty(a_bogus))
                {
                    request.AddParameter("a_bogus", a_bogus);
                }
                CheckProxyExpire();
                // 开始就使用代理
                UseProxyIP();
                
                var restClientOptions = new RestClientOptions(apiUrl)
                {
                    // 绑定 Cookie 容器（替代原 handler.CookieContainer）
                    CookieContainer = cookiesRes.HasValue ? cookiesRes.Value.cookieContainer : new CookieContainer(),
                    // 允许自动重定向（保持和原代码一致）
                    FollowRedirects = true,
                    // 超时时间（可根据需要调整）
                    //Timeout = TimeSpan.FromSeconds(30)
                };

                // 设置代理（替代原 WebProxy 逻辑）
                if (_currentProxyIp != null && !string.IsNullOrEmpty(_currentProxyIp.ip) && !string.IsNullOrEmpty(_currentProxyIp.port))
                {
                    var proxy = new WebProxy($"{_currentProxyIp.ip}:{_currentProxyIp.port}");
                    if (!string.IsNullOrEmpty(_currentProxyIp.proxyUsername) && !string.IsNullOrEmpty(_currentProxyIp.proxyPassword))
                    {
                        proxy.Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword);
                    }
                    restClientOptions.Proxy = proxy;
                    //restClientOptions.UseProxy = true;
                }
                else
                {
                    //restClientOptions.UseProxy = false;
                }

                // 创建 RestClient 实例
                var client = new RestClient(restClientOptions);

                try
                {
                    // ========== 变更3：发送请求（替代原 httpClient.SendAsync） ==========
                    RestResponse response = client.Execute(request);

                    // 检查响应状态（替代原 response.EnsureSuccessStatusCode()）
                    if (!response.IsSuccessful)
                    {
                        FileUtils.log($"请求失败：Status Code = {response.StatusCode}, Error Message = {response.ErrorMessage}", "搜爆款-RestClient请求失败");
                        shouldExit = true;
                        continue;
                    }

                    string responseContent = response.Content;
                    if (string.IsNullOrEmpty(responseContent) || responseContent.Length < 10)
                    {
                        FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, responseContent.Length < 10， responseContent = {responseContent}", "搜爆款出现风控，将使用代理重试");

                        if (allowRetry)
                        {
                            UseProxyIP();
                            allowRetry = false;
                            continue;
                        }
                        else
                        {
                            shouldExit = true;
                            continue;
                        }
                    }

                    // 解析 JSON（原有逻辑不变）
                    dynamic root = JsonConvert.DeserializeObject(responseContent);

                    if (root.search_nil_info != null)
                    {
                        string searchNilType = root.search_nil_info.search_nil_type?.ToString();
                        if (!string.IsNullOrEmpty(searchNilType))
                        {
                            cookiesRes = null;
                            if (searchNilType.Contains("web_need_login"))
                            {
                                loginRetryCount++;
                                if (loginRetryCount > 10)
                                {
                                    FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款web_need_login，当前是第{loginRetryCount}次，已超出重试次数");
                                    shouldExit = true;
                                }
                                else
                                {
                                    FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款web_need_login，将重新请求，当前是第{loginRetryCount}次");
                                    Thread.Sleep(5000);
                                }

                                continue;
                            }
                            else if (allowRetry)
                            {
                                FileUtils.log($"videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款出现风控，将使用代理重试");
                                UseProxyIP();
                                allowRetry = false;
                                Thread.Sleep(5000);
                                continue;
                            }
                            else
                            {
                                shouldExit = true;
                                continue;
                            }
                        }
                        else
                        {
                            shouldExit = true;
                            continue;
                        }
                    }

                    var data = root.data;
                    if ((root?.status_code ?? -1) == 0 && data.Type == JTokenType.Array)
                    {
                        // 处理 search_id
                        if (currentPage == 1 && root.extra?.logid != null)
                        {
                            searchId = root.extra.logid.ToString();
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
                            if (!string.IsNullOrEmpty(temp?.video?.play_addr?.file_hash)) //?? ""
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

                int next = random.Next(nextTime[0], nextTime[1]);
                Thread.Sleep(next);
                currentPage++;
            }

            // 去重（原有逻辑不变）
            List<DouYinVideoInfo> result = items.GroupBy(item => item.aweme_id).Select(group => group.First()).ToList();

            FileUtils.log($"搜爆款共爬取{currentPage - 1}页，总计{count}个作品, 总视频{items.Count}个，总图文{count - items.Count}个，去重后视频{result?.Count ?? 0}个, 一共用时{(DateTime.Now - currentNow).TotalMilliseconds}ms", "搜爆款-统计");
            return result;
        }

        /// <summary>
        /// 获取cookies（仅替换 HttpClient 为 RestClient，业务逻辑不变）
        /// </summary>
        /// <param name="_keyword"></param>
        /// <param name="ua"></param>
        /// <returns></returns>
        public (Dictionary<string, string> cookiesMap, CookieContainer cookieContainer) getCookieContainer(string _keyword, string ua)
        {
            CookieContainer cookieContainer = new CookieContainer();
            var cookies = new Dictionary<string, string>();
            var encodedKeyword = HttpUtility.UrlEncode(_keyword);
            var url = $"https://www.douyin.com/search/{encodedKeyword}?type=video";

            try
            {
                UseProxyIP();
                CheckProxyExpire();

                // ========== 变更4：Cookie获取逻辑替换为 RestClient ==========
                var restClientOptions = new RestClientOptions(url)
                {
                    CookieContainer = cookieContainer,
                    FollowRedirects = true,
                    //Timeout = TimeSpan.FromSeconds(30)
                };

                // 设置代理（和原逻辑一致）
                if (_currentProxyIp != null && !string.IsNullOrEmpty(_currentProxyIp.ip) && !string.IsNullOrEmpty(_currentProxyIp.port))
                {
                    var proxy = new WebProxy($"{_currentProxyIp.ip}:{_currentProxyIp.port}");
                    if (!string.IsNullOrEmpty(_currentProxyIp.proxyUsername) && !string.IsNullOrEmpty(_currentProxyIp.proxyPassword))
                    {
                        proxy.Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword);
                    }
                    restClientOptions.Proxy = proxy;
                    //restClientOptions.UseProxy = true;
                }
                else
                {
                   // restClientOptions.UseProxy = false;
                }

                var client = new RestClient(restClientOptions);

                // 构造第一次请求（获取初始Cookie）
                var firstRequest = new RestRequest(url, Method.Get);
                firstRequest.AddHeader("User-Agent", ua);
                firstRequest.AddHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                firstRequest.AddHeader("sec-ch-ua-mobile", "?0");
                firstRequest.AddHeader("sec-ch-ua-platform", "\"Windows\"");
                firstRequest.AddHeader("upgrade-insecure-requests", "1");
                firstRequest.AddHeader("sec-fetch-site", "none");
                firstRequest.AddHeader("sec-fetch-mode", "navigate");
                firstRequest.AddHeader("sec-fetch-user", "?1");
                firstRequest.AddHeader("sec-fetch-dest", "document");
                firstRequest.AddHeader("accept-language", "zh-CN,zh;q=0.9");
                firstRequest.AddHeader("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
                firstRequest.AddHeader("priority", "u=0, i");

                //cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("ttwid", "1%7C7HqI_MPAME8-ojZdo5J7Xv__5n3yg75d-F_fF9WD9W4%7C1768029448%7C2dd543ddea40be14504dd49482f77ef2230491b62f0d598133026499b9dbfda7"));
                //cookies.Add("ttwid", "1%7C7HqI_MPAME8-ojZdo5J7Xv__5n3yg75d-F_fF9WD9W4%7C1768029448%7C2dd543ddea40be14504dd49482f77ef2230491b62f0d598133026499b9dbfda7");
                //return (cookies, cookieContainer);
                FileUtils.log("第一次请求获取初始Cookie...");
                var firstResponse = client.Execute(firstRequest);
                if (!firstResponse.IsSuccessful)
                {
                    FileUtils.log($"第一次请求失败：{firstResponse.StatusCode}");
                    return (cookies, cookieContainer);
                }

                // 提取Cookie（原有逻辑不变）
                ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);

                if (!cookies.ContainsKey("__ac_nonce"))
                {
                    FileUtils.log("警告: 未获取到__ac_nonce Cookie，尝试从响应头提取");
                    ExtractCookiesFromHeaders(firstResponse.Headers, cookieContainer, cookies);
                }

                // 重试逻辑（原有不变）
                if (!cookies.ContainsKey("__ac_nonce"))
                {
                    FileUtils.log("__ac_nonce获取失败，重试一次...");
                    Thread.Sleep(1000);
                    var retryResponse = client.Execute(firstRequest);
                    ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
                    ExtractCookiesFromHeaders(retryResponse.Headers, cookieContainer, cookies);
                }

                if (!cookies.TryGetValue("__ac_nonce", out var acNonce))
                {
                    FileUtils.log("错误: 无法获取__ac_nonce Cookie");
                    return (cookies, cookieContainer);
                }

                // 生成__ac_signature（原有逻辑不变）
                var acSignature = GetAcSignature(ua, acNonce);
                cookies["__ac_signature"] = acSignature;
                cookies["__ac_referer"] = "__ac_blank";

                // 添加生成的签名到CookieContainer（原有逻辑不变）
                //cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_signature", acSignature));
                //cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_referer", "__ac_blank"));

                // 第二次请求（获取ttwid）
                FileUtils.log("第二次请求尝试获取ttwid...");

                var secondRequest = new RestRequest(url, Method.Get);
                secondRequest.AddHeader("User-Agent", ua);
                secondRequest.AddHeader("Referer", url); // RestSharp 的 Referer 头

                var secondResponse = client.Execute(secondRequest);
                if (secondResponse.IsSuccessful)
                {
                    ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
                }

                // 第三次请求（重试获取ttwid）
                if (!cookies.ContainsKey("ttwid"))
                {
                    FileUtils.log("警告: 未获取到ttwid Cookie，尝试第三次请求...");
                    Thread.Sleep(1500);
                    var thirdResponse = client.Execute(secondRequest);
                    if (thirdResponse.IsSuccessful)
                    {
                        ExtractCookiesFromContainer(cookieContainer, new Uri(url), cookies);
                    }
                }

                // 添加额外Cookie（原有逻辑不变）
                //cookies["x-web-secsdk-uid"] = Guid.NewGuid().ToString();
                cookies["csrf_session_id"] = GenerateRandomHexString(16);
                cookies["SEARCH_RESULT_LIST_TYPE"] = "%22single%22";
                cookies["home_can_add_dy_2_desktop"] = "%220%22";
                cookies["hevc_supported"] = "true";

                // 补充Cookie（原有逻辑）
                cookies["x-web-secsdk-uid"] = Guid.NewGuid().ToString("N");
                cookies["ttwid"] =  GenerateTtwid();

                FileUtils.log("\n初始化cookies信息:");
            }
            catch (Exception ex)
            {
                FileUtils.log($"获取Cookie时出错: {ex.Message}");
            }

            return (cookies, cookieContainer);
        }
        /// <summary>
        /// 生成默认ttwid（降级用）
        /// </summary>
        private string GenerateTtwid()
        {
            return $"1%7C{GenerateRandomHexString(16)}%7C{DateTime.Now.Ticks}%7C{GenerateRandomHexString(8)}";
        }
        // ========== 以下方法均为原有逻辑，无变更 ==========
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
                engine.Execute(DouYinUtils.getAbJs());

                if (parameters == null || parameters.Count == 0)
                    return "";

                var encodedParts = parameters.Select(pair =>
                    $"{HttpUtility.UrlEncode(pair.Key)}={HttpUtility.UrlEncode(pair.Value)}");

                string paramsStr = string.Join("&", encodedParts);

                var result = engine.Invoke("get_a_bogus", ua, paramsStr, "");

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

        // 适配 RestSharp 的响应头提取（新增重载方法）
        private void ExtractCookiesFromHeaders(IEnumerable<Parameter> headers, CookieContainer cookieContainer, Dictionary<string, string> cookies)
        {
            var setCookieHeaders = headers.Where(h => h.Name.Equals("Set-Cookie", StringComparison.OrdinalIgnoreCase));
            foreach (var header in setCookieHeaders)
            {
                var cookieValue = header.Value.ToString();
                var parts = cookieValue.Split(';');
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
            Random random = new Random();
            int randomIndex = random.Next(resolutions.Count);
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

        private void CheckProxyExpire()
        {
            lock (_proxyIpLock)
            {
                if (!_isUseProxy)
                {
                    return;
                }

                long currentTime = ServerTimeUtils.getCurrentTime();

                if (currentTime >= _proxyExpireTime)
                {
                    FileUtils.log($"搜爆款代理IP已过期", $"当前时间：{currentTime}，过期时间：{_proxyExpireTime}");
                    _isUseProxy = false;
                    _currentProxyIp = null;
                }
            }
        }

        private void UseProxyIP()
        {
            lock (_proxyIpLock)
            {
                if (_isUseProxy)
                {
                    return;
                }

                ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(false, 0);
                if (proxyIpVo != null)
                {
                    _isUseProxy = true;
                    _currentProxyIp = proxyIpVo;
                    _proxyExpireTime = proxyIpVo.expireTime;
                    FileUtils.log($"搜爆款代理IP设置成功", $"IP: {proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}");
                }
                else
                {
                    FileUtils.log($"获取搜爆款代理IP失败", $"从服务器获取代理IP返回null");
                }
            }
        }
    }
}