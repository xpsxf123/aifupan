using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using Swan.Formatters;

namespace ReviewAnalysis.api
{
    public class AiTokenUseRecordApi
    {

        /// <summary>
        /// 保存aiToken记录
        /// </summary>
        /// <param name="recordVo"></param>
        /// <returns></returns>
        public static AiTokenUseRecordVo saveAiTokenUseRecord(AiTokenUseRecordVo recordVo)
        {   
            recordVo.tenantId = ReplayHttpUtils.ActiveTenantId.ToString();
            recordVo.userId = ReplayHttpUtils.UserId;
            recordVo.requestSourceType = 0;
            String str = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/aiRelated/saveAiTokenUseRecord", recordVo);
            AiTokenUseRecordVo result = JsonConvert.DeserializeObject<AiTokenUseRecordVo>(str);
            return result;
        }


    }
}
