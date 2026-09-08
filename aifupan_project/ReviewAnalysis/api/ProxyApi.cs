using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.user;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class ProxyApi
    {
        /// <summary>
        /// 获取代理IP
        /// </summary>
        /// <param name="forceUpdate">是否强制更换新的IP</param>
        /// <param name="validityType">时效类型 0：短效 1：长效</param>
        /// <returns></returns>
        public static async Task<ProxyIpVo> GetProxyIp(bool forceUpdate = false, int validityType = 0)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("forceUpdate", forceUpdate);
            param.Add("validityType", validityType);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/openapi/v2200/getProxyIp", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(dataStr);

                ProxyIpVo proxyIpVo = new ProxyIpVo();
                proxyIpVo.ip = jsonObject.ipStr;
                proxyIpVo.port = jsonObject.portStr;
                proxyIpVo.proxyPassword = jsonObject.proxyPassword;
                proxyIpVo.proxyUsername = jsonObject.proxyUsername;
                proxyIpVo.expireTime = jsonObject.expireTime;

                return proxyIpVo;
            }

            return null;
        }

        /// <summary>
        /// 获取代理IP-同步
        /// </summary>
        /// <param name="forceUpdate">是否强制更换新的IP</param>
        /// <param name="validityType">时效类型 0：短效 1：长效</param>
        /// <returns></returns>
        public static ProxyIpVo GetProxyIpSync(bool forceUpdate = false, int validityType = 0)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("forceUpdate", forceUpdate);
            param.Add("validityType", validityType);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2200/getProxyIp", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(dataStr);

                ProxyIpVo proxyIpVo = new ProxyIpVo();
                proxyIpVo.ip = jsonObject.ipStr;
                proxyIpVo.port = jsonObject.portStr;
                proxyIpVo.proxyPassword = jsonObject.proxyPassword;
                proxyIpVo.proxyUsername = jsonObject.proxyUsername;
                proxyIpVo.expireTime = jsonObject.expireTime;

                return proxyIpVo;
            }

            return null;
        }
    }
}
