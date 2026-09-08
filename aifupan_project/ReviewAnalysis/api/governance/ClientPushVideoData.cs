using ReviewAnalysis.Asr;
using ReviewAnalysis.plugins.models;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.api.governance
{
    public class ClientPushVideoData
    {
        /// <summary>
        /// 获取contextId
        /// </summary>
        /// <param name="data">数据</param>
        /// <returns></returns>
        public static HttpUtils.R clientPushVideo(UnifiedDataModel data)
        {
            return HttpUtils.SendServerPostR(ReplayHttpUtils.GovernanceBaseUrl + "/api/governance/performance/video/clientPushVideo", data);
        }
    }
}