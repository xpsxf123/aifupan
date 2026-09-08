

using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json;
using System.Web;
using System.Linq;
using System.Net;

public class DouyinSpider
{
    private readonly Dictionary<string, BrowserEnvironment> browserEnvironments;
    private readonly string sessionId = "54df09a2058775270824fe8a08804c4f";
    private readonly string keyword = "honorofkings"; // 王者荣耀抖音号
    // private readonly string keyword = "王者荣耀";

    public DouyinSpider()
    {
        // 初始化4个浏览器环境
        browserEnvironments = new Dictionary<string, BrowserEnvironment>
        {
            {
                "sougou", new BrowserEnvironment
                {
                    Headers = new Dictionary<string, string>
                    {
                        ["authority"] = "www.douyin.com",
                        ["accept"] = "application/json, text/plain, */*",
                        ["accept-language"] = "zh-CN,zh;q=0.9",
                        ["cache-control"] = "no-cache",
                        ["pragma"] = "no-cache",
                        ["referer"] = "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user",
                        ["sec-ch-ua"] = "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"",
                        ["sec-ch-ua-mobile"] = "?0",
                        ["sec-ch-ua-platform"] = "\"Windows\"",
                        ["sec-fetch-dest"] = "empty",
                        ["sec-fetch-mode"] = "cors",
                        ["sec-fetch-site"] = "same-origin",
                        ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36 SE 2.X MetaSr 1.0"
                    },
                    Params = new Dictionary<string, string>
                    {
                        ["update_version_code"] = "170400",
                        ["pc_client_type"] = "1",
                        ["pc_libra_divert"] = "Windows",
                        ["support_h265"] = "1",
                        ["support_dash"] = "1",
                        ["cpu_core_num"] = "12",
                        ["cookie_enabled"] = "true",
                        ["screen_width"] = "1920",
                        ["screen_height"] = "1080",
                        ["browser_language"] = "zh-CN",
                        ["browser_platform"] = "Win32",
                        ["browser_name"] = "Sogou Explorer",
                        ["browser_version"] = "1.0",
                        ["browser_online"] = "true",
                        ["engine_name"] = "Blink",
                        ["engine_version"] = "116.0.5845.97",
                        ["os_name"] = "Windows",
                        ["os_version"] = "10",
                        ["device_memory"] = "8",
                        ["platform"] = "PC",
                        ["downlink"] = "10",
                        ["effective_type"] = "4g",
                        ["round_trip_time"] = "0"
                    }
                }
            },
            {
                "firefox", new BrowserEnvironment
                {
                    Headers = new Dictionary<string, string>
                    {
                        { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/116.0" },
                        { "Accept", "application/json, text/plain, */*" },
                        { "Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2" },
                        { "uifid", "undefined" },
                        { "Connection", "keep-alive" },
                        { "Referer", "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user" },
                        { "Sec-Fetch-Dest", "empty" },
                        { "Sec-Fetch-Mode", "cors" },
                        { "Sec-Fetch-Site", "same-origin" },
                        { "Pragma", "no-cache" },
                        { "Cache-Control", "no-cache" }
                    },
                    Params = new Dictionary<string, string>
                    {
                        { "update_version_code", "170400" },
                        { "pc_client_type", "1" },
                        { "pc_libra_divert", "Windows" },
                        { "support_h265", "0" },
                        { "support_dash", "0" },
                        { "cpu_core_num", "8" },
                        { "cookie_enabled", "true" },
                        { "screen_width", "1536" },
                        { "screen_height", "864" },
                        { "browser_language", "zh-CN" },
                        { "browser_platform", "Win32" },
                        { "browser_name", "Firefox" },
                        { "browser_version", "116.0" },
                        { "browser_online", "true" },
                        { "engine_name", "Gecko" },
                        { "engine_version", "109.0" },
                        { "os_name", "Windows" },
                        { "os_version", "10" },
                        { "device_memory", "" },
                        { "platform", "PC" }
                    }
                }
            },
            {
                "chrome", new BrowserEnvironment
                {
                    Headers = new Dictionary<string, string>
                    {
                        { "accept", "application/json, text/plain, */*" },
                        { "accept-language", "zh-CN,zh;q=0.9" },
                        { "cache-control", "no-cache" },
                        { "pragma", "no-cache" },
                        { "priority", "u=1, i" },
                        { "referer", "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user" },
                        { "sec-ch-ua", "\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"" },
                        { "sec-ch-ua-mobile", "?0" },
                        { "sec-ch-ua-platform", "\"Windows\"" },
                        { "sec-fetch-dest", "empty" },
                        { "sec-fetch-mode", "cors" },
                        { "sec-fetch-site", "same-origin" },
                        { "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36" }
                    },
                    Params = new Dictionary<string, string>
                    {
                        { "update_version_code", "170400" },
                        { "pc_client_type", "1" },
                        { "pc_libra_divert", "Windows" },
                        { "support_h265", "1" },
                        { "support_dash", "1" },
                        { "cpu_core_num", "8" },
                        { "cookie_enabled", "true" },
                        { "screen_width", "1536" },
                        { "screen_height", "864" },
                        { "browser_language", "zh-CN" },
                        { "browser_platform", "Win32" },
                        { "browser_name", "Chrome" },
                        { "browser_version", "137.0.0.0" },
                        { "browser_online", "true" },
                        { "engine_name", "Blink" },
                        { "engine_version", "137.0.0.0" },
                        { "os_name", "Windows" },
                        { "os_version", "10" },
                        { "device_memory", "8" },
                        { "platform", "PC" }
                    }
                }
            },
            {
                "edge", new BrowserEnvironment
                {
                    Headers = new Dictionary<string, string>
                    {
                        { "accept", "application/json, text/plain, */*" },
                        { "accept-language", "zh-CN,zh;q=0.9" },
                        { "cache-control", "no-cache" },
                        { "pragma", "no-cache" },
                        { "priority", "u=1, i" },
                        { "referer", "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user" },
                        { "sec-ch-ua", "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"" },
                        { "sec-ch-ua-mobile", "?0" },
                        { "sec-ch-ua-platform", "\"Windows\"" },
                        { "sec-fetch-dest", "empty" },
                        { "sec-fetch-mode", "cors" },
                        { "sec-fetch-site", "same-origin" },
                        { "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0" }
                    },
                    Params = new Dictionary<string, string>
                    {
                        { "update_version_code", "170400" },
                        { "pc_client_type", "1" },
                        { "pc_libra_divert", "Windows" },
                        { "support_h265", "1" },
                        { "support_dash", "1" },
                        { "cpu_core_num", "12" },
                        { "cookie_enabled", "true" },
                        { "screen_width", "1920" },
                        { "screen_height", "1080" },
                        { "browser_language", "zh-CN" },
                        { "browser_platform", "Win32" },
                        { "browser_name", "Edge" },
                        { "browser_version", "137.0.0.0" },
                        { "browser_online", "true" },
                        { "engine_name", "Blink" },
                        { "engine_version", "137.0.0.0" },
                        { "os_name", "Windows" },
                        { "os_version", "10" },
                        { "device_memory", "8" },
                        { "platform", "PC" }
                    }
                }
            }
        };
    }

    // 时间戳转日期
    private string ConvertTimestamp(long? timestamp)
    {
        if (timestamp == null) return null;

        DateTimeOffset dateTimeOffset = DateTimeOffset.FromUnixTimeSeconds(timestamp.Value);
        return dateTimeOffset.ToString("yyyy-MM-dd HH:mm:ss");
    }

    // 巨量算数搜索达人
    public async Task<List<Dictionary<string, object>>> SearchDarenAsync()
    {
        string url = "https://trendinsight.oceanengine.com/api/v2/daren/get_sug_great_user_list";

        var data = new
        {
            total = "30",
            keyword = keyword
        };

        using (var client = new HttpClient())
        {
            // 设置请求头
            client.DefaultRequestHeaders.Add("accept", "application/json, text/plain, */*");
            client.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
            client.DefaultRequestHeaders.Add("appsource", "PC");
            client.DefaultRequestHeaders.Add("cache-control", "no-cache");
            client.DefaultRequestHeaders.Add("origin", "https://trendinsight.oceanengine.com");
            client.DefaultRequestHeaders.Add("pragma", "no-cache");
            client.DefaultRequestHeaders.Add("priority", "u=1, i");
            client.DefaultRequestHeaders.Add("referer", $"https://trendinsight.oceanengine.com/arithmetic-index/daren/search?keyword={HttpUtility.UrlEncode(keyword)}");
            client.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
            client.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
            client.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
            client.DefaultRequestHeaders.Add("sec-fetch-dest", "empty");
            client.DefaultRequestHeaders.Add("sec-fetch-mode", "cors");
            client.DefaultRequestHeaders.Add("sec-fetch-site", "same-origin");
            client.DefaultRequestHeaders.Add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
            client.DefaultRequestHeaders.Add("x-secsdk-csrf-token", "DOWNGRADE");

            // 设置Cookie
            client.DefaultRequestHeaders.Add("Cookie", $"sessionid_count={sessionId}");

            var jsonData = JsonConvert.SerializeObject(data);
            var content = new StringContent(jsonData, System.Text.Encoding.UTF8, "application/json");

            try
            {
                var response = await client.PostAsync(url, content);
                var responseString = await response.Content.ReadAsStringAsync();
                var jsonResponse = JsonConvert.DeserializeObject<dynamic>(responseString);

                if (jsonResponse.status == 0)
                {
                    var userList = jsonResponse.data.userlist;
                    if (userList != null && userList.Count > 0)
                    {
                        var result = new List<Dictionary<string, object>>();
                        foreach (var user in userList)
                        {
                            string awemeId = user.aweme_id;
                            if (awemeId == keyword)
                            {
                                result.Add(user.ToObject<Dictionary<string, object>>());
                                return result;
                            }
                        }
                        return userList.ToObject<List<Dictionary<string, object>>>();
                    }
                    else
                    {
                        Console.WriteLine("没有结果");
                        return null;
                    }
                }
                else
                {
                    Console.WriteLine(jsonResponse.ToString());
                    return null;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"请求异常: {ex.Message}");
                return null;
            }
        }
    }

    // 抖音主页获取作品
    private async Task<Dictionary<string, object>> GetAwemeListAsync(string secUid, int currentPage, long maxCursor, string browserName)
    {
        string url = currentPage == 1
            ? "https://www.douyin.com/aweme/v1/web/aweme/post/"
            : "https://www-hj.douyin.com/aweme/v1/web/aweme/post/";

        if (!browserEnvironments.TryGetValue(browserName, out var browserEnv))
        {
            throw new ArgumentException($"Unsupported browser environment: {browserName}");
        }

        // 克隆headers和params以避免修改原始数据
        var headers = new Dictionary<string, string>(browserEnv.Headers);
        var _params = new Dictionary<string, string>(browserEnv.Params);

        // 更新referer
        headers["referer"] = $"https://www.douyin.com/user/{secUid}";

        // 合并基础参数
        var paramsDict = new Dictionary<string, string>
        {
            ["device_platform"] = "webapp",
            ["aid"] = "6383",
            ["channel"] = "channel_pc_web",
            ["sec_user_id"] = secUid,
            ["max_cursor"] = maxCursor.ToString(),
            ["locate_query"] = "false",
            ["show_live_replay_strategy"] = "1",
            ["need_time_list"] = currentPage == 1 ? "1" : "0",
            ["time_list_query"] = "0",
            ["whale_cut_token"] = "",
            ["cut_version"] = "1",
            ["count"] = "25",  // 默认18,可调到41
            ["publish_video_strategy_type"] = "2",
            ["from_user_page"] = "1",
            ["version_code"] = "290100",
            ["version_name"] = "29.1.0",
        };

        // 合并浏览器特定参数
        foreach (var param in _params)
        {
            paramsDict[param.Key] = param.Value;
        }

        // 使用HttpClientHandler处理Cookie
        // var handler = new HttpClientHandler
        // {
        //     UseCookies = true,
        //     CookieContainer = new CookieContainer()
        // };


        // 使用HttpClientHandler处理Cookie和代理
        var handler = new HttpClientHandler
        {
            UseCookies = true,
            CookieContainer = new CookieContainer(),
            // 添加代理设置
            // Proxy = new WebProxy("http://127.0.0.1:7897"), // 替换为实际代理地址
            // UseProxy = true
        };
        // 如果代理需要认证，添加以下代码
        // var proxyCredentials = new NetworkCredential("用户名", "密码");
        // handler.Proxy.Credentials = proxyCredentials;
        // handler.Credentials = proxyCredentials;

        // 添加抖音所需的Cookie
        handler.CookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("sessionid", sessionId));
        handler.CookieContainer.Add(new Uri("https://www-hj.douyin.com"), new Cookie("sessionid", sessionId));


        using (var client = new HttpClient(handler))
        {
            // 设置请求头
            foreach (var header in headers)
            {
                if (!client.DefaultRequestHeaders.Contains(header.Key))
                {
                    client.DefaultRequestHeaders.Add(header.Key, header.Value);
                }
            }

            // 添加抖音API特定的请求头
            // client.DefaultRequestHeaders.Add("accept-encoding", "gzip, deflate, br");
            // client.DefaultRequestHeaders.Add("connection", "keep-alive");

            // 构建查询字符串
            var queryString = string.Join("&", paramsDict.Select(kvp =>
                $"{kvp.Key}={WebUtility.UrlEncode(kvp.Value)}"));
            var requestUri = $"{url}?{queryString}";

            try
            {
                var response = await client.GetAsync(requestUri);
                var responseText = await response.Content.ReadAsStringAsync();

                // 调试输出
                Console.WriteLine($"Request URL: {requestUri}");
                Console.WriteLine($"Response Status: {response.StatusCode}");
                Console.WriteLine($"Response Length: {responseText.Length}");

                if (responseText.Length < 10 || responseText.Contains("error"))
                {
                    Console.WriteLine("Response too short or contains error, switching browser environment...");
                    return await GetAwemeListAsync(secUid, currentPage, maxCursor,
                        browserName == "edge" ? "chrome" : "firefox");
                }

                return JsonConvert.DeserializeObject<Dictionary<string, object>>(responseText);
            }
            catch (HttpRequestException e)
            {
                Console.WriteLine($"Request error: {e.Message}");
                Console.WriteLine($"Stack Trace: {e.StackTrace}");
                return null;
            }
        }
    }

    // 获取用户作品列表
    public async Task GetUserAwemeList(string secUid)
    {
        int currentPage = 1;
        long maxCursor = 0;
        var items = new List<Dictionary<string, object>>();
        int maxCount = 100; // 需要多少个作品
        string browserName = "edge"; // 使用哪个浏览器环境
        bool shouldExit = false;

        while (!shouldExit)
        {
            Console.WriteLine($"正在爬取第{currentPage}页......");

            var jsonData = await GetAwemeListAsync(secUid, currentPage, maxCursor, browserName);

            if (jsonData != null && jsonData.ContainsKey("status_code") && jsonData["status_code"]?.ToString() == "0")
            {
                maxCursor = long.Parse(jsonData["max_cursor"]?.ToString() ?? "0");
                Console.WriteLine($"翻页游标: {maxCursor}");

                bool hasMore = jsonData["has_more"]?.ToString() == "1";
                if (!hasMore)
                {
                    Console.WriteLine("没有下一页");
                    Console.WriteLine($"全部作品也没有{maxCount}条");
                    break;
                }

                var awemeList = jsonData["aweme_list"] as IEnumerable<dynamic>;
                if (awemeList != null)
                {
                    foreach (var i in awemeList)
                    {
                        var item = new Dictionary<string, object>
                        {
                            ["is_top"] = i.is_top, // 1 就是置顶
                            ["aweme_id"] = i.aweme_id,
                            ["aweme_type"] = null,
                            ["aweme_url"] = null,
                            ["desc"] = i.desc,
                            ["images"] = null,
                            ["create_time"] = ConvertTimestamp((long?)i.create_time),
                            ["music_play_url"] = i.music?.play_url?.uri,
                            ["video_play_addr"] = null,
                            ["duration"] = i.duration != null ? (int)(i.duration / 1000) : (int?)null, // 秒
                                                                                                       // 新增封面URL字段（初始为空）
                            ["cover_url"] = null

                        };

                        // 提取封面URL
                        if (i.video != null) // 检查video是否存在
                        {
                            var video = i.video;
                            if (video.cover != null) // 检查cover是否存在
                            {
                                var cover = video.cover;
                                if (cover.url_list != null) // 检查url_list是否存在
                                {
                                    var urlList = cover.url_list as IEnumerable<dynamic>;
                                    if (urlList != null && urlList.Any()) // 检查列表非空
                                    {
                                        // 取列表最后一个元素
                                        item["cover_url"] = urlList.LastOrDefault()?.ToString();
                                    }
                                }
                            }
                        }

                        // 合并统计信息
                        var statistics = i.statistics;
                        if (statistics != null)
                        {
                            foreach (var stat in statistics)
                            {
                                item[stat.Name] = stat.Value;
                            }
                        }

                        // 判断是不是图文
                        var images = i.images;
                        if (images != null)
                        {
                            var urlList = new List<string>();
                            foreach (var img in images)
                            {
                                urlList.Add(img.url_list[0].ToString());
                            }
                            item["images"] = urlList;
                            item["aweme_type"] = "note";
                            item["aweme_url"] = $"https://www.douyin.com/note/{i.aweme_id}";
                        }
                        else
                        {
                            item["aweme_type"] = "video";
                            item["aweme_url"] = $"https://www.douyin.com/video/{i.aweme_id}";
                        }

                        // 获取视频下载地址
                        var videoPlayAddr = i.video?.play_addr?.url_list;
                        if (videoPlayAddr != null)
                        {
                            foreach (var playAddr in videoPlayAddr)
                            {
                                if (playAddr.ToString().Contains("v3-web.douyinvod.com"))
                                {
                                    item["video_play_addr"] = playAddr;
                                    break;
                                }
                            }
                        }

                        Console.WriteLine(JsonConvert.SerializeObject(item, Formatting.Indented));
                        items.Add(item);

                        if (items.Count >= maxCount)
                        {
                            Console.WriteLine($"满足{maxCount}条");
                            shouldExit = true;
                            break;
                        }
                    }
                }
            }

            currentPage++;
        }

        Console.WriteLine("======== 爬取完成 ==========");
        Console.WriteLine($"共{currentPage - 1}页，一共{items.Count}个作品");
    }

    public async Task MainAsync()
    {
        var result = await SearchDarenAsync();
        if (result != null && result.Count == 1)
        {
            var user = result[0];
            Console.WriteLine($"精准搜索结果: {JsonConvert.SerializeObject(user, Formatting.Indented)}");

            string userName = user["user_name"]?.ToString();
            string awemeUrl = user["aweme_url"]?.ToString();

            string secUid = awemeUrl.Split(new[] { "user/" }, StringSplitOptions.None).Last();
            Console.WriteLine($"开始采集 --- {userName} --- 前100个作品");

            await GetUserAwemeList(secUid);
        }
        else if (result != null)
        {
            Console.WriteLine("模糊搜索结果");
            foreach (var i in result)
            {
                Console.WriteLine(JsonConvert.SerializeObject(i, Formatting.Indented));
            }
        }
    }

    // 辅助类
    private class BrowserEnvironment
    {
        public Dictionary<string, string> Headers { get; set; }
        public Dictionary<string, string> Params { get; set; }
    }
}



class Program
{
    static async Task Main(string[] args)
    {
        var spider = new DouyinSpider();
        await spider.MainAsync();
    }
}


