using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.entity.anchor;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.cueWords;

namespace ReviewAnalysis.api
{
    public class CueWordsApi
    {

        /// <summary>
        /// 根据secUid获取主播信息-异步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public static async Task<CueWordsVo> info(string id)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/cuewords/info", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<CueWordsVo>(dataStr);
            }

            return null;
        }

    }
}
