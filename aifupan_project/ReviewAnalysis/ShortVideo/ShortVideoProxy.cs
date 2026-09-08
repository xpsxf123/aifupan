using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;

namespace ReviewAnalysis.ShortVideo
{
    public class ShortVideoProxy
    {
        private static ProxyIpVo currentProxy;

        
        /// <summary>
        /// 获取用于短视频的代码ip
        /// </summary>
        /// <param name="forceUpdate"></param>
        /// <returns></returns>
        public static ProxyIpVo getShortActingProxy(bool forceUpdate)
        {
            if (!forceUpdate && currentProxy != null)
            {
                long current = ServerTimeUtils.getServerCurrentTime();
                if (currentProxy.expireTime > current)
                {
                    return currentProxy;
                }
            }

            // 从服务器获取新的代理IP
            setProxy();

            return ShortVideoProxy.currentProxy;
        }

        /// <summary>
        /// 请求接口获取代理ip
        /// </summary>
        private static void setProxy()
        {
            //if (currentMaxCount != -1 && getCurrentDayCount() >= currentMaxCount)
            //{
            //    ShortVideoProxy.currentProxy = null;
            //    FileUtils.log("每日次数已经", "搜爆款代理ip获取失败");
            //    return;
            //}

            // 从服务器获取新的代理IP
            ProxyIpVo proxyIpVo = null;
            try
            {
                proxyIpVo = ProxyApi.GetProxyIpSync(true);
                FileUtils.log($"代理 ip：{proxyIpVo.ip}, prot = {proxyIpVo.port}");
            }
            catch (Exception ex)
            {

            }
            FileUtils.log(proxyIpVo == null ? "成功" : "失败", "获取代理ip-短视频");
            ShortVideoProxy.currentProxy = proxyIpVo;
        }
    }
}
