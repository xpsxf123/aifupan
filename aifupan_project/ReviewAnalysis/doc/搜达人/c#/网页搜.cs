


using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web;





public class DouyinSpider
{
    private readonly string _keyword;
    private readonly HttpClient _httpClient;
    private static readonly string Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-./";
    private CookieContainer _cookieContainer = new CookieContainer();

    public DouyinSpider(string keyword)
    {
        _keyword = keyword;
        var handler = new HttpClientHandler
        {
            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
            UseCookies = true,
            CookieContainer = _cookieContainer,
            AllowAutoRedirect = true,
            UseDefaultCredentials = false
        };
        _httpClient = new HttpClient(handler);
        _httpClient.Timeout = TimeSpan.FromSeconds(30);
        InitializeDefaultHeaders();
    }

    // 创建 JsonSerializerOptions 配置
    public static class JsonSettings
    {
        public static readonly JsonSerializerOptions Default = new JsonSerializerOptions
        {
            Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping, // 禁用 Unicode 转义
            WriteIndented = true,
            AllowTrailingCommas = true,
            ReadCommentHandling = JsonCommentHandling.Skip
        };
    }


    private void InitializeDefaultHeaders()
    {
        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0");
        _httpClient.DefaultRequestHeaders.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        // _httpClient.DefaultRequestHeaders.Add("Accept-Encoding", "gzip, deflate, br, zstd");
        _httpClient.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-site", "none");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-mode", "navigate");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-user", "?1");
        _httpClient.DefaultRequestHeaders.Add("sec-fetch-dest", "document");
        _httpClient.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
        _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
        _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
        _httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
        _httpClient.DefaultRequestHeaders.Add("priority", "u=0, i");
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

    private async Task<Dictionary<string, string>> GetCookiesAsync()
    {
        var cookies = new Dictionary<string, string>();
        var encodedKeyword = HttpUtility.UrlEncode(_keyword);
        var url = $"https://www.douyin.com/search/{encodedKeyword}?type=general";

        try
        {
            // 第一次请求 - 获取初始Cookie，包括__ac_nonce
            Console.WriteLine("第一次请求获取初始Cookie...");
            var firstResponse = await _httpClient.GetAsync(url);
            firstResponse.EnsureSuccessStatusCode();

            // 从CookieContainer中提取所有Cookie
            ExtractCookiesFromContainer(new Uri(url), cookies);

            if (!cookies.ContainsKey("__ac_nonce"))
            {
                Console.WriteLine("警告: 未获取到__ac_nonce Cookie，尝试从响应头提取");
                ExtractCookiesFromHeaders(firstResponse.Headers, cookies);
            }

            // 如果仍然没有__ac_nonce，重试一次
            if (!cookies.ContainsKey("__ac_nonce"))
            {
                Console.WriteLine("__ac_nonce获取失败，重试一次...");
                await Task.Delay(1000);
                var retryResponse = await _httpClient.GetAsync(url);
                ExtractCookiesFromContainer(new Uri(url), cookies);
                ExtractCookiesFromHeaders(retryResponse.Headers, cookies);
            }

            if (!cookies.TryGetValue("__ac_nonce", out var acNonce))
            {
                Console.WriteLine("错误: 无法获取__ac_nonce Cookie");
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
            Console.WriteLine("第二次请求尝试获取ttwid...");
            _httpClient.DefaultRequestHeaders.Referrer = new Uri(url);
            var secondResponse = await _httpClient.GetAsync(url);
            secondResponse.EnsureSuccessStatusCode();

            // 再次从容器中提取所有Cookie
            ExtractCookiesFromContainer(new Uri(url), cookies);

            // 检查是否获取到ttwid，未获取到则尝试第三次请求
            if (!cookies.ContainsKey("ttwid"))
            {
                Console.WriteLine("警告: 未获取到ttwid Cookie，尝试第三次请求...");
                await Task.Delay(1500); // 增加延迟模拟人类行为
                var thirdResponse = await _httpClient.GetAsync(url);
                ExtractCookiesFromContainer(new Uri(url), cookies);
            }

            // 输出最终获取的Cookie信息
            Console.WriteLine("\n初始化cookies信息:");
            foreach (var cookie in cookies)
            {
                Console.WriteLine($"{cookie.Key}={cookie.Value}");
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"获取Cookie时出错: {ex.Message}");
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
                    var name = cookiePart[..equalPos];
                    var value = cookiePart[(equalPos + 1)..];
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
            return DateTimeOffset.FromUnixTimeSeconds(timestamp.Value)
                .LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss");
        }
        return null;
    }



    private Dictionary<string, object> ProcessAwemeItem(JsonElement item)
    {
        if (!item.TryGetProperty("aweme_info", out var awemeInfo))
            return null;

        var result = new Dictionary<string, object>();

        // 基础信息
        result["aweme_id"] = GetJsonString(awemeInfo, "aweme_id");
        result["desc"] = GetJsonString(awemeInfo, "desc");
        
        if (awemeInfo.TryGetProperty("create_time", out var createTime))
        {
            result["create_time"] = ConvertTimestamp(createTime.GetInt64());
        }

        // 作者信息
        result["author"] = ProcessAuthorInfo(awemeInfo);

        // 视频信息
        result["video"] = ProcessVideoInfo(awemeInfo);

        // 音乐信息
        result["music"] = ProcessMusicInfo(awemeInfo);

        // 统计信息
        result["statistics"] = ProcessStatistics(awemeInfo);

        // 判断作品类型
        if (item.TryGetProperty("images", out var images))
        {
            result["aweme_type"] = "note";
            result["aweme_url"] = $"https://www.douyin.com/note/{result["aweme_id"]}";
            result["images"] = ProcessImageList(images);
        }
        else
        {
            result["aweme_type"] = "video";
            result["aweme_url"] = $"https://www.douyin.com/video/{result["aweme_id"]}";
        }

        return result;
    }

    private Dictionary<string, object> ProcessAuthorInfo(JsonElement awemeInfo)
    {
        var author = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("author", out var authorProp))
            return author;

        author["uid"] = GetJsonString(authorProp, "uid");
        author["nickname"] = GetJsonString(authorProp, "nickname");
        author["short_id"] = GetJsonString(authorProp, "short_id");
        author["unique_id"] = GetJsonString(authorProp, "unique_id");
        author["signature"] = GetJsonString(authorProp, "signature");
        author["sec_uid"] = GetJsonString(authorProp, "sec_uid");

        if (authorProp.TryGetProperty("avatar_thumb", out var avatar) && 
            avatar.TryGetProperty("url_list", out var urlList))
        {
            author["avatar_thumb"] = urlList.EnumerateArray().FirstOrDefault().GetString();
        }

        author["aweme_count"] = GetJsonInt(authorProp, "aweme_count");
        author["following_count"] = GetJsonInt(authorProp, "following_count");
        author["follower_count"] = GetJsonInt(authorProp, "follower_count");
        author["favoriting_count"] = GetJsonInt(authorProp, "favoriting_count");
        author["total_favorited"] = GetJsonString(authorProp, "total_favorited");

        return author;
    }

    private Dictionary<string, object> ProcessVideoInfo(JsonElement awemeInfo)
    {
        var video = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("video", out var videoProp))
            return video;

        // 时长
        if (videoProp.TryGetProperty("duration", out var duration))
        {
            var dur = duration.GetInt64();
            video["duration"] = dur > 1000 ? dur / 1000 : dur;
        }

        // 播放地址
        if (videoProp.TryGetProperty("play_addr", out var playAddr) &&
            playAddr.TryGetProperty("url_list", out var urlList))
        {
            foreach (var url in urlList.EnumerateArray())
            {
                var urlStr = url.GetString();
                if (urlStr?.Contains("v3-web.douyinvod.com") == true)
                {
                    video["play_url"] = urlStr;
                    break;
                }
            }
        }

        // 动态封面
        if (videoProp.TryGetProperty("dynamic_cover", out var cover) &&
            cover.TryGetProperty("url_list", out var coverUrls))
        {
            video["dynamic_cover"] = coverUrls.EnumerateArray().FirstOrDefault().GetString();
        }

        return video;
    }

    private Dictionary<string, object> ProcessMusicInfo(JsonElement awemeInfo)
    {
        var music = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("music", out var musicProp))
            return music;

        music["title"] = GetJsonString(musicProp, "title");
        music["duration"] = GetJsonInt(musicProp, "duration");

        if (musicProp.TryGetProperty("play_url", out var playUrl) && 
            playUrl.TryGetProperty("uri", out var uri))
        {
            music["play_url"] = uri.GetString();
        }

        return music;
    }

    private Dictionary<string, object> ProcessStatistics(JsonElement awemeInfo)
    {
        var stats = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("statistics", out var statsProp))
            return stats;

        stats["digg_count"] = GetJsonInt(statsProp, "digg_count");
        stats["comment_count"] = GetJsonInt(statsProp, "comment_count");
        stats["share_count"] = GetJsonInt(statsProp, "share_count");
        stats["collect_count"] = GetJsonInt(statsProp, "collect_count");

        return stats;
    }

    private List<string> ProcessImageList(JsonElement images)
    {
        var imageList = new List<string>();
        foreach (var img in images.EnumerateArray())
        {
            if (img.TryGetProperty("url_list", out var urlList))
            {
                var url = urlList.EnumerateArray().FirstOrDefault().GetString();
                if (url != null)
                {
                    imageList.Add(url);
                }
            }
        }
        return imageList;
    }

    private string GetJsonString(JsonElement element, string propertyName)
    {
        if (element.TryGetProperty(propertyName, out var prop) && prop.ValueKind == JsonValueKind.String)
        {
            return prop.GetString();
        }
        return null;
    }

    private int GetJsonInt(JsonElement element, string propertyName, int defaultValue = 0)
    {
        if (element.TryGetProperty(propertyName, out var prop) && prop.ValueKind == JsonValueKind.Number)
        {
            return prop.GetInt32();
        }
        return defaultValue;
    }



    public async Task SearchSingleAsync(int maxCount = 100)
    {
        var headers = new Dictionary<string, string>
        {
            {"accept", "application/json, text/plain, */*"},
            {"accept-language", "zh-CN,zh;q=0.9"},
            {"cache-control", "no-cache"},
            {"pragma", "no-cache"},
            {"priority", "u=1, i"},
            {"sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"sec-fetch-dest", "empty"},
            {"sec-fetch-mode", "cors"},
            {"sec-fetch-site", "same-origin"},
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0"}
        };

        var cookies = await GetCookiesAsync();
        if (cookies == null || cookies.Count == 0)
        {
            Console.WriteLine("获取Cookies失败，无法继续");
            return;
        }

        var encodedKeyword = HttpUtility.UrlEncode(_keyword);
        headers["referer"] = $"https://www.douyin.com/search/{encodedKeyword}?type=general";

        var apiUrl = "https://www.douyin.com/aweme/v1/web/general/search/single/";
        var awemeIdList = new List<string>();
        var items = new List<Dictionary<string, object>>();
        var currentPage = 1;
        string searchId = null;
        var shouldExit = false;

        while (!shouldExit)
        {
            Console.WriteLine($"\n正在爬取第{currentPage}页......");

            var parameters = new Dictionary<string, string>
            {
                {"device_platform", "webapp"},
                {"aid", "6383"},
                {"channel", "channel_pc_web"},
                {"search_channel", "aweme_general"},
                {"enable_history", "1"},
                {"filter_selected", "{\"sort_type\":\"1\",\"publish_time\":\"0\"}"},
                {"keyword", _keyword},
                {"search_source", "tab_search"},
                {"query_correct_type", "1"},
                {"is_filter_search", "1"},
                {"from_group_id", ""},
                {"offset", ((currentPage - 1) * 10).ToString()},
                {"count", "10"},
                {"need_filter_settings", currentPage == 1 ? "1" : "0"},
                {"list_type", "single"},
                {"update_version_code", "170400"},
                {"pc_client_type", "1"},
                {"pc_libra_divert", "Windows"},
                {"support_h265", "1"},
                {"support_dash", "1"},
                {"cpu_core_num", "12"},
                {"version_code", "190600"},
                {"version_name", "19.6.0"},
                {"cookie_enabled", "true"},
                {"screen_width", "1920"},
                {"screen_height", "1080"},
                {"browser_language", "zh-CN"},
                {"browser_platform", "Win32"},
                {"browser_name", "Edge"},
                {"browser_version", "138.0.0.0"},
                {"browser_online", "true"},
                {"engine_name", "Blink"},
                {"engine_version", "138.0.0.0"},
                {"os_name", "Windows"},
                {"os_version", "10"},
                {"device_memory", "8"},
                {"platform", "PC"},
                {"downlink", "10"},
                {"effective_type", "4g"},
                {"round_trip_time", "0"}
            };

            if (currentPage > 1 && !string.IsNullOrEmpty(searchId))
            {
                parameters["search_id"] = searchId;
            }


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
                var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();

                var responseContent = await response.Content.ReadAsStringAsync();
                // Console.WriteLine("响应内容:");
                // Console.WriteLine(responseContent);


                // 读取JSON内容并解析
                using var jsonDoc = JsonDocument.Parse(responseContent);
                var root = jsonDoc.RootElement;

                if (root.TryGetProperty("status_code", out var statusCode) && statusCode.GetInt32() == 0)
                {
                    // 处理search_id
                    if (currentPage == 1 && root.TryGetProperty("extra", out var extra) &&
                        extra.TryGetProperty("logid", out var logid))
                    {
                        searchId = logid.ValueKind switch
                        {
                            JsonValueKind.String => logid.GetString(),
                            JsonValueKind.Number => logid.GetInt32().ToString(),
                            _ => null
                        };
                        Console.WriteLine($"search_id: {searchId}");
                    }

                    // 处理has_more
                    bool hasMore = false;
                    if (root.TryGetProperty("has_more", out var hasMoreProp))
                    {
                        hasMore = hasMoreProp.ValueKind switch
                        {
                            JsonValueKind.True => true,
                            JsonValueKind.False => false,
                            JsonValueKind.Number => hasMoreProp.GetInt32() == 1,
                            _ => false
                        };
                    }
                    Console.WriteLine(hasMore ? "还有下一页" : "没有下一页");

                    // 处理数据
                    if (root.TryGetProperty("data", out var data) && data.ValueKind == JsonValueKind.Array)
                    {
                        foreach (var item in data.EnumerateArray())
                        {
                            var resultItem = ProcessAwemeItem(item);
                            if (resultItem != null && resultItem.TryGetValue("aweme_id", out var awemeId))
                            {
                                awemeIdList.Add(awemeId.ToString());
                                items.Add(resultItem);

                                Console.WriteLine(JsonSerializer.Serialize(resultItem, JsonSettings.Default));

                                if (items.Count >= maxCount)
                                {
                                    shouldExit = true;
                                    break;
                                }
                            }
                        }

                        if (!hasMore)
                        {
                            shouldExit = true;
                        }
                    }
                }
                else
                {
                    Console.WriteLine("API返回状态码异常或数据无效");
                    shouldExit = true;
                }


            }
            catch (Exception ex)
            {
                Console.WriteLine($"请求错误: {ex.Message}");
                shouldExit = true;
            }

            currentPage++;
            // 添加延迟，避免请求过于频繁
            await Task.Delay(2000);
        }

        Console.WriteLine("\n======== 爬取完成 ==========");
        Console.WriteLine($"共爬取{currentPage - 1}页，总计{items.Count}个作品");
        Console.WriteLine($"去重前: {awemeIdList.Count}, 去重后: {awemeIdList.Distinct().Count()}");
    }
}

class Program
{
    static async Task Main(string[] args)
    {
        Console.WriteLine("抖音搜索爬虫启动...");
        // Console.Write("请输入搜索关键词: ");
        // var keyword = Console.ReadLine() ?? "旺仔小乔";
        
        var spider = new DouyinSpider("旺仔小乔");
        await spider.SearchSingleAsync(100);
        
        // Console.WriteLine("\n程序执行完毕，按任意键退出...");
        // Console.ReadKey();
    }
}
    