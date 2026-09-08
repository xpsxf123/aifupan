using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using RestSharp;
using ReviewAnalysis.api;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web;
using ReviewAnalysis.ShortVideo.DouYin.dto;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class SearchVideoByH5
    {
        private static int chromeVersion = 130;

        // 代理相关属性
        private static volatile ProxyIpVo _currentProxyIp;
        private static volatile bool _isUseProxy;
        private static long _proxyExpireTime = 0;
        private static readonly object _proxyIpLock = new object();

        #region 初始化字段
        private readonly string _keyword;
        private readonly string _reloadNavStart;
        private readonly int _count = 20; // 每页返回数据
        private readonly int _maxCount = 150; // 最大爬取数
        private readonly string _searchPd = "general"; // 搜索类型

        private readonly string _ua;
        private readonly string _innerWidth;
        private readonly string _innerHeight;
        private readonly List<string> _unableUa = new List<string>();
        private readonly bool _useProxy = true; // 是否使用代理（可外部配置）
        private readonly CookieContainer _cookieContainer = new CookieContainer();
        private Dictionary<string, string> _cookies = new Dictionary<string, string>();
        #endregion

        // 随机数实例（避免短时间重复）
        private readonly Random _random = new Random();


        /// <summary>
        /// 调用抖音搜索视频接口，手动释放资源
        /// </summary>
        /// <param name="keyword"></param>
        /// <param name="next"></param>
        /// <returns></returns>
        public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] next = null)
        {
            SearchVideoByH5 searchVideo = null;
            try
            {
                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
                next = next ?? new int[] { 50, 100 };
                List<DouYinVideoInfo> list = null;
                for (int i = 0; i < 3; i++)
                {
                    searchVideo = new SearchVideoByH5(keyword);
                    list = searchVideo.startSearchVideo();//keyword, videoNum, videoNum * 2, next, true
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
        /// 构造函数
        /// </summary>
        /// <param name="keyword">搜索关键词</param>
        public SearchVideoByH5(string keyword)
        {
            _keyword = keyword;
            _reloadNavStart = ((long)(DateTime.UtcNow - new DateTime(1970, 1, 1)).TotalMilliseconds).ToString();
            // 随机UA
            _ua = MobileUaHelper.GetRandomMobileUa();
            // 随机屏幕尺寸
            _innerWidth = _random.Next(768, 1279).ToString();
            _innerHeight = _random.Next(568, 1792).ToString();

            // 初始化Cookie
            _cookies = GetCookies();
            // 初始化代理
            if (_useProxy) UseProxyIP();
        }

        #region 工具方法（对应Python的工具函数）
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
        /// 获取当前格式化时间
        /// </summary>
        private string GetCurrentTime()
        {
            return DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        }

        /// <summary>
        /// 解析UA，返回平台和版本信息
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

        #region Cookie获取（对应Python的get_cookies）
        private Dictionary<string, string> GetCookies()
        {
            var cookies = new Dictionary<string, string>();
            var cookieContainer = new CookieContainer();

            // 第一步：获取pre_reload_logid
            var firstRequest = new RestRequest("https://so.douyin.com/s", Method.Get);
            firstRequest.AddHeader("User-Agent", _ua);
            firstRequest.AddHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            firstRequest.AddHeader("pragma", "no-cache");
            firstRequest.AddHeader("cache-control", "no-cache");
            firstRequest.AddHeader("upgrade-insecure-requests", "1");
            firstRequest.AddHeader("sec-fetch-site", "none");
            firstRequest.AddHeader("sec-fetch-mode", "navigate");
            firstRequest.AddHeader("sec-fetch-user", "?1");
            firstRequest.AddHeader("sec-fetch-dest", "document");
            firstRequest.AddHeader("accept-language", "zh-CN,zh;q=0.9");
            firstRequest.AddHeader("priority", "u=0, i");

            firstRequest.AddParameter("search_entrance", "aweme_top_bar");
            firstRequest.AddParameter("keyword", _keyword);
            firstRequest.AddParameter("is_no_width_reload", "0");

            var clientOptions = GetRestClientOptions(cookieContainer);
            var firstClient = new RestClient(clientOptions);
            var firstResponse = firstClient.Execute(firstRequest);

            if (!firstResponse.IsSuccessful)
            {
                FileUtils.log($"获取pre_reload_logid失败：{firstResponse.ErrorMessage}");
                return cookies;
            }

            // 提取pre_reload_logid
            var preReloadLogidMatch = Regex.Match(firstResponse.Content, @"document.cookie='pre_reload_logid=(.*?)'");
            if (!preReloadLogidMatch.Success)
            {
                FileUtils.log("未提取到pre_reload_logid");
                return cookies;
            }
            var preReloadLogid = preReloadLogidMatch.Groups[1].Value;
            FileUtils.log($"获取pre_reload_logid: {preReloadLogid}");

            // 第二步：获取search_webid和ttwid
            var secondRequest = new RestRequest("https://so.douyin.com/s", Method.Get);
            secondRequest.AddHeader("User-Agent", _ua);
            secondRequest.AddHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            secondRequest.AddHeader("pragma", "no-cache");
            secondRequest.AddHeader("cache-control", "no-cache");
            secondRequest.AddHeader("upgrade-insecure-requests", "1");
            secondRequest.AddHeader("sec-fetch-site", "same-origin");
            secondRequest.AddHeader("sec-fetch-mode", "navigate");
            secondRequest.AddHeader("sec-fetch-dest", "document");
            secondRequest.AddHeader("referer", $"https://so.douyin.com/s?keyword={HttpUtility.UrlEncode(_keyword)}&innerWidth={_innerWidth}&innerHeight={_innerHeight}&reloadNavStart={_reloadNavStart}&is_no_width_reload=1&reload_from=abnormal_wh_1");
            secondRequest.AddHeader("accept-language", "zh-CN,zh;q=0.9");
            secondRequest.AddHeader("priority", "u=0, i");

            // 添加Cookie参数
            //secondRequest.AddCookie("douyin_search_screen", $"{_innerWidth}*{_innerHeight}", "/", ".douyin.com");
            //secondRequest.AddCookie("pre_reload_logid", preReloadLogid, "/", ".douyin.com");
            secondRequest.AddHeader("Cookie", $"douyin_search_screen={_innerWidth}*{_innerHeight}");
            secondRequest.AddHeader("Cookie", $"pre_reload_logid={preReloadLogid}");

            secondRequest.AddParameter("search_entrance", "aweme_top_bar");
            secondRequest.AddParameter("keyword", _keyword);
            secondRequest.AddParameter("is_no_width_reload", "1");
            secondRequest.AddParameter("innerWidth", _innerWidth);
            secondRequest.AddParameter("innerHeight", _innerHeight);
            secondRequest.AddParameter("reloadNavStart", _reloadNavStart);
            secondRequest.AddParameter("reload_from", "abnormal_wh_1");

            var secondClient = new RestClient(GetRestClientOptions(cookieContainer));
            var secondResponse = secondClient.Execute(secondRequest);

            if (!secondResponse.IsSuccessful)
            {
                FileUtils.log($"获取search_webid失败：{secondResponse.ErrorMessage}");
                return cookies;
            }

            // 提取search_webid
            var searchWebidMatch = Regex.Match(secondResponse.Content, @"setSearchWebid\(""(\d+)""\);");
            var searchWebid = searchWebidMatch.Success ? searchWebidMatch.Groups[1].Value : "";
            FileUtils.log($"获取search_webid: {searchWebid}");

            // 提取ttwid
            var ttwid = "";
            foreach (System.Net.Cookie cookie in secondResponse.Cookies)
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

        //    // 构造最终Cookie
        //    cookies = new Dictionary<string, string>
        //{
        //    { "douyin_search_screen", $"{_innerWidth}*{_innerHeight}" },
        //    { "pre_reload_logid", preReloadLogid },
        //    { "ttwid", ttwid },
        //    { "s_recover_ttwid", ttwid },
        //    { "tti_opt_long_task", "0" },
        //    { "search_webid", searchWebid },
        //    { "__tea_cache_tokens_581610", "{%22_type_%22:%22default%22}" },
        //    { "x-use-dcz", "0" },
        //    { "use-esr", "0" },
        //    { "browser_font_scale", "1" }//,
        //    //{ "gfkadpd", "581610,29095" }
        //};
            // 修正后（正确）：仅保留服务端返回的核心 Cookie，移除非法手动构造项
            cookies = new Dictionary<string, string>
{
    { "douyin_search_screen", $"{_innerWidth}*{_innerHeight}" },
    { "pre_reload_logid", preReloadLogid },
    { "ttwid", ttwid },
    { "s_recover_ttwid", ttwid },
    { "search_webid", searchWebid } // 仅保留核心必要 Cookie，其余由服务端自动返回
};

            // 将Cookie添加到容器
            foreach (var kv in cookies)
            {
                _cookieContainer.Add(new Uri("https://so.douyin.com"), new Cookie(kv.Key, kv.Value));
            }

            return cookies;
        }
        #endregion

        #region 核心搜索方法（对应Python的douyin_search）
        private RestResponse DouyinSearchRequest(string searchId, int currentPage)
        {
            var uaRes = ParseUA(_ua);
            var request = new RestRequest("https://so.douyin.com/s/fe/api/aweme/v2/search/msite/general/search/single/", Method.Get);

            // 添加请求头
            request.AddHeader("User-Agent", _ua);
            request.AddHeader("pragma", "no-cache");
            request.AddHeader("cache-control", "no-cache");
            request.AddHeader("content-type", "application/x-www-form-urlencoded");
            request.AddHeader("sec-fetch-site", "same-origin");
            request.AddHeader("sec-fetch-mode", "cors");
            request.AddHeader("sec-fetch-dest", "empty");
            request.AddHeader("referer", "https://so.douyin.com/s");
            request.AddHeader("accept-language", "zh-CN,zh;q=0.9");
            request.AddHeader("priority", "u=1, i");

            // 添加请求参数
            request.AddParameter("aid", "581610");
            request.AddParameter("device_platform", "webapp");
            request.AddParameter("search_source", "normal_search");
            request.AddParameter("app_version", "290000");
            request.AddParameter("version_code", "290000");
            request.AddParameter("version_name", "29.0.0");
            request.AddParameter("webapp_platform", uaRes["webapp_platform"]);
            request.AddParameter("webapp_platform_version", uaRes["webapp_platform_version"]);
            request.AddParameter("search_entrance", "aweme_top_bar");
            request.AddParameter("keyword", _keyword);
            request.AddParameter("is_no_width_reload", "0");
            request.AddParameter("innerWidth", _innerWidth);
            request.AddParameter("innerHeight", _innerHeight);
            request.AddParameter("reloadNavStart", _reloadNavStart);
            request.AddParameter("reload_from", "abnormal_wh_1");
            request.AddParameter("app_theme", "light");
            request.AddParameter("pd", _searchPd);
            request.AddParameter("count", _count.ToString());
            request.AddParameter("sort_params", "");
            request.AddParameter("appTheme", "light");
            request.AddParameter("need_filter_settings", "1");
            request.AddParameter("location_access", "");
            request.AddParameter("offset", ((currentPage - 1) * _count).ToString());
            request.AddParameter("search_id", searchId);
            request.AddParameter("currentPage", currentPage.ToString());
            request.AddParameter("backtrace", "");
            request.AddParameter("options[stream]", "false");

            // 添加Cookie
            foreach (var kv in _cookies)
            {
                //request.AddCookie(kv.Key, kv.Value, "/", "so.douyin.com");
                request.AddHeader("Cookie", $"{kv.Key}={kv.Value}");
            }

            // 创建客户端并发送请求
            var clientOptions = GetRestClientOptions(_cookieContainer);
            var client = new RestClient(clientOptions);
            try
            {
                return client.Execute(request);
            }
            catch (Exception ex)
            {
                FileUtils.log($"请求出错: {ex.Message}");
                return null;
            }
        }
        #endregion

        #region 响应处理（对应Python的search_handler）
        private JObject SearchHandler(RestResponse response)
        {
            if (response == null || !response.IsSuccessful)
            {
                FileUtils.log("请求失败或响应为空");
                return null;
            }

            // 检查风控
            if (response.Headers.Any(h => h.Name == "Bdturing-Verify"))
            {
                FileUtils.log("出现旋转验证码风控！");
                return null;
            }

            // 检查响应内容长度
            if (string.IsNullOrEmpty(response.Content) || response.Content.Length < 10)
            {
                FileUtils.log("返回数据为空,没有数据");
                FileUtils.log(response.Content);
                return null;
            }

            // 解析JSON
            JObject jsonData;
            try
            {
                jsonData = JObject.Parse(response.Content);
            }
            catch (Exception ex)
            {
                FileUtils.log($"JSON解析失败: {ex.Message}");
                return null;
            }

            // 检查状态码和业务数据
            if (jsonData["status_code"]?.Value<int>() == 0 && jsonData["business_data"] != null)
            {
                return jsonData;
            }
            else
            {
                FileUtils.log("返回了json数据,但是没有结果");
                FileUtils.log(jsonData.ToString());
                return null;
            }
        }
        #endregion

        #region 主执行方法（对应Python的main）
        public List<DouYinVideoInfo> startSearchVideo()
        {
            FileUtils.log($"当前使用ua===》{_ua}");
            FileUtils.log($"cookie信息===》{JsonConvert.SerializeObject(_cookies, Formatting.Indented)}");

            if (_useProxy && _currentProxyIp != null)
            {
                FileUtils.log($"当前使用的ip代理: {_currentProxyIp.ip}:{_currentProxyIp.port}");
            }

            var awemeIdSet = new HashSet<string>(); // 去重
            int currentPage = 1;
            string searchId = "";
            int requestsCount = 0;
            bool shouldExit = false;
            var diggCountList = new List<long?>();

            while (true)
            {
                FileUtils.log($"正在爬取第{currentPage}页。。。。。");
                System.Threading.Thread.Sleep(_random.Next(1500, 3000)); // 随机休眠1.5-3秒

                // 发送搜索请求
                var response = DouyinSearchRequest(searchId, currentPage);
                var jsonData = SearchHandler(response);

                // 检查是否有更多数据
                int hasMore = jsonData?["business_config"]?["has_more"]?.Value<int>() ?? 0;
                if (jsonData == null && hasMore == 0)
                {
                    FileUtils.log("没有下一页 没有更多数据了");
                    break;
                }
                else if (jsonData == null && hasMore == 1)
                {
                    FileUtils.log("还有下一页 但是搜索失败");
                    break;
                }
                else if (jsonData != null && hasMore == 1)
                {
                    FileUtils.log("还有下一页 还有更多");
                }
                else if (jsonData != null && hasMore == 0)
                {
                    FileUtils.log("没有下一页 没有更多数据了");
                }

                requestsCount++;
                FileUtils.log($"当前时间：{GetCurrentTime()} 当前请求次数: {requestsCount}");

                //if (jsonData != null)
                //{
                //    // 第一页提取search_id
                //    if (currentPage == 1)
                //    {
                //        searchId = jsonData["extra"]?["logid"]?.Value<string>() ?? "";
                //        FileUtils.log($"search_id: {searchId}");
                //    }

                //    // 解析业务配置
                //    var businessConfig = new BusinessConfig
                //    {
                //        keyword = jsonData["business_config"]?["keyword"]?.Value<string>(),
                //        has_more = jsonData["business_config"]?["has_more"]?.Value<int>(),
                //        is_filter_search = jsonData["business_config"]?["is_filter_search"]?.Value<int>(),
                //        search_nil_info = jsonData["business_config"]?["search_nil_info"]?.Value<JObject>(),
                //        next_page = jsonData["business_config"]?["next_page"]?.Value<int>(),
                //        card_count = jsonData["business_config"]?["card_count"]?.Value<int>()
                //    };

                //    // 解析视频数据
                //    foreach (var item in jsonData["business_data"]?.Children() ?? new JArray())
                //    {
                //        var awemeInfo = item["data"]?["aweme_info"] ?? new JObject();

                //        // 检查视频封面（有封面则为视频）
                //        var dynamicCover = awemeInfo["video"]?["dynamic_cover"]?["url_list"]?[0]?.Value<string>();
                //        if (dynamicCover == null) continue;

                //        var awemeId = awemeInfo["aweme_id"]?.Value<string>();
                //        // 去重
                //        if (!string.IsNullOrEmpty(awemeId) && !awemeIdSet.Contains(awemeId))
                //        {
                //            awemeIdSet.Add(awemeId);
                //            diggCountList.Add(awemeInfo["statistics"]?["digg_count"]?.Value<long?>());
                //        }

                //        // 解析视频时长
                //        long? videoDuration = awemeInfo["video"]?["duration"]?.Value<long?>();
                //        long? formattedDuration = videoDuration.HasValue && videoDuration > 1000
                //            ? videoDuration / 1000
                //            : videoDuration;

                //        // 构造视频信息对象
                //        var videoInfo = new DouYinVideoInfo
                //        {
                //            aweme_id = awemeId,
                //            desc = awemeInfo["desc"]?.Value<string>(),
                //            create_time = ConvertTimestamp(awemeInfo["create_time"]?.Value<string>()),
                //            business_config = businessConfig,
                //            // 解析作者信息
                //            author = new AuthorInfo
                //            {
                //                uid = awemeInfo["author"]?["uid"]?.Value<string>(),
                //                nickname = awemeInfo["author"]?["nickname"]?.Value<string>(),
                //                short_id = awemeInfo["author"]?["short_id"]?.Value<string>(),
                //                unique_id = awemeInfo["author"]?["unique_id"]?.Value<string>(),
                //                signature = awemeInfo["author"]?["signature"]?.Value<string>(),
                //                sec_uid = awemeInfo["author"]?["sec_uid"]?.Value<string>(),
                //                avatar_thumb = awemeInfo["author"]?["avatar_thumb"]?["url_list"]?[0]?.Value<string>(),
                //                aweme_count = awemeInfo["author"]?["aweme_count"]?.Value<long?>(),
                //                following_count = awemeInfo["author"]?["following_count"]?.Value<long?>(),
                //                follower_count = awemeInfo["author"]?["follower_count"]?.Value<long?>(),
                //                favoriting_count = awemeInfo["author"]?["favoriting_count"]?.Value<long?>(),
                //                total_favorited = awemeInfo["author"]?["total_favorited"]?.Value<long?>()
                //            },
                //            // 解析音乐信息
                //            music = new MusicInfo
                //            {
                //                title = awemeInfo["music"]?["title"]?.Value<string>(),
                //                play_url = awemeInfo["music"]?["play_url"]?["uri"]?.Value<string>(),
                //                duration = awemeInfo["music"]?["duration"]?.Value<long?>()
                //            },
                //            // 解析视频信息
                //            video = new VideoInfo
                //            {
                //                duration = formattedDuration,
                //                dynamic_cover = dynamicCover,
                //                play_url = awemeInfo["video"]?["play_addr"]?["url_list"]?.Select(u => u.Value<string>()).ToList() ?? new List<string>()
                //            },
                //            // 解析统计信息
                //            statistics = new StatisticsInfo
                //            {
                //                digg_count = awemeInfo["statistics"]?["digg_count"]?.Value<long?>(),
                //                comment_count = awemeInfo["statistics"]?["comment_count"]?.Value<long?>(),
                //                share_count = awemeInfo["statistics"]?["share_count"]?.Value<long?>(),
                //                play_count = awemeInfo["statistics"]?["play_count"]?.Value<long?>()
                //            }
                //        };

                //        // 区分图文/视频
                //        var images = item["images"]?.Children()?.ToList();
                //        if (images != null && images.Count > 0)
                //        {
                //            videoInfo.images = images.Select(img => img["url_list"]?[0]?.Value<string>()).Where(u => !string.IsNullOrEmpty(u)).ToList();
                //            videoInfo.aweme_type = "note";
                //            videoInfo.aweme_url = $"https://www.douyin.com/note/{awemeId}";
                //        }
                //        else
                //        {
                //            videoInfo.aweme_type = "video";
                //            videoInfo.aweme_url = $"https://www.douyin.com/video/{awemeId}";
                //        }

                //        // 输出视频信息
                //        FileUtils.log(JsonConvert.SerializeObject(videoInfo, Formatting.Indented));

                //        // 检查是否达到最大爬取数
                //        if (awemeIdSet.Count >= _maxCount)
                //        {
                //            FileUtils.log($"满足{_maxCount}条");
                //            shouldExit = true;
                //            break;
                //        }
                //    }

                //    // 检查是否有下一页
                //    if (hasMore == 0)
                //    {
                //        FileUtils.log("没有下一页");
                //        break;
                //    }
                //    else
                //    {
                //        FileUtils.log("还有下一页");
                //    }
                //}
                //else
                //{
                //    shouldExit = true;
                //}

                if (shouldExit) break;
                currentPage++;
                if (currentPage > 10) break;
            }

            // 爬取完成统计
            FileUtils.log("======== 爬取完成 ==========");
            FileUtils.log($"共{currentPage - 1}页，一共{awemeIdSet.Count}个作品");
            FileUtils.log("点赞数列表");
            FileUtils.log(string.Join(", ", diggCountList));
            FileUtils.log("排序之后点赞数列表");
            FileUtils.log(string.Join(", ", diggCountList.OrderByDescending(x => x)));
            return null;
        }
        #endregion

        #region 代理和RestClient配置方法
        /// <summary>
        /// 获取RestClient配置（包含代理和Cookie）
        /// </summary>
        private RestClientOptions GetRestClientOptions(CookieContainer cookieContainer)
        {
            CheckProxyExpire();
            var options = new RestClientOptions
            {
                CookieContainer = cookieContainer,
                FollowRedirects = true,
                //Timeout = TimeSpan.FromSeconds(10),
                RemoteCertificateValidationCallback = (sender, cert, chain, sslPolicyErrors) => true // 忽略SSL验证
            };

            // 设置代理
            if (_useProxy && _currentProxyIp != null && !string.IsNullOrEmpty(_currentProxyIp.ip) && !string.IsNullOrEmpty(_currentProxyIp.port))
            {
                var proxy = new WebProxy($"{_currentProxyIp.ip}:{_currentProxyIp.port}");
                if (!string.IsNullOrEmpty(_currentProxyIp.proxyUsername) && !string.IsNullOrEmpty(_currentProxyIp.proxyPassword))
                {
                    proxy.Credentials = new NetworkCredential(_currentProxyIp.proxyUsername, _currentProxyIp.proxyPassword);
                }
                options.Proxy = proxy;
            }
            else
            {
                options.Proxy = null;
            }

            return options;
        }

        /// <summary>
        /// 检查代理是否过期
        /// </summary>
        private void CheckProxyExpire()
        {
            lock (_proxyIpLock)
            {
                if (!_isUseProxy) return;

                long currentTime = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1)).TotalMilliseconds / 1000; // 秒级时间戳
                if (currentTime >= _proxyExpireTime)
                {
                    FileUtils.log($"代理IP已过期，当前时间：{currentTime}，过期时间：{_proxyExpireTime}");
                    _isUseProxy = false;
                    _currentProxyIp = null;
                }
            }
        }

        /// <summary>
        /// 使用代理IP（需替换为你的ProxyApi.GetProxyIpSync逻辑）
        /// </summary>
        private void UseProxyIP()
        {
            lock (_proxyIpLock)
            {
                if (_isUseProxy) return;

                // 替换为你的代理获取逻辑（和你原代码一致）
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
        #endregion

    }
   
}
