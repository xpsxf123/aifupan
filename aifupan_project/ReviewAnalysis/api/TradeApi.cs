using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.vo.video;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class TradeApi
    {
        /// <summary>
        /// 根据行业id获取行业信息
        /// </summary>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static TradeVo GetTradeById(string tradeId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("tradeId", tradeId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/trade/getById", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<TradeVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 根据行业id获取行业信息（异步版本）
        /// </summary>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static async Task<TradeVo> GetTradeByIdAsync(string tradeId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("tradeId", tradeId);
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/trade/getById", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<TradeVo>(dataStr);
            }

            return null;
        }
    }
}
