using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.barrage;

namespace ReviewAnalysis.api
{
    public class BarrageApi
    {
        /// <summary>
        /// 查询视频对应的弹幕标注
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static BarrageDataVo videoBarrageData(string videoId)
        {
            BarrageDataVo result = new BarrageDataVo();
            result.barrageDataList = new List<BarrageData>();
            result.totalBarrageNum = 0;
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2400/videoBarrageData", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<BarrageDataVo>(dataStr);
            }
            return result;

        }
        /// <summary>
        /// 查询视频对应的弹幕标注
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public async static Task<BarrageDataVo>  videoBarrageDataAsync(string videoId)
        {
            BarrageDataVo result = new BarrageDataVo();
            result.barrageDataList = new List<BarrageData>();
            result.totalBarrageNum = 0;
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/openapi/v2400/videoBarrageData", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<BarrageDataVo>(dataStr);
            }
            return result;

        }

        public static OnlineChartDto onlineChartData(string videoId)
        {

            OnlineChartDto result = new OnlineChartDto();
            result.totalViewersNum = 0;
            result.maxOnlineNum = 0;
            result.totalBarrageNum = 0;
            result.approachDataList = new List<LineChartData>();
            result.onlineDataList = new List<LineChartData>();
            result.barrageDataList = new List<LineChartData>();

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2100/onlineChartData", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<OnlineChartDto>(dataStr);
            }
            return result;
        }

        public static dynamic onlineChartData2(string videoId)
        {
            OnlineChartDto result = new OnlineChartDto();

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2100/onlineChartData", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<dynamic>(dataStr);
            }
            return result;
        }

        /// <summary>
        /// 异步获取在线图表数据
        /// </summary>
        public static async Task<dynamic> OnlineChartData2Async(string videoId)
        {
            OnlineChartDto result = new OnlineChartDto();

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/openapi/v2100/onlineChartData", param).ConfigureAwait(false);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<dynamic>(dataStr);
            }
            return result;
        }

    }
}
