using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Web;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.ShortVideo;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;

/// <summary>
/// 抖音视频搜索
/// </summary>
public class SearchVideo
{

    #region 私有属性
    private readonly string _keyword;
    private bool useProxy;
    private HttpClient _httpClient;
    private HttpClientHandler handler;
    private static readonly string Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-./";
    private CookieContainer _cookieContainer = new CookieContainer();
    private string _xWebSecsdkUid = Guid.NewGuid().ToString();
    private string _csrfSessionId = GenerateRandomHexString(16);
    private Random rand = new Random();
    private int[] nextTime;
    private ProxyIpVo proxyIpVo;
    private int proxyCount;
    private string version = "132.0.0.0";
    #endregion 私有属性

    #region 构造方法


    /// <summary>
    /// 构造方法
    /// </summary>
    /// <param name="keyword">关键词</param>
    /// <param name="nextTime">每次搜索间隔的时间 大小2，0是开始时间，1是结束时间，单位：毫秒</param>
    public SearchVideo(string keyword, int[] nextTime = null, bool useProxy = false)
    {
        this._keyword = keyword;
        this.useProxy = useProxy;
        this.nextTime = nextTime ?? new[] { 3000, 7001 };
        createHttpClient(false);
    }

    private void createHttpClient(bool usePro, bool forceUpdate = false)
    {
        // 清理旧实例
        _httpClient?.Dispose();
        handler?.Dispose();

        handler = new HttpClientHandler
        {
            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
            UseCookies = true,
            CookieContainer = _cookieContainer,
            AllowAutoRedirect = true,
            UseDefaultCredentials = false,
            Proxy = null,
            UseProxy = true
        };

        this.useProxy = false;
        if (usePro)
        {
            // 从服务器获取新的代理IP
            this.proxyIpVo = ShortVideoProxy.getShortActingProxy(forceUpdate);

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

                this.useProxy = true;
            }
        }

        _httpClient = new HttpClient(handler);
        _httpClient.Timeout = TimeSpan.FromSeconds(30);
        InitializeDefaultHeaders();
    }

    #endregion

    #region 私有方法

    /// <summary>
    /// 设置请求头
    /// </summary>
    private void InitializeDefaultHeaders()
    {
        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("User-Agent", $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version} Safari/537.36 SE 2.X MetaSr 1.0");
        _httpClient.DefaultRequestHeaders.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        _httpClient.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-site", "none");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-mode", "navigate");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-user", "?1");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-dest", "document");
        _httpClient.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"");
        _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
        _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
        _httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
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

    private string GetAcSignature(string ua, string acNonce)
    {
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

    private Dictionary<string, string> GetCookies()
    {
        var cookies = new Dictionary<string, string>();
        var encodedKeyword = HttpUtility.UrlEncode(_keyword);
        var url = $"https://www.douyin.com/search/{encodedKeyword}?type=video";

        try
        {
            // 第一次请求 - 获取初始Cookie，包括__ac_nonce
            FileUtils.log("第一次请求获取初始Cookie...");
            var firstResponse = _httpClient.GetAsync(url).Result;
            firstResponse.EnsureSuccessStatusCode();

            // 从CookieContainer中提取所有Cookie
            ExtractCookiesFromContainer(new Uri(url), cookies);

            if (!cookies.ContainsKey("__ac_nonce"))
            {
                FileUtils.log("警告: 未获取到__ac_nonce Cookie，尝试从响应头提取");
                ExtractCookiesFromHeaders(firstResponse.Headers, cookies);
            }

            // 如果仍然没有__ac_nonce，重试一次
            if (!cookies.ContainsKey("__ac_nonce"))
            {
                FileUtils.log("__ac_nonce获取失败，重试一次...");
                Thread.Sleep(1000);
                var retryResponse = _httpClient.GetAsync(url).Result;
                ExtractCookiesFromContainer(new Uri(url), cookies);
                ExtractCookiesFromHeaders(retryResponse.Headers, cookies);
            }

            if (!cookies.TryGetValue("__ac_nonce", out var acNonce))
            {
                FileUtils.log("错误: 无法获取__ac_nonce Cookie");
                return cookies;
            }

            // 生成__ac_signature
            var ua = _httpClient.DefaultRequestHeaders.UserAgent.ToString();
            var acSignature = GetAcSignature(ua, acNonce);
            cookies["__ac_signature"] = acSignature;
            cookies["__ac_referer"] = "__ac_blank";

            // 添加生成的签名到CookieContainer
            _cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_signature", acSignature));
            _cookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("__ac_referer", "__ac_blank"));

            // 第二次请求 - 尝试获取ttwid
            FileUtils.log("第二次请求尝试获取ttwid...");
            _httpClient.DefaultRequestHeaders.Referrer = new Uri(url);
            var secondResponse = _httpClient.GetAsync(url).Result;
            secondResponse.EnsureSuccessStatusCode();

            // 再次从容器中提取所有Cookie
            ExtractCookiesFromContainer(new Uri(url), cookies);

            // 检查是否获取到ttwid，未获取到则尝试第三次请求
            if (!cookies.ContainsKey("ttwid"))
            {
                FileUtils.log("警告: 未获取到ttwid Cookie，尝试第三次请求...");
                Thread.Sleep(1500);
                var thirdResponse = _httpClient.GetAsync(url).Result;
                ExtractCookiesFromContainer(new Uri(url), cookies);
            }

            // 添加额外需要的Cookie
            cookies["x-web-secsdk-uid"] = _xWebSecsdkUid;
            cookies["csrf_session_id"] = _csrfSessionId;
            cookies["SEARCH_RESULT_LIST_TYPE"] = "%22single%22";
            cookies["home_can_add_dy_2_desktop"] = "%220%22";
            cookies["hevc_supported"] = "true";

            // 输出最终获取的Cookie信息
            FileUtils.log("\n初始化cookies信息:");
            foreach (var cookie in cookies)
            {
                FileUtils.log($"=================={cookie.Key}={cookie.Value}==================");
            }
        }
        catch (Exception ex)
        {
            FileUtils.log($"获取Cookie时出错: {ex.Message}");
        }

        return cookies;
    }

    private void ExtractCookiesFromContainer(Uri uri, Dictionary<string, string> cookies)
    {
        var containerCookies = _cookieContainer.GetCookies(uri);
        foreach (Cookie cookie in containerCookies)
        {
            if (!cookies.ContainsKey(cookie.Name))
            {
                cookies[cookie.Name] = cookie.Value;
            }
        }
    }

    private void ExtractCookiesFromHeaders(HttpResponseHeaders headers, Dictionary<string, string> cookies)
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

    private string ConvertTimestamp(long? timestamp)
    {
        if (timestamp.HasValue)
        {
            long timestampValue = timestamp.Value;
            // 处理毫秒级时间戳
            if (timestampValue.ToString().Length == 13)
            {
                timestampValue /= 1000;
            }
            return DateTimeOffset.FromUnixTimeSeconds(timestampValue)
                .LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss");
        }
        return null;
    }

    private string GetCurrentTime()
    {
        return DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
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

    #endregion 私有方法

    #region public方法

    /// <summary>
    /// 调用抖音搜索视频接口
    /// </summary>
    /// <param name="videoCount"></param>
    /// <param name="maxCount"></param>
    /// <returns></returns>
    public List<DouYinVideoInfo> SearchItem(int videoCount, int maxCount = 300)
    {
        var currentNow = DateTime.Now;
        FileUtils.log($"开始搜爆款 videoCount = {videoCount}", "搜爆款-开始");
        List<DouYinVideoInfo> items = new List<DouYinVideoInfo>();
        var headers = new Dictionary<string, string>
        {
            {"accept", "application/json, text/plain, */*"},
            {"accept-language", "zh-CN,zh;q=0.9"},
            {"cache-control", "no-cache"},
            {"pragma", "no-cache"},
            {"sec-ch-ua", "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"sec-fetch-dest", "empty"},
            {"sec-fetch-mode", "cors"},
            {"sec-fetch-site", "same-origin"},
            {"uifid", "undefined"},
            {"user-agent", $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version} Safari/537.36 SE 2.X MetaSr 1.0"}
        };

        var cookies = GetCookies();
        if (cookies == null || cookies.Count == 0)
        {
            FileUtils.log($"获取Cookies失败，无法继续, videoCount = {videoCount}", "获取Cookies失败");
            return items;
        }

        var encodedKeyword = HttpUtility.UrlEncode(_keyword);
        headers["referer"] = $"https://www.douyin.com/search/{encodedKeyword}?aid={Guid.NewGuid().ToString()}type=video";

        var apiUrl = "https://www.douyin.com/aweme/v1/web/search/item/";
        var currentPage = 1;
        string searchId = null;
        var shouldExit = false;
        var requestsCount = 0;
        int count = 0;
        int countProxy = 0;
        int maxProxy = 10;

        while (!shouldExit && currentPage < 20)
        {
            FileUtils.log($"\n正在爬取第{currentPage}页......");

            var sortType = "0"; // 排序方式 0:综合排序, 1:最多点赞, 2:最新发布
            var publishTime = "180"; // 发布时间 0:不限, 1:一天内, 7:一周内, 180:半年内
            var filterDuration = ""; // 视频时长

            requestsCount++;
            FileUtils.log($"当前时间：{GetCurrentTime()} 当前请求次数: {requestsCount}");

            var parameters = new Dictionary<string, string>
            {
                {"device_platform", "webapp"},
                {"aid", "6383"},
                {"channel", "channel_pc_web"},
                {"search_channel", "aweme_video_web"},
                {"enable_history", "1"},
                {"keyword", _keyword},
                {"search_source", "normal_search"},
                {"query_correct_type", "1"},
                {"is_filter_search", "0"},
                {"from_group_id", ""},
                {"offset", ((currentPage - 1) * 25).ToString()},
                {"count", "25"},
                {"need_filter_settings", currentPage == 1 ? "1" : "0"},
                {"list_type", "single"},
                {"pc_search_top_1_params", "{\"enable_ai_search_top_1\":1}"},
                {"update_version_code", "170400"},
                {"pc_client_type", "1"},
                {"pc_libra_divert", "Windows"},
                {"support_h265", "1"},
                {"support_dash", "0"},
                {"cpu_core_num", "12"},
                {"version_code", "170400"},
                {"version_name", "17.4.0"},
                {"cookie_enabled", "true"},
                {"screen_width", "1920"},
                {"screen_height", "1080"},
                {"browser_language", "zh-CN"},
                {"browser_platform", "Win32"},
                {"browser_name", "Sogou Explorer"},
                {"browser_version", "1.0"},
                {"browser_online", "true"},
                {"engine_name", "Blink"},
                {"engine_version", version},
                {"os_name", "Windows"},
                {"os_version", "10"},
                {"device_memory", "8"},
                {"platform", "PC"},
                {"downlink", "10"},
                {"effective_type", "4g"},
                {"round_trip_time", "50"}
            };

            if (currentPage > 1 && !string.IsNullOrEmpty(searchId))
            {
                parameters["search_id"] = searchId;
            }

            // 设置筛选条件
            parameters["publish_time"] = publishTime;
            parameters["is_filter_search"] = sortType;

            // 构建查询字符串
            var queryString = string.Join("&", parameters.Select(kv => $"{kv.Key}={HttpUtility.UrlEncode(kv.Value)}"));
            var requestUrl = $"{apiUrl}?{queryString}";

            try
            {
                // 创建带有当前Cookie的请求
                var request = new HttpRequestMessage(HttpMethod.Get, requestUrl);
                foreach (var header in headers)
                {
                    request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                }

                // 发送请求并确保成功
                var response = _httpClient.SendAsync(request).Result;
                response.EnsureSuccessStatusCode();

                var responseContent = response.Content.ReadAsStringAsync().Result;
                if (responseContent.Length < 10)
                {
                    FileUtils.log($"出现风控 videoCount = {videoCount}, currentPage = {currentPage}, responseContent.Length < 10， responseContent = {responseContent}", "搜爆款-被风控");
                    shouldExit = true;
                    continue;
                }

                // 读取JSON内容并解析
                //using var jsonDoc = JsonDocument.Parse(responseContent);
                //var root = jsonDoc.RootElement;
                dynamic root = JsonConvert.DeserializeObject(responseContent); // 替换 JsonDocument.Parse 

                if (root.search_nil_info != null)
                {
                    FileUtils.log($"出现风控 {(currentPage == 1 ? "准备使用代理" : "不是第一页不使用代理")} videoCount = {videoCount}, currentPage = {currentPage}, root.search_nil_info != null， search_nil_info = {root.search_nil_info}", $"搜爆款-被风控-使用代理{countProxy}次数");
                    FileUtils.log($"出现风控 - {root?.search_nil_info?.search_nil_type ?? ""} - {(this.useProxy ? "使用" : "没有使用")}代理");
                    //if (currentPage == 1 && countProxy < maxProxy)
                    //{
                    //    createHttpClient(true, this.useProxy);
                    //    GetCookies();
                    //    if (!this.useProxy)
                    //    {
                    //        FileUtils.log("在爆款中获取代理ip失败，结束请求接口", "获取代理失败");
                    //        shouldExit = true;
                    //    }
                    //    countProxy++;
                    //}
                    //else
                    //{
                    //}
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
            int next = rand.Next(nextTime[0], nextTime[1]);
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
    /// 释放资源
    /// </summary>
    public void Dispose()
    {
        handler?.Dispose();
        _httpClient?.Dispose();
    }

    #endregion public方法

    #region 静态方法
    /// <summary>
    /// 调用抖音搜索视频接口，手动释放资源
    /// </summary>
    /// <param name="keyword"></param>
    /// <param name="videoNum"></param>
    /// <returns></returns>
    public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] next = null)
    {
        SearchVideo searchVideo = null;
        try
        {
            int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
            next = next ?? new int[] { 50, 100 };
            searchVideo = new SearchVideo(keyword, next);
            return searchVideo.SearchItem(videoNum, videoNum * 2);
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"Message = {ex.Message}, StackTrace = {ex.StackTrace}", "执行抖音搜索视频报错");
        }
        finally
        {
            searchVideo?.Dispose();
        }
        return new List<DouYinVideoInfo>();
    }

    #endregion 静态方法
}