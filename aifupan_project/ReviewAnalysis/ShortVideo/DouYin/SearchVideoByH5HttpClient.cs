using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    /// <summary>
    /// 抖音H5搜索视频（HttpClient实现 + 异步优化）
    /// </summary>
    public class SearchVideoByH5HttpClient
    {
        #region 常量配置（抽离便于维护）
        private const int ChromeVersion = 130;
        private const int DefaultPageSize = 20; // 每页条数
        private const int MaxCrawlCount = 150; // 最大爬取总数
        private const int MaxRequestPages = 20; // 最大请求页数（避免死循环）
        private const int RequestTimeoutSeconds = 10; // 请求超时时间
        private const string SearchApiBaseUrl = "https://so.douyin.com/s";
        private const string SearchDataApiUrl = "https://so.douyin.com/s/fe/api/aweme/v2/search/msite/general/search/single/";
        private const string SearchPd = "general"; // 搜索类型

        // 移动端UA列表（抽离为常量）
        //private static readonly List<string> _mobileUaList = new List<string>
        //{
        //    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.4936.1160 Mobile Safari/537.36",
        //    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1",
        //    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
        //    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Mobile/15E148 Safari/604.1",
        //    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
        //    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        //};
//        private static readonly List<string> _mobileUaList = new List<string>
//{
//    #region Android 通用版（覆盖Android 8.0-14，Chrome内核）
//    "Mozilla/5.0 (Linux; Android 8.0.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 9; Pixel 3 Build/PQ3A.190801.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 10; Pixel 4 Build/QQ3A.200805.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 11; Pixel 5 Build/RQ3A.210805.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SQ3A.220705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 7.1.2; Redmi Note 5 Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36",
//    #endregion

//    #region Android 华为机型（Mate/P系列，鸿蒙兼容）
//    "Mozilla/5.0 (Linux; Android 10; HUAWEI Mate 30 Pro Build/HUAWEIMate30Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 11; HUAWEI P40 Pro Build/ANP-AN00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 12; HUAWEI Mate 40 Pro Build/NOH-AN00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; HarmonyOS 2.0; HUAWEI Mate 50 Pro Build/HMOS2.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; HarmonyOS 3.0; HUAWEI P60 Pro Build/HMOS3.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 13; HUAWEI Nova 11 Build/CHA-AL80) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
//    #endregion

//    #region Android 小米机型（Redmi/小米数字系列）
//    "Mozilla/5.0 (Linux; Android 12; Xiaomi 12 Pro Build/SKQ1.220213.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 13; Xiaomi 13 Ultra Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 11; Redmi K50 Build/RKQ1.200825.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 10; Redmi Note 10 Pro Build/QP1A.190711.020) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 14; Xiaomi 14 Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
//    #endregion

//    #region Android vivo/OPPO 机型
//    "Mozilla/5.0 (Linux; Android 12; vivo X90 Pro Build/TP1A.220829.005) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 13; vivo X100 Pro Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 11; OPPO Find X5 Pro Build/PEPM00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 14; OPPO Find X6 Pro Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 12; realme GT Neo5 Build/SKQ1.220213.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
//    #endregion

//    #region Android 三星机型
//    "Mozilla/5.0 (Linux; Android 12; SM-S22 Ultra Build/SP2A.220305.013) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 13; SM-S23 Ultra Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 14; SM-S24 Ultra Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 11; Galaxy S21 Ultra Build/RP1A.200720.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
//    #endregion

//    #region iOS iPhone 机型（iOS 15-18，不同机型）
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/360.0.743255906 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone 12; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone 13 Pro; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone 14 Pro Max; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPhone 15 Ultra; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
//    #endregion

//    #region iOS iPad 移动端模式
//    "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
//    "Mozilla/5.0 (iPad Pro; CPU OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
//    #endregion

//    #region 微信内置浏览器（Android/iOS）
//    "Mozilla/5.0 (Linux; Android 12; SM-G9980 Build/SP2A.220305.013; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.500.1000 WeChat/arm64",
//    "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230705.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/134.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.510.1000 WeChat/arm64",
//    //"Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.500(0x18003229) NetType/WIFI MiniProgramEnv/ios",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.510(0x18003329) NetType/5G MiniProgramEnv/ios",
//    #endregion

//    #region QQ浏览器（Android/iOS）
//    "Mozilla/5.0 (Linux; Android 12; Redmi K60 Build/SKQ1.220213.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36 MQQBrowser/14.8.0",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1 MQQBrowser/14.8.0",
//    #endregion

//    #region UC浏览器（Android）
//    "Mozilla/5.0 (Linux; Android 11; vivo X80 Build/TP1A.220829.005) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36 UCBrowser/15.5.0.1000",
//    "Mozilla/5.0 (Linux; Android 13; HUAWEI Mate 50 Pro Build/HMOS2.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36 UCBrowser/15.6.0.1000",
//    #endregion

//    #region 低版本兼容（适配老旧移动端）
//    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.4936.1160 Mobile Safari/537.36",
//    "Mozilla/5.0 (Linux; Android 7.0; SM-G930F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Mobile Safari/537.36",
//    "Mozilla/5.0 (iPhone; CPU iPhone OS 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
//    #endregion
//};
//        #endregion

        #region 代理静态字段（线程安全）
        private static volatile ProxyIpVo _currentProxyIp;
        private static volatile bool _isUseProxy;
        private static long _proxyExpireTime = 0;
        private static readonly object _proxyIpLock = new object();
        #endregion

        #region 实例字段
        private readonly string _keyword;
        private readonly string _reloadNavStart;
        private readonly int _count = DefaultPageSize;
        private readonly int _maxCount = MaxCrawlCount;
        private readonly string _ua;
        private readonly string _innerWidth;
        private readonly string _innerHeight;
        private readonly List<string> _unableUa = new List<string>();
        private readonly bool _useProxy = true;
        private readonly CookieContainer _cookieContainer = new CookieContainer();
        private Dictionary<string, string> _cookies = new Dictionary<string, string>();
        private readonly Random _random = new Random();
        private HttpClient _httpClient; // 实例级HttpClient，避免跨实例污染
        #endregion

        /// <summary>
        /// 异步调用抖音搜索视频接口（优化后）
        /// </summary>
        /// <param name="keyword">搜索关键词</param>
        /// <param name="next">分页偏移（保留原参数）</param>
        /// <returns>视频列表</returns>
        public static async Task<List<DouYinVideoInfo>> SearchDouyinVideoAsync(string keyword, int[] next = null)
        {
            SearchVideoByH5HttpClient searchVideo = null;
            try
            {
                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", MaxCrawlCount);
                next = next ?? new int[] { 50, 100 };
                

                List<DouYinVideoInfo> list = null;
                // 最多重试3次
                for (int i = 0; i < 1; i++)
                {
                    searchVideo = new SearchVideoByH5HttpClient(keyword);
                    list = await searchVideo.StartSearchVideoAsync();
                    if (list != null && list.Count > 0)
                    {
                        break;
                    }
                    FileUtils.log($"【重试{i + 1}次】没有查询到数据，等待1秒后重试...");
                    await Task.Delay(1000);
                }

                if (list == null || list.Count == 0)
                {
                    FileUtils.log("爆款请求三次还是没有数据");
                    return new List<DouYinVideoInfo>();
                }

                return list;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"Message = {ex.Message}, StackTrace = {ex.StackTrace}", "执行抖音搜索视频报错");
                return new List<DouYinVideoInfo>();
            }
            finally
            {
                // 释放HttpClient资源
                searchVideo?.Dispose();
            }
        }

        /// <summary>
        /// 构造函数（初始化HttpClient + 基础参数）
        /// </summary>
        /// <param name="keyword">搜索关键词</param>
        public SearchVideoByH5HttpClient(string keyword)
        {
            _keyword = keyword;
            _reloadNavStart = ((long)(DateTime.UtcNow - new DateTime(1970, 1, 1)).TotalMilliseconds).ToString();

            // 随机UA和屏幕尺寸
            //_ua = _mobileUaList[_random.Next(_mobileUaList.Count)];
            _ua = MobileUaHelper.GetRandomMobileUa();
            _innerWidth = _random.Next(768, 1279).ToString();
            _innerHeight = _random.Next(568, 1792).ToString();
            CheckProxyExpire();
            if (_useProxy) UseProxyIP();
            // 初始化HttpClient（带代理+Cookie）
            InitHttpClient();

            // 初始化Cookie和代理
            _cookies = GetCookiesSync(); // Cookie获取暂时同步（可后续改异步）


        }

        /// <summary>
        /// 初始化HttpClient（核心优化：独立实例+正确配置）
        /// </summary>
        private void InitHttpClient()
        {
            var handler = new HttpClientHandler
            {
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                CookieContainer = _cookieContainer,
                UseCookies = true,
                AllowAutoRedirect = true,
                ServerCertificateCustomValidationCallback = (sender, cert, chain, sslPolicyErrors) => true, // 忽略SSL验证
                                                                                                            //Timeout = TimeSpan.FromSeconds(RequestTimeoutSeconds)
            };

            // 配置代理
            if (_useProxy && _currentProxyIp != null)
            {
                var proxy = new WebProxy($"{_currentProxyIp.ip}:{_currentProxyIp.port}");
                if (!string.IsNullOrEmpty(_currentProxyIp.proxyUsername) && !string.IsNullOrEmpty(_currentProxyIp.proxyPassword))
                {
                    proxy.Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword);
                }
                handler.Proxy = proxy;
                handler.UseProxy = true;
            }

            // 创建HttpClient（实例级，避免静态污染）
            _httpClient = new HttpClient(handler);
            _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(_ua);
            _httpClient.DefaultRequestHeaders.Accept.ParseAdd("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            _httpClient.DefaultRequestHeaders.Add("pragma", "no-cache");
            _httpClient.DefaultRequestHeaders.Add("cache-control", "no-cache");
            _httpClient.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
            _httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
            _httpClient.DefaultRequestHeaders.Add("priority", "u=0, i");
            _httpClient.Timeout = TimeSpan.FromSeconds(RequestTimeoutSeconds);
        }

        /// <summary>
        /// 时间戳转换为格式化时间
        /// </summary>
        private string ConvertTimestamp(string timestamp)
        {
            if (string.IsNullOrEmpty(timestamp)) return null;

            long ts;
            if (!long.TryParse(timestamp, out ts)) return null;

            // 处理13位毫秒时间戳
            if (timestamp.Length == 13) ts /= 1000;

            try
            {
                return DateTimeOffset.FromUnixTimeSeconds(ts).LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss");
            }
            catch
            {
                return null;
            }
        }
        /// <summary>
        /// DateTime 转 Unix 时间戳（毫秒级）
        /// </summary>
        public static long ConvertDateTimeToUnixTimestampMs(DateTime dateTime)
        {
            DateTime unixEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            DateTime targetTime = dateTime.Kind == DateTimeKind.Local ? dateTime.ToUniversalTime() : dateTime;
            TimeSpan timeSpan = targetTime - unixEpoch;
            return (long)timeSpan.TotalMilliseconds; // 毫秒级
        }

        /// <summary>
        /// 获取当前格式化时间
        /// </summary>
        private string GetCurrentTime() => DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

        /// <summary>
        /// 解析UA，返回平台和版本信息（逻辑不变）
        /// </summary>
        private Dictionary<string, string> ParseUA(string ua)
        {
            var result = new Dictionary<string, string>
            {
                { "webapp_platform", "unknown" },
                { "webapp_platform_version", "unknown" }
            };

            // 匹配Android
            var androidMatch = Regex.Match(ua, @"Android (\d+(?:\.\d+)?)");
            if (androidMatch.Success)
            {
                var version = androidMatch.Groups[1].Value;
                var parts = version.Split('.');
                if (parts.Length == 1) version += ".0.0";
                else if (parts.Length == 2) version += ".0";

                result["webapp_platform"] = "android";
                result["webapp_platform_version"] = version;
                return result;
            }

            // 匹配iPhone
            var iphoneMatch = Regex.Match(ua, @"iPhone OS (\d+_\d+(_\d+)?)");
            if (iphoneMatch.Success)
            {
                var version = iphoneMatch.Groups[1].Value.Replace('_', '.');
                var parts = version.Split('.');
                if (parts.Length == 1) version += ".0.0";
                else if (parts.Length == 2) version += ".0";

                result["webapp_platform"] = "iphone";
                result["webapp_platform_version"] = version;
                return result;
            }

            return result;
        }
        #endregion

        #region Cookie获取（替换为HttpClient实现）
        private Dictionary<string, string> GetCookiesSync()
        {
            var cookies = new Dictionary<string, string>();
            try
            {
                // 第一步：获取pre_reload_logid
                var firstParams = new Dictionary<string, string>
                {
                    { "search_entrance", "aweme_top_bar" },
                    { "keyword", _keyword },
                    { "is_no_width_reload", "0" }
                };

                var firstUrl = BuildQueryUrl(SearchApiBaseUrl, firstParams);
                var firstResponse = _httpClient.GetAsync(firstUrl).Result; // 同步调用（可后续改异步）

                if (!firstResponse.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取pre_reload_logid失败：状态码{firstResponse.StatusCode}");
                    return cookies;
                }

                var firstContent = firstResponse.Content.ReadAsStringAsync().Result;
                // 提取pre_reload_logid
                var preReloadLogidMatch = Regex.Match(firstContent, @"document.cookie='pre_reload_logid=(.*?)'");
                if (!preReloadLogidMatch.Success)
                {
                    FileUtils.log("未提取到pre_reload_logid");
                    return cookies;
                }
                var preReloadLogid = preReloadLogidMatch.Groups[1].Value;
                FileUtils.log($"获取pre_reload_logid: {preReloadLogid}");

                // 第二步：添加Cookie并获取search_webid和ttwid
                // 手动添加Cookie到容器
                _cookieContainer.Add(new Uri(SearchApiBaseUrl), new Cookie("douyin_search_screen", $"{_innerWidth}*{_innerHeight}", "/", ".douyin.com"));
                _cookieContainer.Add(new Uri(SearchApiBaseUrl), new Cookie("pre_reload_logid", preReloadLogid, "/", ".douyin.com"));

                var secondParams = new Dictionary<string, string>
                {
                    { "search_entrance", "aweme_top_bar" },
                    { "keyword", _keyword },
                    { "is_no_width_reload", "1" },
                    { "innerWidth", _innerWidth },
                    { "innerHeight", _innerHeight },
                    { "reloadNavStart", _reloadNavStart },
                    { "reload_from", "abnormal_wh_1" }
                };

                var secondUrl = BuildQueryUrl(SearchApiBaseUrl, secondParams);
                // 设置Referer
                var secondRequest = new HttpRequestMessage(HttpMethod.Get, secondUrl);
                secondRequest.Headers.Add("Referer", $"https://so.douyin.com/s?keyword={HttpUtility.UrlEncode(_keyword)}&innerWidth={_innerWidth}&innerHeight={_innerHeight}&reloadNavStart={_reloadNavStart}&is_no_width_reload=1&reload_from=abnormal_wh_1");
                var secondResponse = _httpClient.SendAsync(secondRequest).Result;
                if (!secondResponse.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取search_webid失败：状态码{secondResponse.StatusCode}");
                    return cookies;
                }

                var secondContent = secondResponse.Content.ReadAsStringAsync().Result;
                // 提取search_webid
                var searchWebidMatch = Regex.Match(secondContent, @"setSearchWebid\(""(\d+)""\);");
                var searchWebid = searchWebidMatch.Success ? searchWebidMatch.Groups[1].Value : "";
                FileUtils.log($"获取search_webid: {searchWebid}");

                // 提取ttwid（从CookieContainer）
                var ttwid = string.Empty;
                var cookieCollection = _cookieContainer.GetCookies(new Uri(SearchApiBaseUrl));
                foreach (Cookie cookie in cookieCollection)
                {
                    if (cookie.Name == "ttwid")
                    {
                        ttwid = cookie.Value;
                        break;
                    }
                }
                FileUtils.log($"返回ttwid: {ttwid}");

                if (string.IsNullOrEmpty(ttwid))
                {
                    FileUtils.log("未提取到ttwid");
                    return cookies;
                }

                // 构造核心Cookie
                cookies = new Dictionary<string, string>
                {
                    { "douyin_search_screen", $"{_innerWidth}*{_innerHeight}" },
                    { "pre_reload_logid", preReloadLogid },
                    { "ttwid", ttwid },
                    { "s_recover_ttwid", ttwid },
                    { "search_webid", searchWebid }
                };

                return cookies;
            }
            catch (Exception ex)
            {
                FileUtils.log($"获取Cookie失败：{ex.Message}");
                return cookies;
            }
        }
        #endregion

        #region 核心搜索方法（HttpClient异步实现）
        /// <summary>
        /// 发送搜索请求（异步）
        /// </summary>
        /// <param name="searchId">搜索ID</param>
        /// <param name="currentPage">当前页</param>
        /// <returns>响应内容</returns>
        private async Task<string> DouyinSearchRequestAsync(string searchId, int currentPage)
        {
            try
            {
                var uaRes = ParseUA(_ua);
                var parameters = new Dictionary<string, string>
                {
                    { "aid", "581610" },
                    { "device_platform", "webapp" },
                    { "search_source", "normal_search" },
                    { "app_version", "290000" },
                    { "version_code", "290000" },
                    { "version_name", "29.0.0" },
                    { "webapp_platform", uaRes["webapp_platform"] },
                    { "webapp_platform_version", uaRes["webapp_platform_version"] },
                    { "search_entrance", "aweme_top_bar" },
                    { "keyword", _keyword },
                    { "is_no_width_reload", "0" },
                    { "innerWidth", _innerWidth },
                    { "innerHeight", _innerHeight },
                    { "reloadNavStart", _reloadNavStart },
                    { "reload_from", "abnormal_wh_1" },
                    { "app_theme", "light" },
                    { "pd", SearchPd },
                    { "count", _count.ToString() },
                    { "sort_params", "" },
                    { "appTheme", "light" },
                    { "need_filter_settings", "1" },
                    { "location_access", "" },
                    { "offset", ((currentPage - 1) * _count).ToString() },
                    { "search_id", searchId },
                    { "currentPage", currentPage.ToString() },
                    { "backtrace", "" },
                    { "options[stream]", "false" }
                };

                var requestUrl = BuildQueryUrl(SearchDataApiUrl, parameters);
                var request = new HttpRequestMessage(HttpMethod.Get, requestUrl);
                request.Headers.Add("Referer", SearchApiBaseUrl);

                // 发送请求
                var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode(); // 抛出非200状态码异常
                return await response.Content.ReadAsStringAsync();
            }
            catch (TaskCanceledException)
            {
                FileUtils.log("请求超时");
                return null;
            }
            catch (HttpRequestException ex)
            {
                FileUtils.log($"请求异常：{ex.Message}");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.log($"搜索请求失败：{ex.Message}");
                return null;
            }
        }
        #endregion

        #region 响应处理（优化解析逻辑）
        private JObject SearchHandler(string responseContent)
        {
            if (string.IsNullOrEmpty(responseContent) || responseContent.Length < 10)
            {
                FileUtils.log("返回数据为空,没有数据");
                return null;
            }

            // 解析JSON（容错增强）
            JObject jsonData;
            try
            {
                jsonData = JObject.Parse(responseContent);
            }
            catch (Exception ex)
            {
                FileUtils.log($"JSON解析失败: {ex.Message} | 响应内容：{responseContent.Substring(0, Math.Min(200, responseContent.Length))}");
                return null;
            }

            // 检查业务状态
            if (jsonData["status_code"]?.Value<int>() == 0 && jsonData["business_data"] != null)
            {
                return jsonData;
            }
            else
            {
                FileUtils.log($"业务返回异常：{jsonData["status_code"]} | {jsonData["message"] ?? "无错误信息"}");
                return null;
            }
        }
        #endregion

        #region 主执行方法（异步+恢复解析逻辑）
        /// <summary>
        /// 异步开始搜索视频（核心业务逻辑）
        /// </summary>
        /// <returns>视频列表</returns>
        public async Task<List<DouYinVideoInfo>> StartSearchVideoAsync()
        {
            FileUtils.log($"【开始搜索】关键词：{_keyword} | UA：{_ua}");
            FileUtils.log($"【Cookie信息】：{JsonConvert.SerializeObject(_cookies, Formatting.Indented)}");

            if (_useProxy && _currentProxyIp != null)
            {
                FileUtils.log($"【代理信息】：{_currentProxyIp.ip}:{_currentProxyIp.port}");
            }

            var videoList = new List<DouYinVideoInfo>();
            var awemeIdSet = new HashSet<string>(); // 去重
            int currentPage = 1;
            string searchId = "";
            int requestsCount = 0;
            bool hasMore = true;

            // 分页搜索（增加最大页数限制，避免死循环）
            while (hasMore && currentPage <= MaxRequestPages && videoList.Count < _maxCount)
            {
                FileUtils.log($"【分页爬取】第{currentPage}页 | 已获取{videoList.Count}条 | 最大{_maxCount}条");
                await Task.Delay(_random.Next(1500, 3000)); // 异步休眠，不阻塞线程

                // 发送搜索请求
                var responseContent = await DouyinSearchRequestAsync(searchId, currentPage);
                var jsonData = SearchHandler(responseContent);

                // 无数据则终止
                if (jsonData == null)
                {
                    FileUtils.log("【终止】无有效数据返回");
                    break;
                }

                requestsCount++;
                FileUtils.log($"【请求统计】当前时间：{GetCurrentTime()} | 请求次数：{requestsCount}");

                // 第一页提取search_id
                if (currentPage == 1)
                {
                    searchId = jsonData["extra"]?["logid"]?.Value<string>() ?? "";
                    FileUtils.log($"【SearchId】：{searchId}");
                }

                // 解析是否有更多数据
                hasMore = jsonData["business_config"]?["has_more"]?.Value<int>() == 1;

                // 解析视频数据（恢复原注释的核心逻辑）
                var businessData = jsonData["business_data"];
                if (businessData == null) continue;

                foreach (var item in businessData.Children())
                {
                    var awemeInfo = item["data"]?["aweme_info"] ?? new JObject();
                    if (awemeInfo.Type == JTokenType.Null) continue;

                    // 过滤非视频（有动态封面才是视频）
                    var dynamicCover = awemeInfo["video"]?["dynamic_cover"]?["url_list"]?[0]?.Value<string>();
                    if (dynamicCover == null) continue;

                    var awemeId = awemeInfo["aweme_id"]?.Value<string>();
                    // 去重
                    if (string.IsNullOrEmpty(awemeId) || awemeIdSet.Contains(awemeId)) continue;
                    awemeIdSet.Add(awemeId);

                    // 解析视频时长（兼容毫秒/秒）
                    long? videoDuration = awemeInfo["video"]?["duration"]?.Value<long?>();
                    //long videoDuration = JMESPath.Search<long?>(awemeInfo, "video.duration");
                    if (videoDuration.HasValue && videoDuration > 1000)
                    {
                        videoDuration /= 1000;
                    }
                    // 检查是否有视频封面
                    var coverUrl = awemeInfo["video"]?["dynamic_cover"]?["url_list"]?[0]?.Value<string>();// JMESPath.Search<string>(awemeInfo, "video.cover.url_list[0]");
                    //var awemeId = JMESPath.Search<string>(awemeInfo, "aweme_id");
                    var mytitle = awemeInfo["desc"]?.Value<string>();
                    var filehash = awemeInfo["video"]?["play_addr"]?["file_hash"]?.Value<string>();// JMESPath.Search<string>(awemeInfo, "video.play_addr.file_hash");
                    if (string.IsNullOrEmpty(coverUrl) || string.IsNullOrEmpty(awemeId) || string.IsNullOrEmpty(mytitle) || string.IsNullOrEmpty(filehash))
                        continue;
                    // 构造视频信息对象
                    var videoInfo = new DouYinVideoInfo
                    {
                        aweme_id = awemeId,
                        desc = SafeTruncate(awemeInfo["desc"]?.Value<string>(),500),
                        create_time =long.Parse(awemeInfo["create_time"]?.Value<string>()),
                        sec_uid = awemeInfo["author"]?["sec_uid"]?.Value<string>(),
                        // 解析作者信息
                        author = new DouYinAuthorInfo
                        {
                            uid = awemeInfo["author"]?["uid"]?.Value<string>(),
                            nickname = awemeInfo["author"]?["nickname"]?.Value<string>(),
                            //short_id = awemeInfo["author"]?["short_id"]?.Value<string>(),
                            //unique_id = awemeInfo["author"]?["unique_id"]?.Value<string>(),
                            //signature = awemeInfo["author"]?["signature"]?.Value<string>(),
                            sec_uid =Convert.ToString(((Newtonsoft.Json.Linq.JValue)(awemeInfo["author"]?["sec_uid"])).Value), 
                            avatar_thumb = new avatarThumb
                            {
                                url_list = new List<string>
                                                {
                                                    awemeInfo["author"]?["avatar_thumb"]?["url_list"]?[0]?.Value<string>()
                                                }
                            },
                            //aweme_count = awemeInfo["author"]?["aweme_count"]?.Value<long?>(),
                            //following_count = awemeInfo["author"]?["following_count"]?.Value<long?>(),
                            follower_count = awemeInfo["author"]?["follower_count"]?.Value<long?>(),
                            //favoriting_count = awemeInfo["author"]?["favoriting_count"]?.Value<long?>(),
                            //total_favorited = awemeInfo["author"]?["total_favorited"]?.Value<long?>()
                        },
                        //// 解析音乐信息
                        //music = new MusicInfo
                        //{
                        //    title = awemeInfo["music"]?["title"]?.Value<string>(),
                        //    play_url = awemeInfo["music"]?["play_url"]?["uri"]?.Value<string>(),
                        //    duration = awemeInfo["music"]?["duration"]?.Value<long?>()
                        //},
                        // 解析视频信息
                        video = new DouYinVideoDetail
                        {
                            duration = videoDuration?? 0,
                            cover = new cover
                            {
                                url_list = new List<string>
                                                {
                                                    awemeInfo["video"]?["dynamic_cover"]?["url_list"]?[0]?.Value<string>()
                                                }
                            },
                            play_addr = new playAddr
                            {
                                file_hash = awemeInfo["video"]?["play_addr"]?["file_hash"]?.Value<string>()
                            }
                            
                        },
                        // 解析统计信息
                        statistics = new DouYinStatistics
                        {
                            digg_count = awemeInfo["statistics"]?["digg_count"]?.Value<long?>(),
                            comment_count = awemeInfo["statistics"]?["comment_count"]?.Value<long?>(),
                            share_count = awemeInfo["statistics"]?["share_count"]?.Value<long?>(),
                            collect_count = awemeInfo["statistics"]?["collect_count"]?.Value<long?>()
                            //play_count = awemeInfo["statistics"]?["play_count"]?.Value<long?>()
                        },
                        // 解析业务配置
                        //business_config = new BusinessConfig
                        //{
                        //    keyword = jsonData["business_config"]?["keyword"]?.Value<string>(),
                        //    has_more = jsonData["business_config"]?["has_more"]?.Value<int>(),
                        //    is_filter_search = jsonData["business_config"]?["is_filter_search"]?.Value<int>(),
                        //    search_nil_info = jsonData["business_config"]?["search_nil_info"]?.Value<JObject>(),
                        //    next_page = jsonData["business_config"]?["next_page"]?.Value<int>(),
                        //    card_count = jsonData["business_config"]?["card_count"]?.Value<int>()
                        //}
                    };

                    //// 区分图文/视频
                    //var images = item["images"]?.Children().ToList();
                    //if (images != null && images.Count > 0)
                    //{
                    //    videoInfo.images = images.Select(img => img["url_list"]?[0]?.Value<string>()).Where(u => !string.IsNullOrEmpty(u)).ToList();
                    //    videoInfo.aweme_type = "note";
                    //    videoInfo.aweme_url = $"https://www.douyin.com/note/{awemeId}";
                    //}
                    //else
                    //{
                    //    videoInfo.aweme_type = "video";
                    //    videoInfo.aweme_url = $"https://www.douyin.com/video/{awemeId}";
                    //}

                    videoList.Add(videoInfo);
                    FileUtils.log($"【解析成功】视频ID：{awemeId} | 标题：{videoInfo.desc?.Substring(0, Math.Min(50, videoInfo.desc.Length))}");

                    // 达到最大数量则终止
                    if (videoList.Count >= _maxCount)
                    {
                        FileUtils.log($"【终止】已达到最大爬取数：{_maxCount}条");
                        hasMore = false;
                        break;
                    }
                }

                currentPage++;
            }

            // 爬取完成统计
            FileUtils.log("======== 爬取完成 =========");
            FileUtils.log($"总页数：{currentPage - 1} | 总作品数：{videoList.Count}");
            if (videoList.Count > 0)
            {
                //var diggCountList = videoList.Select(v => v.statistics.digg_count).ToList();
                //FileUtils.log($"点赞数TOP5：{string.Join(", ", diggCountList.OrderByDescending(x => x).Take(5))}");
            }

            return videoList;
        }
        #endregion

        #region 代理和工具方法（优化）
        /// <summary>
        /// 构建带查询参数的URL
        /// </summary>
        /// <param name="baseUrl">基础URL</param>
        /// <param name="parameters">查询参数</param>
        /// <returns>拼接后的URL</returns>
        private string BuildQueryUrl(string baseUrl, Dictionary<string, string> parameters)
        {
            var queryBuilder = new StringBuilder(baseUrl);
            queryBuilder.Append("?");
            foreach (var param in parameters)
            {
                if (string.IsNullOrEmpty(param.Value)) continue;
                queryBuilder.Append($"{HttpUtility.UrlEncode(param.Key)}={HttpUtility.UrlEncode(param.Value)}&");
            }
            return queryBuilder.ToString().TrimEnd('&');
        }

        /// <summary>
        /// 检查代理是否过期
        /// </summary>
        private void CheckProxyExpire()
        {
            lock (_proxyIpLock)
            {
                if (!_isUseProxy) return;

                long currentTime = ServerTimeUtils.getCurrentTime();
                if (currentTime >= _proxyExpireTime)
                {
                    FileUtils.log($"代理IP已过期，当前时间：{currentTime}，过期时间：{_proxyExpireTime}");
                    _isUseProxy = false;
                    _currentProxyIp = null;
               }
            }
        }
        
        /// <summary>
        /// 使用代理IP（保留原逻辑）
        /// </summary>
        private void UseProxyIP()
        {
            lock (_proxyIpLock)
            {
                if (_isUseProxy) return;

                // 替换为你的代理获取逻辑
                ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(false, 0);
                if (proxyIpVo != null)
                {
                    _isUseProxy = true;
                    _currentProxyIp = proxyIpVo;
                    _proxyExpireTime = proxyIpVo.expireTime;
                    FileUtils.log($"代理IP设置成功：{proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}");
                }
                else
                {
                    FileUtils.log("获取代理IP失败");
                }
            }
        }

        /// <summary>
        /// 释放资源
        /// </summary>
        public void Dispose()
        {
            _httpClient?.Dispose();
        }
        #endregion

        public string SafeTruncate(string input, int maxLength)
        {
            if (string.IsNullOrEmpty(input))
                return input;

            // 处理可能的代理对字符（如某些emoji）
            int actualLength = 0;
            int i = 0;
            for (; i < input.Length && actualLength < maxLength; i++)
            {
                actualLength += char.IsHighSurrogate(input[i]) ? 2 : 1;
            }

            return input.Substring(0, i);
        }
    }

    
}
