using douyin.Utils;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Bll.Anchor.Entity;
using CefSharp.DevTools.IO;
using ReviewAnalysis.Asr;
using System.Security.Policy;
using System.Threading;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.vo.proxyIP;

namespace ReviewAnalysis.Bll.Anchor
{
    public class AnchorOnlineWebClient
    {
        private static readonly Lazy<AnchorOnlineWebClient> _instance = new Lazy<AnchorOnlineWebClient>(() => new AnchorOnlineWebClient());
        private static bool switchUrl = false;
        private static string getAnchorOnlineUrl1 = "https://live.douyin.com/webcast/distribution/check_user_live_status";
        private static string getAnchorOnlineUrl2 = "https://webcast5-normal-m-hj.amemv.com/webcast/distribution/check_user_live_status";

        /// <summary>
        /// 请求实例
        /// </summary>
        public static AnchorOnlineWebClient Instance => _instance.Value;

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
        /// 允许更换的IP数量
        /// </summary>
        private volatile int _forceUpdateNum = 10;

        /// <summary>
        /// 私有化构造器
        /// </summary>
        private AnchorOnlineWebClient()
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
                _httpClientHandler = new HttpClientHandler();

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
        /// 获取主播在线状态请求
        /// </summary>
        /// <param name="anchorUserId">抖音主播用户id</param>
        /// <returns> -1：被风控 0：检测失败 1：在线 2：离线</returns>
        public int anchorOnlineRequest(string anchorUserId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("user_ids", anchorUserId);
            param.Add("aid", 6383);
            param.Add("distribution_scenes", 254);
            param.Add("channel", "test");

            string resultStr = "";
            try
            {
                string requestUrl = "";
                if (switchUrl)
                {
                    requestUrl = getAnchorOnlineUrl2;
                    switchUrl = false;
                }else
                {
                    requestUrl = getAnchorOnlineUrl1;
                    switchUrl = true;
                }

                // 拼接请求参数
                string paramStr = "";
                if (param != null && param.Count > 0)
                {
                    paramStr = string.Join("&", param.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value?.ToString())}"));
                }
                string url = $"{requestUrl}?{paramStr}";

                // 发送请求
                HttpResponseMessage response = null;
                
                try
                {
                    response = _httpClient.GetAsync(url).Result;
                    if (response.IsSuccessStatusCode)
                    {
                        // 通讯成功 
                        resultStr = response.Content.ReadAsStringAsync().Result;
                        if (!string.IsNullOrEmpty(resultStr))
                        {
                            var douYinIsOnlieResultEntity = JsonConvert.DeserializeObject<DouYinIsOnlieResultEntity>(resultStr);

                            // 判断是否在直播，UserLive不为空表示在直播
                            if (!string.IsNullOrEmpty(resultStr))
                            {
                                if (douYinIsOnlieResultEntity != null && douYinIsOnlieResultEntity.Data != null && douYinIsOnlieResultEntity.Data.Count > 0)
                                {
                                    foreach (var dataItem in douYinIsOnlieResultEntity.Data)
                                    {
                                        if (dataItem.UserLive != null && dataItem.UserLive.Count > 0)
                                        {
                                            UserLive userLive = dataItem.UserLive[0];
                                            if(userLive != null && userLive.LiveStatus == 1)
                                            {
                                                return 1;
                                            }else
                                            {
                                                return 2;
                                            }
                                        }
                                        else
                                        {
                                            return 2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        // 记录非成功状态码的日志 
                        int statusCode = (int)response.StatusCode;
                        FileUtils.LogRecrd($"{anchorUserId}", $"获取抖音主播在线状态请求异常，状态码: {statusCode}");

                        if (statusCode == 444)
                        {
                            return -1;
                        }
                    }

                }
                catch (Exception e)
                {
                    FileUtils.LogRecrd($"{e}", $"获取抖音主播在线状态请求发生异常，主播id：{anchorUserId}");
                    FileUtils.LogRecrd($"{resultStr}", $"获取抖音主播在线状态请求发生异常，抖音内容");
                }
                finally
                {
                    response?.Dispose();
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"获取抖音主播是否在线发生错误，主播id：{anchorUserId}");
                FileUtils.LogRecrd($"{resultStr}", $"获取抖音主播在线状态请求发生异常，抖音内容");
            }

            return 0;
        }

        /// <summary>
        /// 获取主播在线状态
        /// </summary>
        /// <param name="anchorUserId">抖音主播用户id</param>
        /// <returns> 0：检测失败 1：在线 2：离线</returns>
        public int getAnchorOnlineStatus(string anchorUserId)
        {
            // 首先检查代理状态和IP过期情况
            CheckAndUpdateProxyStatus();

            int status = anchorOnlineRequest(anchorUserId);
            if(status == -1)
            {
                // 被风控，启用代理
                if(!_isUseProxy)
                {
                    HandleRiskControl(anchorUserId);
                }
                
                status = 0; // 被风控时返回检测失败
            }

            return status;
        }

        /// <summary>
        /// 检查并更新代理状态
        /// </summary>
        private void CheckAndUpdateProxyStatus()
        {
            lock (_proxyIpLock) {

                if (!_isUseProxy)
                {
                    return;
                }

                long currentTime = ServerTimeUtils.getCurrentTime();

                // 检查代理周期是否过期
                if (currentTime > _proxyExpireTime)
                {
                    FileUtils.LogRecrd($"代理周期已过期", $"当前时间：{currentTime}，过期时间：{_proxyExpireTime}，结束代理周期并重置IP次数");
                    EndProxyCycle();
                    return;
                }

                // 如果IP获取次数已用完，但代理周期未过期，则等待周期结束
                if (_forceUpdateNum <= 0)
                {
                    FileUtils.LogRecrd($"代理IP获取次数已用完", $"剩余次数：{_forceUpdateNum}，等待代理周期结束，周期过期时间：{_proxyExpireTime}");
                    // 恢复直连，但是不结束代理周期，等待自然过期
                    if (_currentProxyIp != null)
                    {
                        SetProxy();
                        _currentProxyIp = null;
                    }
                    return;
                }

                // 检查当前IP是否过期，如果过期且还有获取次数则获取新IP
                if (_currentProxyIp != null && currentTime > _currentProxyIp.expireTime)
                {
                    FileUtils.LogRecrd($"当前代理IP已过期", $"当前时间：{currentTime}，IP过期时间：{_currentProxyIp.expireTime}，重新获取代理IP");
                    SetProxyIpInternal(true); // 强制更新IP
                }
            }

            
        }

        /// <summary>
        /// 处理风控情况
        /// </summary>
        /// <param name="anchorUserId">主播用户ID</param>
        private void HandleRiskControl(string anchorUserId)
        {
            FileUtils.LogRecrd($"{anchorUserId}", $"获取主播在线状态被风控，准备启用代理IP");

            // 检查用户权限
            UserInfoVo userInfoVo = UserApi.GetUserInfoVoSync();
            if (userInfoVo == null || userInfoVo.packageLevel <= 0)
            {
                FileUtils.LogRecrd($"用户无权限使用代理", $"packageLevel: {userInfoVo?.packageLevel}");
                return;
            }

            FileUtils.LogRecrd($"用户有权限使用代理", $"packageLevel: {userInfoVo.packageLevel}，开始启用代理");

            // 启动新的代理周期
            StartNewProxyCycle();
        }

        /// <summary>
        /// 启动新的代理周期
        /// </summary>
        private void StartNewProxyCycle()
        {
            long currentTime = ServerTimeUtils.getCurrentTime();

            // 设置代理使用标志
            _isUseProxy = true;

            // 随机设置代理周期时间：2-4小时（120-240分钟）
            Random random = new Random();
            int cycleMinutes = random.Next(120, 240);
            long cycleTimeMs = cycleMinutes * 60 * 1000;
            _proxyExpireTime = currentTime + cycleTimeMs;

            // 重置IP获取次数限制
            _forceUpdateNum = 10;

            FileUtils.LogRecrd($"启动新代理周期", $"周期时长：{cycleMinutes}分钟，过期时间：{_proxyExpireTime}，允许获取IP次数：{_forceUpdateNum}");

            // 获取并设置代理IP
            SetProxyIpInternal();
        }

        /// <summary>
        /// 结束代理周期
        /// </summary>
        private void EndProxyCycle()
        {
            // 清除代理设置
            SetProxy(); // 清除代理，恢复直连

            // 重置代理相关状态
            _isUseProxy = false;
            _proxyExpireTime = 0;
            _currentProxyIp = null;
            _forceUpdateNum = 10; // 重置为默认值，为下次代理周期做准备

            FileUtils.LogRecrd($"代理周期已结束", $"已切换回直连模式，代理状态已重置");
        }

        /// <summary>
        /// 设置代理IP
        /// </summary>
        /// <param name="forceUpdate">是否强制更换新的IP</param>
        private void SetProxyIpInternal(bool forceUpdate = false)
        {
            try
            {
                if (!_isUseProxy)
                {
                    FileUtils.LogRecrd($"代理未启用", $"_isUseProxy为false，跳过代理IP设置");
                    return;
                }

                // 获取当前时间
                long currentTime = ServerTimeUtils.getCurrentTime();

                // 检查代理周期是否过期
                if (currentTime > _proxyExpireTime)
                {
                    FileUtils.LogRecrd($"代理周期已过期", $"当前时间：{currentTime}，过期时间：{_proxyExpireTime}，结束使用代理并重置IP次数");
                    EndProxyCycle();
                    return;
                }

                // 检查IP获取次数是否超限
                if (_forceUpdateNum <= 0)
                {
                    FileUtils.LogRecrd($"代理IP获取次数已用完", $"剩余次数：{_forceUpdateNum}，等待代理周期结束，无法获取新IP");
                    // 恢复直连，但是不结束代理周期，等待自然过期
                    SetProxy();
                    return;
                }

                // 检查是否需要获取新的代理IP
                bool needNewIp = _currentProxyIp == null ||
                                currentTime > _currentProxyIp.expireTime ||
                                forceUpdate;

                if (needNewIp)
                {
                    string reason = _currentProxyIp == null ? "当前没有代理IP" :
                                   currentTime > _currentProxyIp.expireTime ? "IP已过期" : "强制更新";

                    FileUtils.LogRecrd($"需要获取新的代理IP", $"原因：{reason}，剩余获取次数：{_forceUpdateNum}");

                    // 从服务器获取新的代理IP
                    ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(forceUpdate);

                    if (proxyIpVo != null)
                    {
                        _currentProxyIp = proxyIpVo;

                        // 设置代理
                        SetProxy(proxyIpVo.ip, proxyIpVo.port, proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);

                        // 减少可用次数
                        _forceUpdateNum--;

                        FileUtils.LogRecrd($"代理IP设置成功", $"IP: {proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}，剩余获取次数：{_forceUpdateNum}");
                    }
                    else
                    {
                        FileUtils.LogError($"获取代理IP失败", $"从服务器获取代理IP返回null");
                        // 获取失败也要减少次数，避免无限重试
                        _forceUpdateNum--;
                    }
                }
                else
                {
                    FileUtils.LogRecrd($"当前代理IP仍有效", $"IP: {_currentProxyIp.ip}:{_currentProxyIp.port}，过期时间：{_currentProxyIp.expireTime}");
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"设置代理IP异常");
                // 异常时也减少次数，避免无限重试
                if (_forceUpdateNum > 0)
                {
                    _forceUpdateNum--;
                }
            }
        }

        /// <summary>
        /// 获取代理状态信息（用于调试和监控）
        /// </summary>
        /// <returns></returns>
        public string GetProxyStatusInfo()
        {
            if (!_isUseProxy)
            {
                return "代理未启用";
            }

            long currentTime = ServerTimeUtils.getCurrentTime();
            string currentIpInfo = _currentProxyIp != null ?
                $"{_currentProxyIp.ip}:{_currentProxyIp.port}(过期时间:{_currentProxyIp.expireTime})" : "无";

            string status = "";
            if (currentTime > _proxyExpireTime)
            {
                status = "周期已过期";
            }
            else if (_forceUpdateNum <= 0)
            {
                status = "IP次数已用完，已恢复直连，等待周期结束";
            }
            else
            {
                status = "正常运行";
            }

            return $"代理已启用 - 状态:{status}, 周期过期时间:{_proxyExpireTime}, 当前IP:{currentIpInfo}, 剩余获取次数:{_forceUpdateNum}, 当前时间:{currentTime}";
        }
    }
}
