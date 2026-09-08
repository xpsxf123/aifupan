using System.Collections.Generic;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.config;

namespace ReviewAnalysis.api
{
    public class OrderApi
    {
        /// <summary>
        /// 纯录制版/复盘按切换
        /// </summary>
        /// <param name="clientVersion"></param>
        /// <returns></returns>
        public static void setClientVersionConfig(string clientVersion)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("clientVersion", clientVersion);
            HttpUtils.R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/order/clientOrder/setClientVersionConfig", param);
            if (r.code != 0)
            {
                throw new CustomException(r?.msg ?? "当前网络情况不佳，请稍后重试或联系管理员");
            }
        }

        /// <summary>
        /// 获取客户端版本配置
        /// </summary>
        /// <returns></returns>
        public static SetTokenVo userClientVersionConfig()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/order/clientOrder/userClientVersionConfig", null);
            
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SetTokenVo>(dataStr);
            }

            return null;
        }
    }
}