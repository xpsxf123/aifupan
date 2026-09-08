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
using static System.Windows.Forms.VisualStyles.VisualStyleElement.StartPanel;
using System.Net.Http.Headers;
using System.Text.Json;
using System.IO;
using Newtonsoft.Json.Serialization;

namespace ReviewAnalysis.Utils.kuaishou
{
    public class KuaiShouGetStreamClient
    {
        private static readonly Lazy<KuaiShouGetStreamClient> _instance = new Lazy<KuaiShouGetStreamClient>(() => new KuaiShouGetStreamClient());
        private static bool switchUrl = false;
        private static string getAnchorOnlineUrl1 = "https://live.douyin.com/webcast/distribution/check_user_live_status";
        private static string getAnchorOnlineUrl2 = "https://webcast5-normal-m-hj.amemv.com/webcast/distribution/check_user_live_status";

        /// <summary>
        /// 请求实例
        /// </summary>
        public static KuaiShouGetStreamClient Instance => _instance.Value;

        private HttpClient _httpClient;
        private HttpClientHandler _httpClientHandler;
        private CookieContainer _cookieContainer;

        private readonly object _httpClientLock = new object();
        private readonly object _proxyIpLock = new object();

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
        private KuaiShouGetStreamClient()
        {
            InitializeClient(useProxy: false);
        }

        /// <summary>
        /// 快手解封接口
        /// </summary>
        /// <param name="did">快手did</param>
        /// <returns></returns>
        public async Task KuaishouUnblock(string did)
        {
            try
            {
                var data = new
                {
                    common = new
                    {
                        identity_package = new
                        {
                            device_id = did,
                            global_id = ""
                        },
                        app_package = new
                        {
                            language = "zh-CN",
                            platform = 10,
                            container = "WEB",
                            product_name = "KS_GAME_LIVE_PC"
                        },
                        device_package = new
                        {
                            os_version = "NT 10.0",
                            model = "Windows",
                            ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0"
                        },
                        need_encrypt = false,
                        network_package = new
                        {
                            type = 3
                        },
                        h5_extra_attr = "{\"sdk_name\":\"webLogger\",\"sdk_version\":\"3.9.49\",\"sdk_bundle\":\"log.common.js\",\"app_version_name\":\"\",\"host_product\":\"\",\"resolution\":\"1920x1080\",\"screen_with\":1920,\"screen_height\":1080,\"device_pixel_ratio\":1,\"domain\":\"https://live.kuaishou.com\"}",
                        global_attr = "{}"
                    },
                    logs = new[]
                {
                    new
                    {
                        client_timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        client_increment_id = 5,
                        session_id = Guid.NewGuid().ToString(),
                        time_zone = "GMT+08:00",
                        event_package = new
                        {
                            show_event = new
                            {
                                action = 3,
                                sub_action = 3,
                                type = 11,
                                first_load = false,
                                time_cost = 0,
                                stay_length = 0,
                                status = 1,
                                action_type = 1,
                                url_package = new
                                {
                                    page = "https://live.kuaishou.com",
                                    identity = Guid.NewGuid().ToString(),
                                    page_type = 2
                                }
                            }
                        }
                    }
                }
                };

                var settings = new JsonSerializerSettings
                {
                    Formatting = Formatting.None,
                    NullValueHandling = NullValueHandling.Ignore,
                    ContractResolver = new DefaultContractResolver { NamingStrategy = new CamelCaseNamingStrategy() },
                    StringEscapeHandling = StringEscapeHandling.EscapeNonAscii
                };

                string dataStr = JsonConvert.SerializeObject(data, settings);

                byte[] compressedData = Compress(dataStr);

                // 构建请求 URI（含查询参数）
                var url = "https://log-sdk.ksapisrv.com/rest/wd/common/log/collect/misc2";
                var uriBuilder = new UriBuilder(url);
                var query = System.Web.HttpUtility.ParseQueryString(uriBuilder.Query);
                query["encoding"] = "gzip";
                query["v"] = "3.9.49";
                query["kpn"] = "KS_GAME_LIVE_PC";
                uriBuilder.Query = query.ToString();
                var requestUri = uriBuilder.Uri;

                // 创建请求对象
                using(var request = new HttpRequestMessage(HttpMethod.Post, requestUri))
                {
                    // 设置请求头
                    request.Headers.UserAgent.ParseAdd("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0");
                    request.Headers.AcceptEncoding.Add(new StringWithQualityHeaderValue("gzip"));
                    request.Headers.AcceptEncoding.Add(new StringWithQualityHeaderValue("deflate"));
                    request.Headers.AcceptEncoding.Add(new StringWithQualityHeaderValue("br"));
                    request.Headers.AcceptEncoding.Add(new StringWithQualityHeaderValue("zstd"));
                    request.Headers.Add("pragma", "no-cache");
                    request.Headers.Add("cache-control", "no-cache");
                    request.Headers.Add("origin", "https://live.kuaishou.com");
                    request.Headers.Add("sec-fetch-site", "cross-site");
                    request.Headers.Add("sec-fetch-mode", "cors");
                    request.Headers.Add("sec-fetch-dest", "empty");
                    request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");
                    request.Headers.Add("priority", "u=1, i");

                    // 设置请求内容
                    var content = new ByteArrayContent(compressedData);
                    content.Headers.ContentType = MediaTypeHeaderValue.Parse("application/octet-stream");
                    request.Content = content;

                    // 发送请求
                    using (HttpResponseMessage response = await _httpClient.SendAsync(request))
                    {
                        string responseText = await response.Content.ReadAsStringAsync();
                        FileUtils.LogError($"{responseText}", $"快手解封接口结果");
                    }

                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"调用快手解封接口发生异常");
            }
            
        }

        private byte[] Compress(string input)
        {
            byte[] inputBytes = Encoding.UTF8.GetBytes(input);
            using (var output = new MemoryStream())
            {
                using (var gzip = new System.IO.Compression.GZipStream(output, System.IO.Compression.CompressionLevel.Optimal))
                {
                    gzip.Write(inputBytes, 0, inputBytes.Length);
                }
                return output.ToArray();
            }
        }


        /// <summary>
        /// 获取主播直播信息
        /// </summary>
        /// <param name="webIdOrCustomId">快手网页id或自定义id</param>
        /// <returns></returns>
        public async Task<JObject> getAnchorLiveInfo(string webIdOrCustomId, bool retryed = false)
        {
            try
            {
                //string did = await KuaiShouCaptchaUtils.getDid();
                //if (string.IsNullOrEmpty(did))
                //{
                //    return null;
                //}
                //setDid(did);

                //string did = await KuaiShouCaptchaUtils.getDid();
                //setDid(did);

                CheckProxyExpire();

                _httpClient.DefaultRequestHeaders.Clear();
                _httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");
                _httpClient.DefaultRequestHeaders.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                
                _httpClient.DefaultRequestHeaders.Add("Upgrade-Insecure-Requests", "1");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Site", "none");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Mode", "navigate");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-User", "?1");
                _httpClient.DefaultRequestHeaders.Add("Sec-Fetch-Dest", "document");
                _httpClient.DefaultRequestHeaders.Add("Accept-Language", "zh-CN,zh;q=0.9");

                // 拼接 URL 和查询参数
                string url = $"https://live.kuaishou.com/u/{webIdOrCustomId}";

                // 发送 GET 请求
                using (HttpResponseMessage response = await _httpClient.GetAsync(url))
                {
                    var html = await response.Content.ReadAsStringAsync();

                    var match = Regex.Match(html, "\"liveroom\":(.*?),\"emoji\"");
                    if (match.Success)
                    {
                        // 截取出数据json字符串
                        string jsonStr = match.Groups[1].Value;
                        jsonStr = jsonStr.Replace("undefined", "null");

                        JObject jObject = JObject.Parse(jsonStr);

                        int? errorType = jObject?["playList"]?[0]?["errorType"]?["type"]?.Value<int>();
                        if (errorType != null && errorType == 2)
                        {
                            if (!retryed)
                            {
                                // 使用代理
                                UseProxyIP();

                                // 重新获取did
                                //KuaiShouCaptchaUtils.clearDid();
                                //FileUtils.LogRecrd($"快手被封控，重新获取did");

                                return await getAnchorLiveInfo(webIdOrCustomId, true);
                            }
                        }


                        return jObject;
                    }
                    else
                    {
                        if(!retryed)
                        {
                            // 使用代理
                            UseProxyIP();

                            // 重新获取did
                            //KuaiShouCaptchaUtils.clearDid();
                            //FileUtils.LogRecrd($"快手被封控，重新获取did");

                            return await getAnchorLiveInfo(webIdOrCustomId, true);
                        }
                        
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据快手webId或修改后的快手号 采集 主播信息==={webIdOrCustomId}");
            }
            

            return null;
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
        /// 使用代理IP
        /// </summary>
        private void UseProxyIP()
        {
            lock (_proxyIpLock)
            {
                //if (_isUseProxy)
                //{
                //    return;
                //}

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
