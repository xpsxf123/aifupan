using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using COSXML.Network;
using douyin.Utils;
using Microsoft.Extensions.Caching.Memory;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.user;

namespace ReviewAnalysis.Utils
{
    public class ProxyIPUtils
    {
        /// <summary>
        /// 是否在定时获取IP
        /// </summary>
        //public static volatile bool isGetIPing = false;
        /// <summary>
        /// 当前代理IP信息
        /// </summary>
        public static volatile ProxyIpVo currentProxyIp;
        /// <summary>
        /// 是否正在使用代理
        /// </summary>
        public static volatile bool isUseProxy;
        /// <summary>
        /// 使用代理的过期时间，超过这个时间则切回正常IP
        /// </summary>
        public static long expireTime = 0;
        /// <summary>
        /// 允许更换的IP数量
        /// </summary>
        public static volatile int forceUpdateNum = 10;

        private readonly object _lock = new object();
        private static readonly object _staticLock = new object();


        /// <summary>
        /// 获取代理IP
        /// </summary>
        /// <param name="forceUpdate">是否强制更换新的IP</param>
        /// <returns></returns>
        public static async Task SetProxyIp(bool forceUpdate = false)
        {
            try
            {
                if(!isUseProxy)
                {
                    return;
                }

                // 获取当前时间
                long currentTime = ServerTimeUtils.getCurrentTime();

                if(currentTime > expireTime || forceUpdateNum <= 0)
                {
                    // 当前已超过代理的使用时间 或 当前已超过获取代理IP的次数，切回正常IP
                    FileUtils.LogRecrd($"结束使用代理", $"当前已超过代理的使用时间 或 当前已超过获取代理IP的次数，切回正常IP");
                    DouyinHttpClient client = DouyinHttpClient.Instance;
                    client.SetProxy();

                    expireTime = 0;
                    currentProxyIp = null;
                    isUseProxy = false;
                    return;
                }

                if (currentProxyIp == null || currentTime > currentProxyIp.expireTime || forceUpdate)
                {
                    // 当前没有代理IP 或 IP已过期 或 需强制更新，从服务器获取新的代理IP
                    ProxyIpVo proxyIpVo = await ProxyApi.GetProxyIp(forceUpdate);
                    currentProxyIp = proxyIpVo;
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(proxyIpVo)}", $"当前没有代理IP 或 IP已过期 或 需强制更新，从服务器获取新的代理IP");
                    // 给请求客户端设置代理
                    if(proxyIpVo != null)
                    {
                        DouyinHttpClient webClient = DouyinHttpClient.Instance;
                        webClient.SetProxy(proxyIpVo.ip, proxyIpVo.port, proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);

                        FileUtils.LogRecrd($"{JsonConvert.SerializeObject(proxyIpVo)}，剩余尝试次数：{forceUpdateNum}", $"设置代理成功");
                        forceUpdateNum--;
                    }
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"ex", $"从服务器获取代理IP异常");
            }
        }

        /// <summary>
        /// 获取代理IP-同步
        /// </summary>
        /// <param name="forceUpdate">是否强制更换新的IP</param>
        /// <returns></returns>
        public static void SetProxyIpSync(bool forceUpdate = false)
        {
            try
            {
                if (!isUseProxy)
                {
                    return;
                }

                // 获取当前时间
                long currentTime = ServerTimeUtils.getCurrentTime();

                if (currentTime > expireTime || forceUpdateNum <= 0)
                {
                    // 当前已超过代理的使用时间 或 当前已超过获取代理IP的次数，切回正常IP
                    FileUtils.LogRecrd($"结束使用代理", $"当前已超过代理的使用时间 或 当前已超过获取代理IP的次数，切回正常IP");
                    DouyinHttpClient client = DouyinHttpClient.Instance;
                    client.SetProxy();

                    expireTime = 0;
                    currentProxyIp = null;
                    isUseProxy = false;
                }

                if (currentProxyIp == null || currentTime > currentProxyIp.expireTime || forceUpdate)
                {
                    // 当前没有代理IP 或 IP已过期 或 需强制更新，从服务器获取新的代理IP
                    ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(forceUpdate);
                    currentProxyIp = proxyIpVo;
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(proxyIpVo)}", $"当前没有代理IP 或 IP已过期 或 需强制更新，从服务器获取新的代理IP");
                    // 给请求客户端设置代理
                    if (proxyIpVo != null)
                    {
                        DouyinHttpClient webClient = DouyinHttpClient.Instance;
                        webClient.SetProxy(proxyIpVo.ip, proxyIpVo.port, proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);

                        FileUtils.LogRecrd($"{JsonConvert.SerializeObject(proxyIpVo)}，剩余代理IP数：{forceUpdateNum}", $"设置代理成功");
                        forceUpdateNum--;
                    }
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"ex", $"从服务器获取代理IP异常");
            }
        }

        /// <summary>
        /// 获取代理IP
        /// </summary>
        /// <param name="forceUpdate">是否强制更换新的IP</param>
        /// <returns></returns>
        //public static async Task<ProxyIpVo> GetProxyIp(bool forceUpdate = false)
        //{
        //    try
        //    {
        //        Dictionary<string, object> param = new Dictionary<string, object>();
        //        param.Add("forceUpdate", forceUpdate);
        //        string result = await HttpUtils.SendGetSync(ReplayHttpUtils.BaseUrl + "/openapi/v2200/getProxyIp", param);

        //        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
        //        if (jsonObject != null && jsonObject.code == 0 && jsonObject.data != null)
        //        {
        //            ProxyIpVo proxyIpVo = new ProxyIpVo();
        //            proxyIpVo.ip = jsonObject.data.ipStr;
        //            proxyIpVo.port = jsonObject.data.portStr;
        //            proxyIpVo.proxyPassword = jsonObject.data.proxyPassword;
        //            proxyIpVo.proxyUsername = jsonObject.data.proxyUsername;
        //            proxyIpVo.expireTime = jsonObject.data.expireTime;

        //            return proxyIpVo;
        //        }
        //    }
        //    catch (Exception ex)
        //    {
        //        FileUtils.LogError($"ex", $"从服务器获取代理IP异常");
        //    }

        //    return null;

        //}



        ///// <summary>
        ///// 使用代理
        ///// </summary>
        //public static void UseProxy()
        //{
        //    lock (_staticLock)
        //    {
        //        try
        //        {
        //            if (isGetIPing)
        //            {
        //                return;
        //            }

        //            isGetIPing = true;

        //            Random random = new Random();
        //            // 随机获取240-420，毫秒
        //            int time = random.Next(240, 420) * 60 * 1000;

        //            long startTime = ServerTimeUtils.getCurrentTime();

        //            while (true)
        //            {
        //                long currentTime = ServerTimeUtils.getCurrentTime();
        //                if (currentTime - startTime > time)
        //                {
        //                    // 超过代理的使用时间，恢复原本IP
        //                    DouyinHttpClient client = DouyinHttpClient.Instance;
        //                    client.SetProxy();

        //                    break;
        //                }

        //                // 从服务器获取代理
        //                ProxyIpVo proxyIpVo = GetProxyIp();
        //                if (proxyIpVo != null)
        //                {
        //                    if (currentProxyIp == null || !proxyIpVo.ip.Equals(currentProxyIp.ip)) { 
        //                        // 给请求客户端设置代理
        //                        DouyinHttpClient webClient = DouyinHttpClient.Instance;
        //                        webClient.SetProxy(proxyIpVo.ip, proxyIpVo.port, proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);
        //                        currentProxyIp = proxyIpVo;
        //                    }
        //                }

        //                Thread.Sleep(20 * 1000);
        //            }

        //            isGetIPing = false;
        //        }
        //        catch (Exception ex)
        //        {
        //            FileUtils.LogRecrd($"{ex}", $"使用代理发生异常");
        //            isGetIPing = false;
        //        }

        //    }


        //}

        //public ProxyIpVo GetProxyIp()
        //{
        //    lock(_lock)
        //    {
        //        if (proxyCache.TryGetValue("proxyIp", out ProxyIpVo cacheProxyIpVo))
        //        {
        //            return cacheProxyIpVo;
        //        }
        //        else
        //        {
        //            // 缓存已过期或不存在
        //            ProxyIpVo proxyIpVo = GetServerProxxyIp();
        //            if(proxyIpVo != null)
        //            {
        //                // 设置到缓存
        //                return proxyIpVo;
        //            }
        //        }

        //        return null;
        //    }
        //}

        /// <summary>
        /// 从服务器获取代理IP
        /// </summary>
        /// <returns></returns>
        private ProxyIpVo GetServerProxxyIp()
        {
            try
            {
                string result = HttpUtils.SendGet(ReplayHttpUtils.BaseUrl + "openapi/v2200/getProxyIp", null);

                var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                if (jsonObject != null && jsonObject.code == 0 && jsonObject.data != null)
                {
                    if (DateTime.TryParseExact(jsonObject.data.validityDate, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime dateTime))
                    {
                        // 解析成功 
                        ProxyIpVo proxyIpVo = new ProxyIpVo();
                        proxyIpVo.ip = jsonObject.data.ipStr;
                        proxyIpVo.port = jsonObject.data.portStr;
                        DateTimeOffset dto = new DateTimeOffset(dateTime, TimeZoneInfo.Local.GetUtcOffset(dateTime));
                        //proxyIpVo.expireTime = dto.ToUnixTimeSeconds();

                        return proxyIpVo; 
                    }
                    
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"ex", $"从服务器获取代理IP异常");
            }

            return null;
        }
    }
}
