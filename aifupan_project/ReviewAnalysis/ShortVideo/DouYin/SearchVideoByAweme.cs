using douyin.Utils;
using Newtonsoft.Json;
using RestSharp;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.hotSearch;
using ReviewAnalysis.vo.proxyIP;
using Swan;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Reflection;
using System.Runtime.Remoting.Lifetime;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Web;
using Swan;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using RestSharp;
using ReviewAnalysis.Utils;
using douyin.Utils;
using System.Reflection;
using System.Collections;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.hotSearch;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    /// <summary>
    /// 通过Aweme,抖音平台API获取视频信息
    /// 管理员身份运行 ipconfig /flushdns  # 刷新 DNS 缓存（核心）
    /// ipconfig /release   # 释放旧 IP
    /// ipconfig /renew     # 重新获取 IP（可选）
    /// </summary>
    public class SearchVideoByAweme
    {
        private static string _sessionid = string.Empty;
        public static HotSearchEmailAccountVo _emailAccount;
        private static DateTime _sessionidTime = DateTime.MinValue;
        private readonly RestClient _restClient;

        /// <summary>
        /// 调用抖音搜索视频接口，手动释放资源
        /// </summary>
        /// <param name="keyword"></param>
        /// <param name="videoNum"></param>
        /// <returns></returns>
        public static List<DouYinVideoInfo> SearchDouyinVideo(string keyword, int[] next = null)
        {
            try
            {
                int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
                next = next ?? new int[] { 50, 100 };
                List<DouYinVideoInfo> list = null;
                for (int i = 0; i < 3; i++)
                {
                    list = Task.Run(async () => await new SearchVideoByAweme().AppSearchAsync(keyword, videoNum, videoNum * 2)).ConfigureAwait(false) // 关键：不切换回原同步上下文
        .GetAwaiter()
        .GetResult(); // 替代 .Result，异常直接抛出（非 AggregateException）;
                    if (list != null && list.Count > 0)
                    {
                        break;
                    }
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
        /// 抖音热门视频搜索（优化版：异步优先+智能重试+超时控制+日志增强）
        /// </summary>
        /// <param name="keyword">搜索关键词</param>
        /// <returns>抖音视频信息列表</returns>
        public static async Task<List<DouYinVideoInfo>> SearchDouYinHotVideosAsync(string keyword)
        {
            // 1. 配置参数（抽离为变量，便于维护）
            const int maxRetryCount = 1; // 最大重试次数（可移到配置文件）
            var retryDelays = new[] { TimeSpan.FromSeconds(1), TimeSpan.FromSeconds(2), TimeSpan.FromSeconds(3) }; // 递增重试延迟（避免反爬/限流）
            int videoNum = KvHelper.GetIntKvByKey("hotSearch_sync_video_numberMax", 150);
            videoNum = Math.Max(videoNum, 10); // 兜底：避免配置为0或负数导致接口参数无效

            // 2. 参数校验（提前拦截无效输入）
            if (string.IsNullOrWhiteSpace(keyword))
            {
                await FileUtils.LogErrorAsync("搜索关键词不能为空", "抖音搜索视频参数校验失败");
                return new List<DouYinVideoInfo>();
            }

            var searcher = new SearchVideoByAweme();
            List<DouYinVideoInfo> videoList = null;

            // 3. 智能重试逻辑（带延迟+超时）
            for (int retryIndex = 0; retryIndex < maxRetryCount; retryIndex++)
            {
                var currentRetry = retryIndex + 1;
                try
                {
                    await FileUtils.LogAsync($"开始第{currentRetry}次搜索抖音视频，关键词：{keyword}，请求数量：{videoNum}", "抖音搜索视频");

                    // 3.1 异步调用+10秒超时控制（避免无限阻塞）
                    var searchTask = searcher.AppSearchAsync(keyword, 0, videoNum);
                    var timeoutTask = Task.Delay(TimeSpan.FromSeconds(15)); // 超时阈值（可配置）
                    var completedTask = await Task.WhenAny(searchTask, timeoutTask);

                    if (completedTask == timeoutTask)
                    {
                        // 3.2 超时处理
                        await FileUtils.LogErrorAsync($"第{currentRetry}次搜索抖音视频超时（10秒），关键词：{keyword}", "抖音搜索视频超时");
                        continue; // 进入下一次重试
                    }

                    // 3.3 正常获取结果（已确保searchTask完成）
                    videoList = await searchTask;

                    // 3.4 结果校验
                    if (videoList != null && videoList.Count > 0)
                    {
                        await FileUtils.LogAsync($"第{currentRetry}次搜索成功，关键词：{keyword}，获取视频数量：{videoList.Count}", "抖音搜索视频成功");
                        break; // 有数据则退出重试
                    }
                    var param = new ReportFailureVo()
                    {
                        accountId = Convert.ToInt64(SearchVideoByAweme._emailAccount?.accountId),
                        failureReason = "邮箱查不到数据",
                        email = SearchVideoByAweme._emailAccount?.email,
                        errorMessage = "使用此邮箱查不到数据！",
                        failureType = 5 //其他
                    };
                    HotSearchApi.HotSearchReportFailure(param);
                    await FileUtils.LogErrorAsync($"{SearchVideoByAweme._emailAccount?.email},使用此邮箱查不到数据！", "爆款");
                    // 3.5 无数据处理（未超时但无结果）
                    await FileUtils.LogErrorAsync($"第{currentRetry}次搜索未获取到数据，关键词：{keyword}", "抖音搜索视频无结果");
                }
                catch (HttpRequestException ex)
                {
                    // 3.6 网络/接口类异常（重试友好）
                    await FileUtils.LogErrorAsync($"第{currentRetry}次搜索抖音视频失败（网络/接口错误）：{ex.Message}，关键词：{keyword}", "抖音搜索视频重试异常");
                }
                catch (Exception ex)
                {
                    // 3.7 致命异常（如参数错误、序列化失败，无需重试）
                    await FileUtils.LogErrorAsync($"第{currentRetry}次搜索抖音视频发生致命错误，关键词：{keyword}，Message：{ex.Message}，StackTrace：{ex.StackTrace}", "抖音搜索视频致命异常");
                    break; // 终止重试，避免无效循环
                }

                // 3.8 重试延迟（最后一次重试后不延迟）
                if (retryIndex < maxRetryCount - 1)
                {
                    var delay = retryDelays[retryIndex];
                    await FileUtils.LogErrorAsync($"第{currentRetry}次搜索失败，将在{delay.TotalSeconds}秒后进行第{currentRetry + 1}次重试", "抖音搜索视频重试延迟");
                    await Task.Delay(delay);
                }
            }

            // 4. 最终结果校验
            if (videoList == null || videoList.Count == 0)
            {
                await FileUtils.LogErrorAsync($"关键词：{keyword}，连续{maxRetryCount}次搜索抖音视频均未获取到数据", "抖音搜索视频最终失败");
                return new List<DouYinVideoInfo>(); // 返回空列表而非null，避免调用方空指针
            }

            return videoList;
        }
        public SearchVideoByAweme()
        {
            var cookieContainer = new CookieContainer();
            var clientOptions = new RestClientOptions
            {
                CookieContainer = cookieContainer,
                FollowRedirects = true // 允许重定向，捕获重定向中的Cookie
            };
            // 创建RestClient实例
            _restClient = new RestClient(clientOptions)
            {

            };

        }

        private RestClient getCreateProxyRestClient()
        {
            ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(false, 0);
            FileUtils.log(JsonConvert.SerializeObject(proxyIpVo), "获取代理");
            
            // 添加 null 检查
            if (proxyIpVo == null || string.IsNullOrEmpty(proxyIpVo.ip) || string.IsNullOrEmpty(proxyIpVo.port))
            {
                FileUtils.log("获取代理IP失败，使用无代理模式", "获取代理");
                // 创建无代理的 RestClient
                var noProxyCookieContainer = new CookieContainer();
                var noProxyClientOptions = new RestClientOptions
                {
                    CookieContainer = noProxyCookieContainer,
                    FollowRedirects = true
                };
                return new RestClient(noProxyClientOptions);
            }
            
            // 创建代理
            WebProxy proxy = new WebProxy($"{proxyIpVo.ip}:{proxyIpVo.port}");
            // 如果代理需要认证：
            if (!string.IsNullOrEmpty(proxyIpVo.proxyUsername) && !string.IsNullOrEmpty(proxyIpVo.proxyPassword))
            {
                proxy.Credentials = new NetworkCredential(proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);
            }

            var cookieContainer = new CookieContainer();
            var clientOptions = new RestClientOptions
            {
                Proxy = proxy,
                CookieContainer = cookieContainer,
                FollowRedirects = true // 允许重定向，捕获重定向中的Cookie
            };

            // 创建RestClient实例
            RestClient res = new RestClient(clientOptions)
            {

            };

            return res;
        }

        private string ConvertTimestamp(long? timestamp)
        {
            if (timestamp.HasValue)
            {
                // 处理13位时间戳（毫秒）
                long unixTimestamp = timestamp.Value;
                if (unixTimestamp > 1000000000000) // 13位时间戳
                {
                    unixTimestamp /= 1000;
                }

                return DateTimeOffset.FromUnixTimeSeconds(unixTimestamp)
                    .LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss");
            }
            return null;
        }

        private string GetCurrentTime()
        {
            return DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        }

        private string ConvertDuration(long? duration)
        {
            if (!duration.HasValue) return null;

            // 将毫秒转换为秒（取整）
            long totalSeconds = duration.Value / 1000;

            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            if (hours > 0)
            {
                return $"{hours}小时{minutes}分{seconds}秒";
            }
            else if (minutes > 0)
            {
                return $"{minutes}分{seconds}秒";
            }
            else
            {
                return $"{seconds}秒";
            }
        }

        private string ConvertFollowerCount(long? followerCount, string end = "")
        {
            if (!followerCount.HasValue) return null;

            if (followerCount >= 100000000) // 1亿及以上
            {
                return $"{(followerCount.Value / 100000000.0):F1}亿{end}";
            }
            else if (followerCount >= 10000) // 1万及以上，1亿以下
            {
                return $"{(followerCount.Value / 10000.0):F1}万{end}";
            }
            else // 1万以下
            {
                return $"{followerCount}{end}";
            }
        }
      

        public async Task<List<DouYinVideoInfo>> AppSearchAsync(string keyword, int requestCount = 0, int maxCount = 100)
        {
            //sessionid是否过期
            int exTime = KvHelper.GetKvByKey("hot_search_email_expiration_time", 60);
            if (string.IsNullOrEmpty(_sessionid)|| DateTime.Now.AddMinutes(-exTime) > _sessionidTime) //DateTime.Now.AddHours(-1)
            {
                //取账号密码
                HotSearchEmailAccountVo emailAccount = HotSearchApi.GetHotSearchEmailAccount();
                _emailAccount = emailAccount;
                if (emailAccount == null || string.IsNullOrEmpty(emailAccount.email) || string.IsNullOrEmpty(emailAccount.emailPassword))
                {
                    var param = new ReportFailureVo()
                    {
                        accountId = Convert.ToInt64(emailAccount.accountId),
                        failureReason = "取邮箱账号失败！",
                        email = emailAccount.email,
                        errorMessage = "取邮箱账号失败！",
                        failureType = 5 //其他
                    };
                    await HotSearchApi.HotSearchReportFailure(param);
                    FileUtils.LogError($"{emailAccount.email},取邮箱密码解密失败！", "爆款");
                    return null;
                }
                else
                {
                    // 解密（使用相同email）
                    string decryptedPassword = AesCbcUtils.DecryptEmailPassword(emailAccount.emailPassword, emailAccount.email);
                    if (string.IsNullOrWhiteSpace(decryptedPassword))
                    {
                        var param = new ReportFailureVo()
                        {
                            accountId = Convert.ToInt64(emailAccount.accountId),
                            failureReason = "邮箱密码解密错误",
                            email = emailAccount.email,
                            errorMessage = "邮箱账号登录错误",
                            failureType = 1 //密码错误
                        };
                        await HotSearchApi.HotSearchReportFailure(param);
                        FileUtils.LogError($"{emailAccount.email},取邮箱密码解密失败！", "爆款");
                        return null;
                    }
                    //取sessionid，若异常回调异常
                    var(sessionid, reason, message, type)  = await new DouYinDeveloperOpen().AccountLoginByEmail(emailAccount.email, decryptedPassword);
                    if (string.IsNullOrEmpty(sessionid))
                    {
                        var param = new ReportFailureVo()
                        {
                            accountId =Convert.ToInt64(emailAccount.accountId),
                            failureReason = string.IsNullOrEmpty(reason) ? "邮箱登录错误":reason, 
                            email = emailAccount.email,
                            errorMessage = string.IsNullOrEmpty(message)? "邮箱账号登录错误" : message,
                            failureType = type
                        };
                        if(!(param.errorMessage.Contains("发送请求时出错") || param.errorMessage.Contains("已取消一个任务")))
                        await HotSearchApi.HotSearchReportFailure(param);
                        FileUtils.LogError($"{emailAccount.email}邮箱登录错误!"+ reason+ message,"爆款");
                        return null;
                    }
                    else
                    {
                        _sessionid = sessionid;
                        _sessionidTime = DateTime.Now;
                        FileUtils.log($"{emailAccount.email}邮箱登录成功!" + reason + message, "爆款");
                    }
                }


            }

            var cookies = new Dictionary<string, string>
        {
            { "sessionid", _sessionid }
        };

            var baseUrl = "https://aweme.snssdk.com/aweme/v1/search/item/";

            var cdid = Guid.NewGuid().ToString();
            var openudid = GenerateOpenUdid();
            var deviceId = GenerateId();
            var iid = GenerateId();

            int douyinVersionCode = 10; // 版本选择：33---》每页7条数据，有红V认证  10---》每页25条数据，没有红V认证

            var headers = new Dictionary<string, string>
        {
            { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" }, //ttnet okhttp/3.10.0.2
            { "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8" },
            { "sdk-version", "1" }
        };

            // 去重视频列表
            var awemeIdSet = new HashSet<string>();
            var diggCountList = new List<long>();
            List<DouYinVideoInfo> douYinVideoInfoList = new List<DouYinVideoInfo>();

            int currentPage = 1;
            string searchId = "";
            bool shouldExit = false;

            while (!shouldExit && requestCount<20)
            {
                requestCount++;
                //await Task.Delay(TimeSpan.FromSeconds(new Random().Next(3, 5)));

                var rticket = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;
                var ts = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;

                var parameters = new Dictionary<string, string>
            {
                { "manifest_version_code", "100001" },
                { "_rticket", rticket.ToString() },
                { "app_type", "normal" },
                { "iid", iid },
                { "channel", "huawei" },
                { "publish_time", "182" },//排序方式：0= 综合排序，1= 最多点赞，2= 最新发布
                { "sort_type", "2" }, //排序方式：0= 综合排序，1= 最多点赞，2= 最新发布
                { "device_type", "22127RK46C" },
                { "language", "zh" },
                { "resolution", "900*1600" },
                { "openudid", openudid },
                { "update_version_code", "10009900" },
                { "cdid", cdid },
                { "os_api", "28" },
                { "dpi", "240" },
                { "ac", "wifi" },
                { "cookie_enabled", "true" },
                { "device_id", deviceId },
                { "mcc_mnc", "46000" },
                { "os_version", "9" },
                { "version_code", "100000" },
                { "app_name", "douyin_lite" },
                { "version_name", $"{douyinVersionCode}.0.0" },
                { "device_brand", "Redmi" },
                { "ssmix", "a" },
                { "device_platform", "android" },
                { "aid", "2329" },
                { "ts", ts.ToString() }
            };

                var paramsStr = string.Join("&", parameters.Select(kv => $"{kv.Key}={HttpUtility.UrlEncode(kv.Value)}"));
                var url = $"{baseUrl}?{paramsStr}";

                var xgorgon = new XGorgon();
                var signature = xgorgon.Calculate(paramsStr, headers);

                int count = douyinVersionCode == 10 ? 25 : 10;
                var data = new Dictionary<string, string>
            {
                { "keyword", keyword },
                { "offset", ((currentPage - 1) * count).ToString() },
                { "count", count.ToString() },
                { "source", "video_search" },
                { "search_source", "switch_tab" },
                { "is_pull_refresh", "1" },
                { "hot_search", "0" },
                { "search_id", searchId },
                { "query_correct_type", "1" },
                { "is_filter_search", "0" },
                { "enter_from", "homepage_hot" }
            };

                try
                {
                    // 创建RestRequest实例
                    var request = new RestRequest(url);
                    request.Method = Method.Post;


                    // 设置请求头
                    request.AddHeader("User-Agent", "com.ss.android.ugc.aweme/17.4.0 (Linux; U; Android 11; zh_CN; Pixel 5; Build/RP1A.200720.011; Cronet/58.0.2991.0)");
                    request.AddHeader("Content-Type", "application/x-www-form-urlencoded");
                    request.AddHeader("X-Gorgon", signature["X-Gorgon"]);
                    request.AddHeader("X-Khronos", signature["X-Khronos"]);
                    request.AddHeader("X-SS-REQ-TICKET", rticket.ToString());
                    //request.AddHeader("X-SS-QUERIES", "version_code=17.4.0&language=zh_CN&manifest_version_code=17400&app_name=musical_ly&app_version=17.4.0&os_language=zh-CN&os_api=30&os_version=11&device_type=Pixel%205&device_brand=Google&resolution=1080*2340&dpi=440&update_version_code=17400&_rticket=1620000000000&ac=wifi&is_pad=0&first_launch=0&device_id=1234567890&install_id=1234567890");
                    foreach (var cookie in cookies)
                    {
                        request.AddHeader("Cookie", $"{cookie.Key}={cookie.Value}");
                    }
                    // 添加表单参数
                    foreach (var d in data)
                    {
                        request.AddParameter(d.Key, d.Value, ParameterType.GetOrPost);
                    }
                   
                    // 发送请求

                    //var response = _restClient.Execute(request);
                    var response = getCreateProxyRestClient().Execute(request);
                    string responseContent = string.Empty;

                    // 处理响应
                    if (response.IsSuccessful)
                    {
                        responseContent = response.Content;
                    }
                    else
                    {
                        shouldExit = true;
                        break;
                    }

                    var jsonDoc = JsonDocument.Parse(responseContent);
                    var root = jsonDoc.RootElement;

                    // 检查状态码
                    if (root.TryGetProperty("status_code", out var statusCode))
                    {
                        int code = statusCode.ValueKind == JsonValueKind.Number ? statusCode.GetInt32() : 0;
                        if (code != 0)
                        {
                            FileUtils.log($"API返回错误状态码: {code}");
                        }
                    }

                    // 处理search_id
                    if (currentPage == 1)
                    {
                        searchId = JMESPath.Search<string>(root, "extra.logid");
                    }

                    // 安全地获取aweme_list
                    JsonElement awemeList = default;
                    bool hasData = false;

                    if (douyinVersionCode == 10)
                    {
                        if (root.TryGetProperty("aweme_list", out var awemeListProp))
                        {
                            awemeList = awemeListProp;
                            hasData = true;
                        }
                    }
                    else
                    {
                        if (root.TryGetProperty("data", out var dataProp))
                        {
                            awemeList = dataProp;
                            hasData = true;
                        }
                    }

                    if (hasData && awemeList.ValueKind == JsonValueKind.Array && awemeList.GetArrayLength() > 0)
                    {
                        foreach (var item in awemeList.EnumerateArray())
                        {
                            try
                            {
                                JsonElement awemeInfo;
                                if (douyinVersionCode == 10)
                                {
                                    awemeInfo = item;
                                }
                                else
                                {
                                    if (item.TryGetProperty("aweme_info", out var awemeInfoProp))
                                        awemeInfo = awemeInfoProp;
                                    else
                                        continue;
                                }

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
                    else
                    {
                        shouldExit = true;
                    }

                    // 检查是否有更多数据
                    bool hasMore = root.TryGetProperty("has_more", out var hasMoreProp) &&
                                  (hasMoreProp.ValueKind == JsonValueKind.True ||
                                   (hasMoreProp.ValueKind == JsonValueKind.Number && hasMoreProp.GetInt32() == 1));

                    if (hasMore && !shouldExit)
                    {

                    }
                    else
                    {
                        shouldExit = true;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"爆款中请求错误: {ex.Message}");
                    shouldExit = true;
                }

                currentPage++;
            }

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
        private string GenerateId()
        {
            var random = new Random();
            byte[] buffer = new byte[8];
            random.NextBytes(buffer);
            long num = BitConverter.ToInt64(buffer, 0);
            num = Math.Abs(num) % 900000000000000 + 100000000000000;
            return num.ToString();
        }

        private string GenerateOpenUdid()
        {
            var random = new Random();
            var bytes = new byte[8];
            random.NextBytes(bytes);
            return string.Join("", bytes.Select(b => b.ToString("x2")));
        }
    }
    public class XGorgon
    {
        private readonly int length = 20;
        private readonly byte[] hexStr = { 30, 64, 224, 217, 147, 69, 0, 180 };

        private byte[] Encryption()
        {
            byte[] hexZu = Enumerable.Range(0, 256).Select(i => (byte)i).ToArray();
            object tmp = null; // 用 object 模拟 Python 的 '' 或 None

            for (int i = 0; i < 256; i++)
            {
                int A;
                if (i == 0)
                {
                    A = 0;
                }
                else if (tmp != null)
                {
                    A = (int)tmp;
                }
                else
                {
                    A = hexZu[i - 1];
                }

                byte B = hexStr[i % 8];

                // 特殊处理 A == 85
                if (A == 85 && i != 1 && (tmp == null || (int)tmp != 85))
                {
                    A = 0;
                }
                int C = A + i + B;
                while (C >= 256) C -= 256;

                // 更新 tmp
                tmp = C < i ? (object)C : null;

                hexZu[i] = hexZu[C];
            }

            return hexZu;
        }

        private byte[] Initialize(byte[] inputData, byte[] hexZu)
        {
            var tmpAdd = new List<int>();
            byte[] tmpHex = (byte[])hexZu.Clone(); // 深拷贝

            for (int i = 0; i < length; i++)
            {
                byte A = inputData[i];
                int B = tmpAdd.Count > 0 ? tmpAdd[tmpAdd.Count - 1] : 0;

                int C = hexZu[i + 1] + B; // 注意：这里用的是原始 hexZu[i+1]，不是 tmpHex
                while (C >= 256) C -= 256;

                tmpAdd.Add(C);
                byte D = tmpHex[C];
                tmpHex[i + 1] = D; // 修改 tmpHex

                int E = D + D;
                while (E >= 256) E -= 256;

                byte F = tmpHex[E]; // 使用修改后的 tmpHex
                inputData[i] = (byte)(A ^ F);
            }

            return inputData;
        }


        private byte Reverse(byte num)
        {
            string hex = num.ToString("x2");
            // 交换两个字符：ab -> ba，将 char 转为 string 后拼接
            string reversed = hex[1].ToString() + hex[0].ToString();
            return Convert.ToByte(reversed, 16);
        }

        private byte RBIT(byte num)
        {
            string bin = Convert.ToString(num, 2).PadLeft(8, '0');
            char[] chars = bin.ToCharArray();
            Array.Reverse(chars);
            string reversed = new string(chars);
            return Convert.ToByte(reversed, 2);
        }

        private byte[] Handle(byte[] inputData)
        {
            byte[] data = (byte[])inputData.Clone();

            for (int i = 0; i < length; i++)
            {
                byte A = data[i];
                byte B = Reverse(A);
                byte C = data[(i + 1) % length];
                byte D = (byte)(B ^ C);
                byte E = RBIT(D);
                byte F = (byte)(E ^ length);
                uint G = (uint)(~F); // ✅ 用 uint 自动处理补码

                byte H = (byte)(G & 0xFF);
                data[i] = H;
            }

            return data;
        }

        private string Hex2String(byte num)
        {
            return num.ToString("x2");
        }

        private string Main(byte[] gorgon)
        {
            byte[] processed = Handle(Initialize(gorgon, Encryption()));
            string result = string.Concat(processed.Select(Hex2String));

            string prefix = $"0401{Hex2String(hexStr[7])}{Hex2String(hexStr[3])}{Hex2String(hexStr[1])}{Hex2String(hexStr[6])}";
            return prefix + result;
        }

        public Dictionary<string, string> Calculate(string paramsStr, Dictionary<string, string> headers = null)
        {
            headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            byte[] gorgon = new byte[20];

            long timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;
            string khronosHex = timestamp.ToString("x");

            // 补齐为偶数长度
            if (khronosHex.Length % 2 != 0)
                khronosHex = "0" + khronosHex;

            // 1. URL MD5 前4字节
            byte[] urlMd5 = MD5.Create().ComputeHash(Encoding.UTF8.GetBytes(paramsStr));
            Array.Copy(urlMd5, 0, gorgon, 0, 4);

            // 2. x-ss-stub 前4字节
            if (headers.TryGetValue("x-ss-stub", out string dataMd5) && dataMd5.Length >= 8)
            {
                byte[] dataBytes = StringToByteArray(dataMd5.Substring(0, 8));
                Array.Copy(dataBytes, 0, gorgon, 4, 4);
            }
            else
            {
                Array.Clear(gorgon, 4, 4);
            }

            // 3. cookie MD5 前4字节
            if (headers.TryGetValue("cookie", out string cookie))
            {
                byte[] cookieMd5 = MD5.Create().ComputeHash(Encoding.UTF8.GetBytes(cookie));
                Array.Copy(cookieMd5, 0, gorgon, 8, 4);
            }
            else
            {
                Array.Clear(gorgon, 8, 4);
            }

            // 4. 中间4字节填0
            Array.Clear(gorgon, 12, 4);

            // 5. 时间戳：从左到右填充4字节（注意顺序）
            byte[] khronosBytes = StringToByteArray(khronosHex.PadLeft(8, '0')); // 补到8位hex（4字节）
            if (khronosBytes.Length >= 4)
            {
                Array.Copy(khronosBytes, khronosBytes.Length - 4, gorgon, 16, 4); // 低4字节放最后
            }
            else
            {
                Array.Copy(khronosBytes, 0, gorgon, 20 - khronosBytes.Length, khronosBytes.Length);
            }

            string xGorgon = Main(gorgon);
            string xKhronos = timestamp.ToString();

            return new Dictionary<string, string>
        {
            { "X-Gorgon", xGorgon },
            { "X-Khronos", xKhronos }
        };
        }

        private static byte[] StringToByteArray(string hex)
        {
            return Enumerable.Range(0, hex.Length / 2)
                .Select(i => Convert.ToByte(hex.Substring(i * 2, 2), 16))
                .ToArray();
        }

        // public static void Main(string[] args)
        // {
        //     string paramsStr = "manifest_version_code=100001&_rticket=1754537817617&app_type=normal&iid=174403850931673&channel=huawei&device_type=22127RK46C&language=zh&resolution=900*1600&openudid=d9d028ddfdd75301&update_version_code=10009900&cdid=85f877fb-5056-4cc1-a6f7-51671e84bde7&os_api=28&dpi=240&ac=wifi&device_id=120557179413229&mcc_mnc=46000&os_version=9&version_code=100000&app_name=douyin_lite&version_name=10.0.0&device_brand=Redmi&ssmix=a&device_platform=android&aid=2329&ts=1754537817";

        //     var xgorgon = new XGorgon();
        //     var result = xgorgon.Calculate(paramsStr);

        //     FileUtils.log("生成的签名信息:");
        //     FileUtils.log($"X-Gorgon: {result["X-Gorgon"]}");
        //     FileUtils.log($"X-Khronos: {result["X-Khronos"]}");
        // }
    }
    // JMESPath实现（改进版）
    public static class JMESPath
    {
        public static T Search<T>(JsonElement element, string path)
        {
            try
            {
                if (string.IsNullOrEmpty(path)) return default(T);

                var parts = path.Split('.');
                JsonElement current = element;

                foreach (var part in parts)
                {
                    if (part.Contains("["))
                    {
                        // 处理数组索引
                        var arrayPart = part.Split('[');
                        var propertyName = arrayPart[0];
                        var indexPart = arrayPart[1].TrimEnd(']');

                        if (!string.IsNullOrEmpty(propertyName) && current.ValueKind == JsonValueKind.Object)
                        {
                            if (current.TryGetProperty(propertyName, out var temp))
                                current = temp;
                            else
                                return default(T);
                        }

                        if (int.TryParse(indexPart, out int index) && current.ValueKind == JsonValueKind.Array)
                        {
                            var array = current.EnumerateArray().ToArray();
                            if (index >= 0 && index < array.Length)
                                current = array[index];
                            else
                                return default(T);
                        }
                    }
                    else
                    {
                        if (current.ValueKind == JsonValueKind.Object && current.TryGetProperty(part, out var property))
                        {
                            current = property;
                        }
                        else
                        {
                            return default(T);
                        }
                    }
                }

                return ConvertValue<T>(current);
            }
            catch
            {
                return default(T);
            }
        }

        private static T ConvertValue<T>(JsonElement element)
        {
            try
            {
                if (typeof(T) == typeof(string))
                {
                    return (T)(object)(element.ValueKind == JsonValueKind.String ? element.GetString() :
                                      element.ValueKind == JsonValueKind.Number ? element.GetRawText() : null);
                }
                else if (typeof(T) == typeof(int?) || typeof(T) == typeof(int))
                {
                    return (T)(object)(element.ValueKind == JsonValueKind.Number ? element.GetInt32() : 0);
                }
                else if (typeof(T) == typeof(long?) || typeof(T) == typeof(long))
                {
                    return (T)(object)(element.ValueKind == JsonValueKind.Number ? element.GetInt64() : 0L);
                }
                else if (typeof(T) == typeof(bool?) || typeof(T) == typeof(bool))
                {
                    return (T)(object)(element.ValueKind == JsonValueKind.True ? true :
                                      element.ValueKind == JsonValueKind.False ? false : false);
                }

                return default(T);
            }
            catch
            {
                return default(T);
            }
        }
    }
}


