using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.oceanEngineData;
using ReviewAnalysis.vo.proxyIP;
using static ReviewAnalysis.Utils.HttpUtils;

namespace ReviewAnalysis.api
{
    public class OceanEngineDataApi
    {

        /// <summary>
        /// 获取诊断报告上传的预签名链接
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static SignUploadUrlVo getDiagnosisSignUploadUrl(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/words/oceanEngineData/getSignUploadUrl", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SignUploadUrlVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 巨量数据更新接口
        /// </summary>
        /// <param name="batchNumber">直播批次</param>
        /// <param name="videoId">视频id</param>
        /// <param name="secUid">主播</param>
        /// <param name="cosPath">ossKey</param>
        /// <param name="dataJson">汇总数据</param>
        /// <returns></returns>
        public static string updateOceanEngine(JuliangGatherDataEntity juliangGatherDataEntity)
        {

            return HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/oceanEngineData/updateOceanEngine", juliangGatherDataEntity);
        }

        /// <summary>
        /// 获取巨量的数据
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public static OceanEngineDataVo getOceanEngine(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            R r = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/words/oceanEngineData/getOceanEngine", param);
            if (r.success() && !string.IsNullOrEmpty(r.data))
            {
                return JsonConvert.DeserializeObject<OceanEngineDataVo>(r.data);
            }

            return null;
        }

        /// <summary>
        /// 根据视频id获取看盘数据
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static VideoDataViewingConfuseVo videodataviewingInfoByVideoId(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/videodataviewing/infoByVideoId", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<VideoDataViewingConfuseVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 根据视频id获取看盘数据-异步
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public async static Task<VideoDataViewingConfuseVo> videodataviewingInfoByVideoIdAsync(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/videodataviewing/infoByVideoId", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<VideoDataViewingConfuseVo>(dataStr);
            }

            return null;
        }

    }
}
