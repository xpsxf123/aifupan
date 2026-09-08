using ReviewAnalysis.HttpServer;
using System;
using System.Collections.Generic;
using System.IO.Compression;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Cache;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;
using System.Security.Policy;
using System.Text;
using System.Threading.Tasks;
using System.Net.Http;
using System.Runtime.InteropServices;
using douyin.Utils;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.api;

namespace ReviewAnalysis.Utils
{
    public class DouyinHttpClient
    {

        private static readonly Lazy<DouyinHttpClient> _instance = new Lazy<DouyinHttpClient>(() => new DouyinHttpClient());

        /// <summary>
        /// 请求实例
        /// </summary>
        public static DouyinHttpClient Instance => _instance.Value;

        private HttpClient _httpClient;
        private HttpClientHandler _httpClientHandler;
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
        /// 私有化构造器
        /// </summary>
        private DouyinHttpClient()
        {
            InitializeClient(useProxy: false);
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

                // 创建新的 Handler
                _httpClientHandler = new HttpClientHandler
                {
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                    ServerCertificateCustomValidationCallback = (sender, cert, chain, errors) => true,
                    UseCookies = true,
                    AllowAutoRedirect = false
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
                ConfigureDefaultHeaders();
            }
        }

        ///// <summary>
        ///// 设置/更新代理
        ///// </summary>
        ///// <param name="proxyIp">代理的IP</param>
        ///// <param name="proxyPort">代理的端口</param>
        ///// <param name="username">代理验证账号</param>
        ///// <param name="password">代理验证密码</param>
        //public void SetProxy(string proxyIp = "", string proxyPort = "", string username = "", string password = "")
        //{
        //    bool useProxy = !string.IsNullOrEmpty(proxyIp) && !string.IsNullOrEmpty(proxyPort);
        //    InitializeClient(useProxy, proxyIp, proxyPort, username, password);
        //}

        /// <summary>
        /// 配置默认请求头
        /// </summary>
        private void ConfigureDefaultHeaders()
        {
            _httpClient.DefaultRequestHeaders.Clear();
            _httpClient.DefaultRequestHeaders.Add("authority", "live.douyin.com");
            _httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            _httpClient.DefaultRequestHeaders.Referrer = new Uri("https://live.douyin.com/");
            _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");
            _httpClient.DefaultRequestHeaders.Add("Cookie", "ttwid=1%7CB1qls3GdnZhUov9o2NxOMxxYS2ff6OSvEWbv0ytbES4%7C1680522049%7C280d802d6d478e3e78d0c807f7c487e7ffec0ae4e5fdd6a0fe74c3c6af149511; my_rd=1; passport_csrf_token=3ab34460fa656183fccfb904b16ff742; passport_csrf_token_default=3ab34460fa656183fccfb904b16ff742; d_ticket=9f562383ac0547d0b561904513229d76c9c21; n_mh=hvnJEQ4Q5eiH74-84kTFUyv4VK8xtSrpRZG1AhCeFNI; store-region=cn-fj; store-region-src=uid; LOGIN_STATUS=1; __security_server_data_status=1; FORCE_LOGIN=%7B%22videoConsumedRemainSeconds%22%3A180%7D; pwa2=%223%7C0%7C3%7C0%22; download_guide=%223%2F20230729%2F0%22; volume_info=%7B%22isUserMute%22%3Afalse%2C%22isMute%22%3Afalse%2C%22volume%22%3A0.6%7D; strategyABtestKey=%221690824679.923%22; stream_recommend_feed_params=%22%7B%5C%22cookie_enabled%5C%22%3Atrue%2C%5C%22screen_width%5C%22%3A1536%2C%5C%22screen_height%5C%22%3A864%2C%5C%22browser_online%5C%22%3Atrue%2C%5C%22cpu_core_num%5C%22%3A8%2C%5C%22device_memory%5C%22%3A8%2C%5C%22downlink%5C%22%3A10%2C%5C%22effective_type%5C%22%3A%5C%224g%5C%22%2C%5C%22round_trip_time%5C%22%3A150%7D%22; VIDEO_FILTER_MEMO_SELECT=%7B%22expireTime%22%3A1691443863751%2C%22type%22%3Anull%7D; home_can_add_dy_2_desktop=%221%22; __live_version__=%221.1.1.2169%22; device_web_cpu_core=8; device_web_memory_size=8; xgplayer_user_id=346045893336; csrf_session_id=2e00356b5cd8544d17a0e66484946f28; odin_tt=724eb4dd23bc6ffaed9a1571ac4c757ef597768a70c75fef695b95845b7ffcd8b1524278c2ac31c2587996d058e03414595f0a4e856c53bd0d5e5f56dc6d82e24004dc77773e6b83ced6f80f1bb70627; __ac_nonce=064caded4009deafd8b89; __ac_signature=_02B4Z6wo00f01HLUuwwAAIDBh6tRkVLvBQBy9L-AAHiHf7; ttcid=2e9619ebbb8449eaa3d5a42d8ce88ec835; webcast_leading_last_show_time=1691016922379; webcast_leading_total_show_times=1; webcast_local_quality=sd; live_can_add_dy_2_desktop=%221%22; msToken=1JDHnVPw_9yTvzIrwb7cQj8dCMNOoesXbA_IooV8cezcOdpe4pzusZE7NB7tZn9TBXPr0ylxmv-KMs5rqbNUBHP4P7VBFUu0ZAht_BEylqrLpzgt3y5ne_38hXDOX8o=; msToken=jV_yeN1IQKUd9PlNtpL7k5vthGKcHo0dEh_QPUQhr8G3cuYv-Jbb4NnIxGDmhVOkZOCSihNpA2kvYtHiTW25XNNX_yrsv5FN8O6zm3qmCIXcEe0LywLn7oBO2gITEeg=; tt_scid=mYfqpfbDjqXrIGJuQ7q-DlQJfUSG51qG.KUdzztuGP83OjuVLXnQHjsz-BRHRJu4e986");
        }


        /// <summary>
        /// 读取网页内容（异步方法）
        /// </summary>
        public async Task<string> GetHtmlTextAsync(string url)
        {

            CheckProxyExpire();

            // 打印使用的代理
            if (_httpClientHandler.Proxy is WebProxy webProxy)
            {
                var proxyUri = webProxy.Address;
                FileUtils.LogRecrd($"{proxyUri.Host}:{proxyUri.Port}", $"发送请求时使用了代理");
            }
            else
            {
                // 未使用代理
                FileUtils.LogRecrd($"发送请求时未使用代理");
            }

            // 动态 Cookie 配置（示例，实际需替换为动态逻辑）
            var request = new HttpRequestMessage(HttpMethod.Get, url);

            using (var response = await _httpClient.SendAsync(request))
            {
                response.EnsureSuccessStatusCode();
                byte[] bytes = await response.Content.ReadAsByteArrayAsync();
                string resultStr = Encoding.UTF8.GetString(bytes);
                return resultStr;
            }
        }

        /// <summary>
        ///  读取网页内容（同步）
        /// </summary>
        /// <param name="url"></param>
        /// <returns></returns>
        public string GetHtmlText(string url)
        {

            CheckProxyExpire();

            // 打印使用的代理
            if (_httpClientHandler.Proxy is WebProxy webProxy)
            {
                var proxyUri = webProxy.Address;
                FileUtils.LogRecrd($"{proxyUri.Host}:{proxyUri.Port}", $"发送请求时使用了代理");
            }
            else
            {
                // 未使用代理
                FileUtils.LogRecrd($"发送请求时未使用代理");
            }

            // 动态 Cookie 配置（示例，实际需替换为动态逻辑）
            var request = new HttpRequestMessage(HttpMethod.Get, url);

            using (var response = _httpClient.SendAsync(request).Result)
            {
                response.EnsureSuccessStatusCode();
                byte[] bytes = response.Content.ReadAsByteArrayAsync().Result;
                string resultStr = Encoding.UTF8.GetString(bytes);
                return resultStr;
            }
        }

        /// <summary>
        /// 释放资源
        /// </summary>
        public void Dispose()
        {
            _httpClient?.Dispose();
            _httpClientHandler?.Dispose();
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
                    // 未启用代理IP，return
                    return;
                }

                long currentTime = ServerTimeUtils.getCurrentTime();

                // 检查代理周期是否过期
                if (currentTime > _proxyExpireTime)
                {
                    FileUtils.LogRecrd($"当前时间：{currentTime}，过期时间：{_proxyExpireTime}", $"抖音采集流地址代理IP已过期");
                    SetProxy();
                    _isUseProxy = false;
                }
            }
        }
        /// <summary>
        /// 使用代理IP
        /// </summary>
        public void UseProxyIP()
        {
            lock (_proxyIpLock)
            {
                if (_isUseProxy)
                {
                    // 已经在使用代理IP，return
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

                    FileUtils.LogRecrd($"IP: {proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}", $"抖音采集流地址代理IP设置成功");
                }
                else
                {
                    FileUtils.LogRecrd($"没有可用IP，已取消设置代理", $"获取抖音采集流地址代理IP失败");
                    SetProxy();
                    _isUseProxy = false;
                }
            }

        }
    }
}
