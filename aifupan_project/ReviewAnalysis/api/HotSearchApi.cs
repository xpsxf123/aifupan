using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.hotSearch;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using static ReviewAnalysis.Utils.HttpUtils;

namespace ReviewAnalysis.api
{
    public class HotSearchApi
    {
        /// <summary>
        /// 获取可用的邮箱账号
        /// </summary>
        /// <returns></returns>
        public static HotSearchEmailAccountVo GetHotSearchEmailAccount(int? sceneType = 0)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sceneType", sceneType);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/video/hotSearch/email/getAccount", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<HotSearchEmailAccountVo>(result.data);
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }
        public static async Task  HotSearchReportFailure(ReportFailureVo param)
        {
            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/video/hotSearch/email/reportFailure", param);

        }
    }
}
