using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Caching;
using System.Text;
using System.Threading.Tasks;
using CefSharp.DevTools.CSS;
using CefSharp.DevTools.IndexedDB;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.Websocket;

namespace ReviewAnalysis.DataCache
{
    public class WebsocketProxyCacheManager
    {

        private static ObjectCache cache = MemoryCache.Default;
        private static object lockObject = new object();  // 用于线程同步

        private static string proxyKey = "replay:websocket:proxy-expire-time";

        private static int proxyExoireTime = 3540; // 秒
        //private static int proxyExoireTime = 120; // 秒

        /// <summary>
        /// 使用代理
        /// </summary>
        public static void addProxy()
        {
            lock (lockObject)
            {
                // 判断Key是否存在，存在则直接返回false 
                if (cache.Contains(proxyKey))
                {
                    return;
                }
                var policy = new CacheItemPolicy
                {
                    AbsoluteExpiration = DateTimeOffset.Now.AddSeconds(proxyExoireTime)
                };

                cache.Add(proxyKey, 1, policy);
            }
        }

        /// <summary>
        /// 判断是否使用代理
        /// </summary>
        /// <returns></returns>
        public static bool hasProxy()
        {
            lock (lockObject)
            {
                return cache.Contains(proxyKey);
            }
        }

        /// <summary>
        /// 获取Websocket的代理url
        /// </summary>
        /// <param name="forceUpdate">是否强制更新</param>
        /// <returns></returns>
        public static string getProxyHttpUrl(string proxyUrl, bool isRepeat)
        {
            lock (lockObject)
            {
                // 去服务器获取代理
                // 如果获取的ip是一样的，就重新获取
                // 如果获取的ip是不一样的就用
                string tempUrl = getProxyUrl(false);
                FileUtils.log(tempUrl, "获取代理ip-有缓存");
                if (tempUrl == null) return "";
                if (isRepeat) return tempUrl;
                if (tempUrl.Equals(proxyUrl))
                {
                    string tempUrl2 = getProxyUrl(true);
                    FileUtils.log(tempUrl2, "获取代理ip-请求新的ip");
                    if (tempUrl2 == null) return "";
                    return tempUrl2;
                }
                else
                {
                    return tempUrl;
                }
            }
        }

        /// <summary>
        /// 获取代理url
        /// </summary>
        /// <param name="forceUpdate"></param>
        /// <returns></returns>
        public static string getProxyUrl(bool forceUpdate)
        {
            ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(forceUpdate, 1);
            long currentTime = ServerTimeUtils.getServerCurrentTime();
            if (proxyIpVo != null)
            {
                long ex = proxyIpVo.expireTime - currentTime;
                long temp = ex / 1000;
                if (temp < 0)
                {
                    return getProxyUrl(true);
                }

                var policy = new CacheItemPolicy
                {
                    AbsoluteExpiration = DateTimeOffset.Now.AddSeconds(temp)
                };
                return $"http://{proxyIpVo.proxyUsername}:{proxyIpVo.proxyPassword}@{proxyIpVo.ip}:{proxyIpVo.port}";
            }
            else
            {
                // 获取null代表今天的代理已经获取到打上限了，只能明天才能获取了
                WebsocketConnection.disableProxyTime = DateTime.Today.AddDays(1).AddSeconds(-1);
            }
            return null;
        }

        /// <summary>
        /// 获取proxyUrl-有校验
        /// </summary>
        /// <param name="proxyUrl">单前的代理ip</param>
        /// <param name="isRepeat">是否可以使用重复的ip</param>
        /// <returns></returns>
        public static string getChangeProxyHttpUrl(string proxyUrl, bool isRepeat)
        {
            lock (lockObject)
            {
                if (hasProxy())
                {
                    return getProxyHttpUrl(proxyUrl, isRepeat);
                }
                return "";
            }
        }

    }
}
