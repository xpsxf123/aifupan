using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor.Entity;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.user;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using CefSharp.DevTools.IO;
using ReviewAnalysis.Asr;
using System.Text.RegularExpressions;
using Swan.Formatters;
using System.Web.UI.WebControls;
using Newtonsoft.Json.Linq;
using System.Security.Cryptography;

namespace ReviewAnalysis.Utils.kuaishou
{
    public class KuaiShouRequestClient
    {
        private static readonly Lazy<KuaiShouRequestClient> _instance = new Lazy<KuaiShouRequestClient>(() => new KuaiShouRequestClient());
        private static bool switchUrl = false;
        private static string getAnchorOnlineUrl1 = "https://live.douyin.com/webcast/distribution/check_user_live_status";
        private static string getAnchorOnlineUrl2 = "https://webcast5-normal-m-hj.amemv.com/webcast/distribution/check_user_live_status";

        /// <summary>
        /// 请求实例
        /// </summary>
        public static KuaiShouRequestClient Instance => _instance.Value;

        private HttpClient _httpClient;
        private HttpClientHandler _httpClientHandler;
        private CookieContainer _cookieContainer;

        private readonly object _httpClientLock = new object();
        private readonly object _proxyIpLock = new object();

        private List<string> userAgentList = new List<string>() {
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.4936.1160 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.5813.1294 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.5431.1706 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.4 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/27.0 Chrome/125.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/135.0.7049.83 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.1470.1951 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.4102.1129 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/135.0.7049.53 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2893.1844 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/135.0.7049.83 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.7100.1187 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        };
        private volatile int currentUserAgentIndex = 0;

        // 代理相关属性
        /// <summary>
        /// 当前代理IP信息
        /// </summary>
        private volatile ProxyIpVo _currentProxyIp;

        /// <summary>
        /// 是否正在使用代理
        /// </summary>
        private volatile bool _isUseProxy;

        /// <summary>
        /// 使用代理的过期时间，超过这个时间则切回正常IP
        /// </summary>
        private long _proxyExpireTime = 0;

        /// <summary>
        /// 允许更换的IP数量
        /// </summary>
        private volatile int _forceUpdateNum = 10;

        /// <summary>
        /// 私有化构造器
        /// </summary>
        private KuaiShouRequestClient()
        {
            InitializeClient(useProxy: false);
        }

        /// <summary>
        /// 判断当前的快手号是快手网页id还是用户自定义ID
        /// </summary>
        /// <param name="number">快手网页id</param>
        /// <returns> -1：请求失败 0：网页id  1：用户自定义id</returns>
        public async Task<int> judgeIsWebOrCustom(string number)
        {
            try
            {
                string did = await KuaiShouCaptchaUtils.getDidCaptcha();
                if (string.IsNullOrEmpty(did))
                {
                    return -1;
                }

                setDid(did, "https://www.kuaishou.com");

                _httpClient.DefaultRequestHeaders.Clear();
                _httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");
                _httpClient.DefaultRequestHeaders.Add("Accept", "application/json");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                _httpClient.DefaultRequestHeaders.Add("Origin", "https://www.kuaishou.com");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Site", "same-origin");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Mode", "cors");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Dest", "empty");
                _httpClient.DefaultRequestHeaders.Add("Referer", $"https://www.kuaishou.com/profile/{number}");
                _httpClient.DefaultRequestHeaders.Add("Accept-Language", "zh-CN,zh;q=0.9");
                


                // URL 和参数
                string url = "https://www.kuaishou.com/rest/v/profile/user";

                // 请求数据
                var requestData = new
                {
                    user_id = number
                };
                string json = JsonConvert.SerializeObject(requestData);

                using (StringContent content = new StringContent(json, Encoding.UTF8, "application/json"))
                {
                    // 发送 POST 请求
                    using (HttpResponseMessage response = await _httpClient.PostAsync(url, content))
                    {
                        var responseText = await response.Content.ReadAsStringAsync();

                        JObject jo = JObject.Parse(responseText);

                        bool? online = jo?["userProfile"]?["livingInfo"]?["living"]?.Value<bool>();
                        if (online != null)
                        {
                            // 当前是快手网页id
                            return 0;
                        }

                        int? resultStatus = jo?["result"]?.Value<int>();
                        if(resultStatus != null && resultStatus == 21)
                        {
                            // 当前是快手用户自定义id
                            return 1;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"判断当前的快手号是快手网页id还是用户自定义ID==={number}");
            }


            return -1;
        }

        /// <summary>
        /// 根据快手webId获取开播状态 0：检测失败 1：正在直播 2：未开播
        /// </summary>
        /// <param name="webId">快手网页id</param>
        /// <returns></returns>
        public async Task<int> getOnlineStatusByWebId(string webId)
        {
            try
            {
                string did = await KuaiShouCaptchaUtils.getDidCaptcha();
                if (string.IsNullOrEmpty(did))
                {
                    return 0;
                }

                setDid(did, "https://www.kuaishou.com");

                _httpClient.DefaultRequestHeaders.Clear();
                _httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");
                _httpClient.DefaultRequestHeaders.Add("Accept", "application/json");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                _httpClient.DefaultRequestHeaders.Add("Origin", "https://www.kuaishou.com");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Site", "same-origin");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Mode", "cors");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Dest", "empty");
                _httpClient.DefaultRequestHeaders.Add("Referer", $"https://www.kuaishou.com/profile/{webId}");
                _httpClient.DefaultRequestHeaders.Add("Accept-Language", "zh-CN,zh;q=0.9");
                

                // URL 和参数
                string url = "https://www.kuaishou.com/rest/v/profile/user";

                // 请求数据
                var requestData = new
                {
                    user_id = webId
                };
                string json = JsonConvert.SerializeObject(requestData);

                using (StringContent content = new StringContent(json, Encoding.UTF8, "application/json"))
                {
                    // 发送 POST 请求
                    using (HttpResponseMessage response = await _httpClient.PostAsync(url, content))
                    {
                        var responseText = await response.Content.ReadAsStringAsync();

                        JObject jo = JObject.Parse(responseText);

                        int? resultStatus = jo?["result"]?.Value<int>();
                        if (resultStatus != null && resultStatus == 2)
                        {
                            // 被封控，清除did
                            KuaiShouCaptchaUtils.clearDid();
                        }

                        bool? online = jo?["userProfile"]?["livingInfo"]?["living"]?.Value<bool>();
                        if(online == null)
                        {
                            return 0;
                        }
                        else
                        {
                            return (bool)online ? 1 : 2;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据快手webId获取开播状态==={webId}");
            }


            return 0;
        }

        /// <summary>
        /// 根据快手webId 或 自定义id 获取开播状态 0：检测失败 1：正在直播 2：未开播
        /// </summary>
        /// <param name="webIdOrCustomId">快手网页id或自定义id</param>
        /// <returns></returns>
        public async Task<int> getOnlineStatusByWebIdOrCustomId(string webIdOrCustomId, bool retryed = false)
        {
            try
            {

                setDid("", "https://live.kuaishou.com");

                using (var request = new HttpRequestMessage())
                {
                    request.Method = HttpMethod.Get;
                    request.Headers.Add("User-Agent", getUserAgent());
                    request.Headers.Add("Accept", "application/json, text/plain, */*");
                    request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
                    request.Headers.Add("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"");
                    request.Headers.Add("sec-ch-ua-mobile", "?0");
                    request.Headers.Add("Sec-Fetch-Site", "same-origin");
                    request.Headers.Add("Sec-Fetch-Mode", "cors");
                    request.Headers.Add("Sec-Fetch-Dest", "empty");
                    request.Headers.Add("Referer", $"https://live.kuaishou.com/profile/{webIdOrCustomId}");
                    request.Headers.Add("Accept-Language", "zh-CN,zh;q=0.9");

                    // URL 和参数
                    string url = $"https://live.kuaishou.com/live_api/baseuser/userinfo/byid?caver=2&principalId={WebUtility.UrlEncode(webIdOrCustomId)}";
                    request.RequestUri = new Uri(url);


                    // 发送 GET 请求
                    using (HttpResponseMessage response = await _httpClient.SendAsync(request))
                    {
                        var responseText = await response.Content.ReadAsStringAsync();

                        JObject jo = JObject.Parse(responseText);

                        int? resultStatus = jo?["data"]?["result"]?.Value<int>();
                        if (resultStatus != null && resultStatus == 2)
                        {
                            if (!retryed)
                            {
                                // 被封控，换一个User-Agent
                                return await getOnlineStatusByWebIdOrCustomId(webIdOrCustomId, true);
                            }
                        }

                        bool? living = jo?["data"]?["userInfo"]?["living"]?.Value<bool>();

                        if (living != null)
                        {
                            return (bool)living ? 1 : 2;
                        }


                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据快手webId 或 自定义id 获取开播状态==={webIdOrCustomId}");
            }


            return 0;
        }


        /// <summary>
        /// 根据快手webId或 自定义id 获取 快手原始用户id
        /// </summary>
        /// <param name="kuaishouId">快手webId或 自定义id</param>
        /// <returns></returns>
        public async Task<string> getOriginUserIdByWebIdOrCustomId(string kuaishouId, bool retryed = false)
        {
            try
            {
                //string did = await KuaiShouCaptchaUtils.getDid();
                //if (string.IsNullOrEmpty(did))
                //{
                //    return "";
                //}

                //setDid(did);

                setDid("", "https://live.kuaishou.com");

                using (var request = new HttpRequestMessage())
                {
                    request.Method = HttpMethod.Get;
                    request.Headers.Add("User-Agent", getUserAgent());
                    request.Headers.Add("Accept", "application/json, text/plain, */*");
                    request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
                    request.Headers.Add("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Microsoft Edge\";v=\"139\", \"Chromium\";v=\"139\"");
                    request.Headers.Add("sec-ch-ua-mobile", "?0");
                    request.Headers.Add("Sec-Fetch-Site", "same-origin");
                    request.Headers.Add("Sec-Fetch-Mode", "cors");
                    request.Headers.Add("Sec-Fetch-Dest", "empty");
                    request.Headers.Add("Referer", $"https://live.kuaishou.com/profile/{kuaishouId}");
                    request.Headers.Add("Accept-Language", "zh-CN,zh;q=0.9");

                    // 拼接 URL 和查询参数
                    string url = $"https://live.kuaishou.com/live_api/baseuser/userinfo/byid?caver=2&principalId={WebUtility.UrlEncode(kuaishouId)}";
                    request.RequestUri = new Uri(url);

                    // 发送 GET 请求
                    using (HttpResponseMessage response = await _httpClient.SendAsync(request))
                    {
                        var responseText = await response.Content.ReadAsStringAsync();

                        JObject jo = JObject.Parse(responseText);

                        int? resultStatus = jo?["data"]?["result"]?.Value<int>();
                        if (resultStatus != null && resultStatus == 2)
                        {
                            if (!retryed)
                            {
                                // 被封控，换一个User-Agent
                                return await getOriginUserIdByWebIdOrCustomId(kuaishouId, true);
                            }
                        }

                        string originUserId = jo?["data"]?["userInfo"]?["originUserId"]?.Value<string>();

                        if (!string.IsNullOrEmpty(originUserId))
                        {
                            return originUserId;
                        }

                    }

                }
                
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据快手webId或修改后的快手号 获取 快手原始用户id==={kuaishouId}");
            }
            

            return "";
        }

        /// <summary>
        /// 获取UserAgent
        /// </summary>
        /// <returns></returns>
        private string getUserAgent()
        {
            string userAgent = userAgentList[currentUserAgentIndex];
            currentUserAgentIndex++;
            if (currentUserAgentIndex >= userAgentList.Count)
            {
                currentUserAgentIndex = 0;
            }
            return userAgent;
        }


        /// <summary>
        /// 根据快手原始用户id获取webId
        /// </summary>
        /// <param name="originUserId"></param>
        /// <returns></returns>
        public async Task<string> getWebIdByOriginUserId(string originUserId)
        {

            try
            {
                using (var request = new HttpRequestMessage())
                {
                    request.Method = HttpMethod.Post;
                    request.Headers.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");
                    request.Headers.Add("xweb_xhr", "1");
                    request.Headers.Add("Sec-Fetch-Site", "cross-site");
                    request.Headers.Add("Sec-Fetch-Mode", "cors");
                    request.Headers.Add("Sec-Fetch-Dest", "empty");
                    request.Headers.Add("Referer", "https://servicewechat.com/wx79a83b1a1e8a7978/692/page-frame.html");
                    request.Headers.Add("Accept-Language", "zh-CN,zh;q=0.9");

                    // URL 和参数
                    string url = "https://wxmini-api.uyouqu.com/rest/zt/share/w/batch/web?kpn=KUAISHOU";
                    request.RequestUri = new Uri(url);

                    // 请求数据
                    var requestData = new
                    {
                        kpn = "KUAISHOU",
                        kpf = "OUTSIDE_ANDROID_H5",
                        subBiz = "PROFILE",
                        isSmallApp = true,
                        shareWebModels = new[]
                        {
                            new
                            {
                                shareObjectId = Convert.ToInt64(originUserId),
                                subBiz = "PROFILE"
                            }
                        }
                    };

                    string json = JsonConvert.SerializeObject(requestData);
                    request.Content = new StringContent(json, Encoding.UTF8, "application/json");

                    // 发送 POST 请求
                    using (HttpResponseMessage response = await _httpClient.SendAsync(request))
                    {
                        var responseText = await response.Content.ReadAsStringAsync();

                        // 提取 ks_user_id
                        var match = Regex.Match(responseText, @"COVER_(.*?)_");
                        if (match.Success)
                        {
                            return match.Groups[1].Value;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据快手原始用户id获取webId发生异常==={originUserId}");
            }

            return "";
        }





        /// <summary>
        /// 初始化/重置 HttpClient
        /// </summary>
        /// <param name="useProxy">是否使用代理</param>
        /// <param name="proxyIp">代理的IP</param>
        /// <param name="proxyPort">代理的端口</param>
        /// <param name="username">代理验证账号</param>
        /// <param name="password">代理验证密码</param>
        private void InitializeClient(bool useProxy, string proxyIp = "", string proxyPort = "", string username = "", string password = "")
        {
            lock (_httpClientLock)
            {
                // 清理旧实例
                _httpClient?.Dispose();
                _httpClientHandler?.Dispose();

                // 创建新的 cookie
                _cookieContainer = new CookieContainer();
                // 创建新的 Handler
                _httpClientHandler = new HttpClientHandler
                {
                    CookieContainer = _cookieContainer,
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate
                };

                // 设置代理
                if (useProxy && !string.IsNullOrEmpty(proxyIp) && !string.IsNullOrEmpty(proxyPort))
                {
                    // 设置代理
                    WebProxy webProxy = new WebProxy($"{proxyIp}:{proxyPort}");
                    if (!string.IsNullOrEmpty(username) && !string.IsNullOrEmpty(password))
                    {
                        webProxy.Credentials = new NetworkCredential(username, password);
                    }

                    _httpClientHandler.Proxy = webProxy;
                    _httpClientHandler.UseProxy = true;
                }
                else
                {
                    // 去掉代理
                    _httpClientHandler.Proxy = null;
                    _httpClientHandler.UseProxy = false;
                }

                // 创建新的 HttpClient
                _httpClient = new HttpClient(_httpClientHandler);
            }
        }
        /// <summary>
        /// 重新设置did cookie
        /// </summary>
        /// <param name="did"></param>
        public void setDid(string did, string url = "https://live.kuaishou.com")
        {
            _cookieContainer.Add(new Uri(url), new Cookie("did", did));
        }












        /// <summary>
        /// 设置/更新代理
        /// </summary>
        /// <param name="proxyIp">代理的IP</param>
        /// <param name="proxyPort">代理的端口</param>
        /// <param name="username">代理验证账号</param>
        /// <param name="password">代理验证密码</param>
        public void SetProxy(string proxyIp = "", string proxyPort = "", string username = "", string password = "")
        {
            bool useProxy = !string.IsNullOrEmpty(proxyIp) && !string.IsNullOrEmpty(proxyPort);
            InitializeClient(useProxy, proxyIp, proxyPort, username, password);
        }
        /// <summary>
        /// 检查并代理是否已失效，失效了设置为直连
        /// </summary>
        private void CheckProxyExpire()
        {
            lock (_proxyIpLock)
            {

                if (!_isUseProxy)
                {
                    return;
                }

                long currentTime = ServerTimeUtils.getCurrentTime();

                // 检查代理周期是否过期
                if (currentTime > _proxyExpireTime)
                {
                    FileUtils.LogRecrd($"快手代理IP已过期", $"当前时间：{currentTime}，过期时间：{_proxyExpireTime}");
                    SetProxy();
                    _isUseProxy = false;
                }
            }
        }
        /// <summary>
        /// 设置代理IP
        /// </summary>
        private void SetProxyIpInternal()
        {
            lock (_proxyIpLock)
            {
                if (_isUseProxy)
                {
                    return;
                }

                ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(true, 0);
                if (proxyIpVo != null)
                {
                    _isUseProxy = true;
                    _currentProxyIp = proxyIpVo;
                    _proxyExpireTime = proxyIpVo.expireTime;

                    // 设置代理
                    SetProxy(proxyIpVo.ip, proxyIpVo.port, proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);

                    FileUtils.LogRecrd($"快手代理IP设置成功", $"IP: {proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}");
                }
                else
                {
                    FileUtils.LogRecrd($"获取快手代理IP失败", $"从服务器获取代理IP返回null");
                }
            }
            
        }






    }
}
