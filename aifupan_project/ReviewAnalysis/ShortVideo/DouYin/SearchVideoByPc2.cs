using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class SearchVideoByPc2
    {
        //// 代理设置 - 修改这里
        private readonly IWebProxy _proxy = null;
        private HttpClient _httpClient;
        // 代理相关属性
        private static volatile ProxyIpVo _currentProxyIp;
        private static volatile bool _isUseProxy;
        private static long _proxyExpireTime = 0;
        private static readonly object _proxyIpLock = new object();
        private static string _keyword;
        private readonly string _userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36";
        
        /// <summary>
        /// 调用抖音搜索视频接口，手动释放资源
        /// </summary>
        /// <param name="keyword"></param>
        /// <param name="next"></param>
        /// <returns></returns>
        public static async Task<List<DouYinVideoInfo>> SearchDouyinVideo(string keyword, int[] next = null)
        {
            SearchVideoByPc2 searchVideo = null;
            try
            {
                _keyword = keyword;
                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
                next = next ?? new int[] { 50, 100 };
                List<DouYinVideoInfo> list = null;
                for (int i = 0; i < 3; i++)
                {
                    searchVideo = new SearchVideoByPc2();
                    list = await searchVideo.MainAsync(videoNum);
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

        public SearchVideoByPc2()
        {
            //_userAgent = MobileUaHelper.GetRandomMobileUa();
            _userAgent = MobileUaHelper.GetRandomIOSUA();
            CheckProxyExpire();
            // 开始就使用代理
            UseProxyIP();
            if (_currentProxyIp != null)
            {
                _proxy = new WebProxy(_currentProxyIp.ip, Convert.ToInt32(_currentProxyIp.port))
                {
                    Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword)
                };
            }

            var handler = new HttpClientHandler
            {
                UseCookies = true,
                CookieContainer = new CookieContainer(),
                //AutomaticDecompression = DecompressionMethods.All,
                Proxy = _proxy,
                UseProxy = _proxy != null
            };

            _httpClient = new HttpClient(handler);
            _httpClient.DefaultRequestHeaders.Add("User-Agent", _userAgent);
        }

        // 获取 ttwid
        public async Task<Dictionary<string, string>> GetTtwidFromJingxuanAsync()
        {
            var headers = new Dictionary<string, string>
            {
                { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36" },
                { "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7" },
                { "sec-ch-ua", "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"" },
                { "sec-ch-ua-mobile", "?0" },
                { "sec-ch-ua-platform", "\"Windows\"" },
                { "upgrade-insecure-requests", "1" },
                { "sec-fetch-site", "none" },
                { "sec-fetch-mode", "navigate" },
                { "sec-fetch-user", "?1" },
                { "sec-fetch-dest", "document" },
                { "accept-language", "zh-CN,zh;q=0.9" },
                { "priority", "u=0, i" }
            };

            var url = "https://www.douyin.com/jingxuan";
            var request = new HttpRequestMessage(HttpMethod.Get, url);

            foreach (var header in headers)
            {
                request.Headers.Add(header.Key, header.Value);
            }

            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();

            var cookies = new Dictionary<string, string>();
            var cookieHeaders = response.Headers.GetValues("Set-Cookie");
            foreach (var cookieHeader in cookieHeaders)
            {
                var cookieParts = cookieHeader.Split(';')[0].Split('=');
                if (cookieParts.Length == 2)
                {
                    cookies[cookieParts[0]] = cookieParts[1];
                }
            }

            return cookies;
        }

        private string ConvertTimestamp(long timestamp)
        {
            if (timestamp == 0) return null;

            DateTimeOffset dateTime;
            if (timestamp.ToString().Length == 13)
            {
                dateTime = DateTimeOffset.FromUnixTimeMilliseconds(timestamp);
            }
            else
            {
                dateTime = DateTimeOffset.FromUnixTimeSeconds(timestamp);
            }

            return dateTime.LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss");
        }

        private string GetCurrentTime()
        {
            return DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        }

        public async Task<HttpResponseMessage> SearchVideoItemAsync(
            Dictionary<string, string> cookies,
            int currentPage,
            string searchId,
            string sortType,
            string publishTime,
            string filterDuration)
        {
            var headers = new Dictionary<string, string>
            {
                { "User-Agent", _userAgent },
                { "pragma", "no-cache" },
                { "cache-control", "no-cache" },
                { "sec-ch-ua-platform", "\"Android\"" },
                { "sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"" },
                { "sec-ch-ua-mobile", "?1" },
                { "sec-fetch-site", "same-origin" },
                { "sec-fetch-mode", "cors" },
                { "sec-fetch-dest", "empty" },
                { "referer", "https://so.douyin.com/" },
                { "accept-language", "zh-CN,zh;q=0.9" },
                { "priority", "u=1, i" }
            };

            // 添加cookies到请求头
            if (cookies != null && cookies.Count > 0)
            {
                var cookieString = string.Join("; ", cookies.Select(kv => $"{kv.Key}={kv.Value}"));
                headers["Cookie"] = cookieString;
            }

            var url = "https://www.douyin.com/aweme/v1/web/search/item/";
            var queryParams = new Dictionary<string, string>
            {
                { "device_platform", "webapp" },
                { "aid", "581610" },
                { "channel", "channel_pc_web" },
                { "search_channel", "aweme_video_web" },
                { "enable_history", "1" },
                { "keyword", _keyword },
                { "search_source", "normal_search" },
                { "query_correct_type", "1" },
                { "is_filter_search", "0" },
                { "from_group_id", "" },
                { "disable_rs", "0" },
                { "offset", ((currentPage - 1) * 25).ToString() },
                { "count", "25" },
                { "need_filter_settings", currentPage == 1 ? "1" : "0" },
                { "update_version_code", "170400" },
                { "version_code", "170400" },
                { "version_name", "17.4.0" },
                { "platform", "PC" },
                { "webid", "" }
            };

            if (currentPage > 1 && !string.IsNullOrEmpty(searchId))
            {
                queryParams["search_id"] = searchId;
            }

            if (sortType != "0" || publishTime != "0" || !string.IsNullOrEmpty(filterDuration))
            {
                queryParams["publish_time"] = publishTime;
                queryParams["is_filter_search"] = "1";
            }

            var queryString = string.Join("&", queryParams.Select(kv => $"{kv.Key}={Uri.EscapeDataString(kv.Value)}"));
            var fullUrl = $"{url}?{queryString}";

            var request = new HttpRequestMessage(HttpMethod.Get, fullUrl);

            foreach (var header in headers)
            {
                request.Headers.Add(header.Key, header.Value);
            }

            var response = await _httpClient.SendAsync(request);
            return response;
        }

        private JsonDocument SearchHandler(HttpResponseMessage response, string responseText)
        {
            try
            {
                FileUtils.log("请求搜索视频接口");

                if (responseText.Length < 10)
                {
                    FileUtils.log("被风控,没有数据!!!!!!!!!!!!!!!!!!!!!!");
                    FileUtils.log(responseText);
                    return null;
                }

                var jsonDoc = JsonDocument.Parse(responseText);

                if (jsonDoc.RootElement.TryGetProperty("search_nil_info", out _))
                {
                    FileUtils.log($"出现风控！验证码风控！切换浏览器 {responseText}");
                    return null;
                }

                if (jsonDoc.RootElement.TryGetProperty("status_code", out var statusCodeElement) &&
                    statusCodeElement.GetInt32() == 0 &&
                    jsonDoc.RootElement.TryGetProperty("data", out var dataElement) &&
                    dataElement.ValueKind != JsonValueKind.Null)
                {
                    return jsonDoc;
                }

                FileUtils.log(responseText);
                FileUtils.log("出现风控！没有成功返回数据，切换浏览器");
                return null;
            }
            catch (JsonException ex)
            {
                FileUtils.log($"JSON解析出错: {ex.Message}");
                return null;
            }
        }

        public async Task<List<DouYinVideoInfo>> MainAsync(int videoMaxCount)
        {
            // 1.获取cookies
            var cookies = await GetTtwidFromJingxuanAsync();
            FileUtils.log($"获取的cookies: {string.Join(", ", cookies.Select(kv => $"{kv.Key}={kv.Value}"))}");

            var awemeIdSet = new HashSet<string>(); // 去重 id 集合

            int currentPage = 1;
            string searchId = null;
            int maxCount = videoMaxCount; // 爬取的最大作品数
            int requestsCount = 0;
            bool shouldExit = false; // 用于控制是否退出整个爬取循环
            List<DouYinVideoInfo> douYinVideoInfoList = new List<DouYinVideoInfo>();
            var diggCountList = new List<long>();

            while (currentPage < 20)
            {
                FileUtils.log($"正在爬取第{currentPage}页。。。。。");
                await Task.Delay(3000);

                string sortType = "1"; // 排序方式 0:综合排序, 1:最多点赞, 2:最新发布
                string publishTime = "0"; // 发布时间 0:不限, 1:一天内, 7:一周内, 180:半年内
                string filterDuration = ""; // 视频时长 空字符串:默认不限, 0-1:一分钟以下, 1-5:五分钟, 5-1000:五分钟以上

                var response = await SearchVideoItemAsync(cookies, currentPage, searchId, sortType, publishTime, filterDuration);
                var responseText = await response.Content.ReadAsStringAsync();
                var jsonData = SearchHandler(response, responseText);

                if (jsonData == null)
                {
                    FileUtils.log("++++++++ 出现风控++++++++");
                    break;
                }

                requestsCount++;
                FileUtils.log($"当前时间：{GetCurrentTime()} 当前请求次数: {requestsCount}");

                var root = jsonData.RootElement;

                if (currentPage == 1)
                {
                    if (root.TryGetProperty("extra", out var extraElement) &&
                        extraElement.TryGetProperty("logid", out var logidElement))
                    {
                        searchId = logidElement.GetString();
                        FileUtils.log($"search_id: {searchId}");
                    }
                }

                // 修复has_more的处理，先检查值类型
                bool hasMore = false;
                if (root.TryGetProperty("has_more", out var hasMoreElement))
                {
                    // 检查值类型
                    if (hasMoreElement.ValueKind == JsonValueKind.Number)
                    {
                        // 抖音API中，has_more可能是1或0
                        hasMore = hasMoreElement.GetInt32() == 1;
                    }
                    else if (hasMoreElement.ValueKind == JsonValueKind.True ||
                            hasMoreElement.ValueKind == JsonValueKind.False)
                    {
                        hasMore = hasMoreElement.GetBoolean();
                    }

                    if (hasMore)
                    {
                        FileUtils.log("还有下一页");
                    }
                    else
                    {
                        FileUtils.log("没有下一页");
                        break;
                    }
                }
                else
                {
                    FileUtils.log("没有has_more字段");
                    break;
                }

                if (root.TryGetProperty("data", out var dataElement) &&
                    dataElement.ValueKind == JsonValueKind.Array)
                {
                    foreach (var item in dataElement.EnumerateArray())
                    {
                        try
                        {
                            JsonElement awemeInfo;
                            if (item.TryGetProperty("aweme_info", out var awemeInfoProp))
                                awemeInfo = awemeInfoProp;
                            else
                                continue;

                            // 检查是否有视频封面
                            var coverUrl = JMESPath.Search<string>(awemeInfo, "video.cover.url_list[0]");
                            var awemeId = JMESPath.Search<string>(awemeInfo, "aweme_id");
                            var mytitle = JMESPath.Search<string>(awemeInfo, "desc") ?? "";
                            var filehash = JMESPath.Search<string>(awemeInfo, "video.play_addr.file_hash");

                            if (string.IsNullOrEmpty(coverUrl) || string.IsNullOrEmpty(awemeId) || string.IsNullOrEmpty(mytitle) || string.IsNullOrEmpty(filehash))
                                continue;

                            // 检查视频去重
                            if (!awemeIdSet.Contains(awemeId))
                            {

                                awemeIdSet.Add(awemeId);

                                var diggCount = JMESPath.Search<long?>(awemeInfo, "statistics.digg_count");
                                if (diggCount.HasValue)
                                {
                                    diggCountList.Add(diggCount.Value);
                                }

                                var videoDuration = JMESPath.Search<long?>(awemeInfo, "video.duration");
                                var secUid = JMESPath.Search<string>(awemeInfo, "author.sec_uid");

                                var uniqueId = JMESPath.Search<string>(awemeInfo, "author.unique_id");
                                if (string.IsNullOrEmpty(uniqueId))
                                {
                                    uniqueId = JMESPath.Search<string>(awemeInfo, "author.short_id");
                                }
                                //抖音视频信息赋值
                                var douYinVideoInfo = new DouYinVideoInfo
                                {
                                    aweme_id = awemeId,
                                    //unique_id = uniqueId,
                                    sec_uid = secUid ?? "未知",

                                    desc = SafeTruncate(JMESPath.Search<string>(awemeInfo, "desc") ?? "", 500),
                                    create_time = JMESPath.Search<long?>(awemeInfo, "create_time"),


                                    author = new DouYinAuthorInfo
                                    {
                                        sec_uid = secUid,
                                        nickname = JMESPath.Search<string>(awemeInfo, "author.nickname") ?? "未知",
                                        follower_count = JMESPath.Search<long?>(awemeInfo, "author.follower_count"),
                                        avatar_thumb = new avatarThumb
                                        {
                                            url_list = new List<string>
                                                {
                                                    JMESPath.Search<string>(awemeInfo, "author.avatar_thumb.url_list[0]") ?? ""
                                                }
                                        }
                                    },
                                    video = new DouYinVideoDetail
                                    {
                                        duration = (long)videoDuration / 1000,
                                        cover = new cover
                                        {
                                            url_list = new List<string>
                                                {
                                                    JMESPath.Search<string>(awemeInfo, "video.cover.url_list[0]" ?? "")
                                                }
                                            //uri = coverUrl
                                        },
                                        play_addr = new playAddr
                                        {
                                            file_hash = filehash
                                        }

                                    },
                                    statistics = new DouYinStatistics
                                    {
                                        digg_count = JMESPath.Search<long?>(awemeInfo, "statistics.digg_count"),
                                        comment_count = JMESPath.Search<long?>(awemeInfo, "statistics.comment_count"),
                                        collect_count = JMESPath.Search<long?>(awemeInfo, "statistics.collect_count"),
                                        share_count = JMESPath.Search<long?>(awemeInfo, "statistics.share_count")
                                    }


                                };
                                douYinVideoInfoList.Add(douYinVideoInfo);

                                if (douYinVideoInfoList.Count >= maxCount)
                                {
                                    shouldExit = true;
                                    break;
                                }
                            }
                        }
                        catch (Exception itemEx)
                        {
                            FileUtils.LogError($"爆款中处理单条数据时出错: {itemEx.Message}");
                            continue;
                        }
                    }
                }

                if (shouldExit) break;
                currentPage++;
            }

            FileUtils.log("======== 爬取完成 ==========");
            FileUtils.log($"共{currentPage - 1}页，一共{awemeIdSet.Count}个作品");
            return douYinVideoInfoList;
        }
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
