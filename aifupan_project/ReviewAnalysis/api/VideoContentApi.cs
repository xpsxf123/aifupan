using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.video;

namespace ReviewAnalysis.api
{
    public class VideoContentApi
    {

        /// <summary>
        /// 根据视频id获取视频信息
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public static AnchorVideoFileAllVo getVideoContent(string sourceId, int sourceType, int type)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("type", type);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/words/videoContent/getVideoContent", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                AnchorVideoFileAllVo videoEntity = JsonConvert.DeserializeObject<AnchorVideoFileAllVo>(dataStr);
                return videoEntity;
            }

            return null;
        }

        /// <summary>
        /// 获取当前用户待生产的自然、优化原文
        /// </summary>
        /// <returns></returns>
        public static async Task<List<ToGeneratedVo>> contentByToGenerated()
        {
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/words/videoContent/contentByToGenerated", null);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<ToGeneratedVo>>(dataStr);
            }

            return new List<ToGeneratedVo>();
        }

        /// <summary>
        /// 保存自然、优化原文
        /// </summary>
        /// <param name="id"></param>
        /// <param name="content"></param>
        /// <returns></returns>
        public static async Task saveVideoContent(string id, string content)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);
            param.Add("content", content);
            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/words/videoContent/saveVideoContent", param);
        }

        /// <summary>
        /// 检测自然、优化原文是否生成成功-不返回
        /// </summary>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="type"></param>
        /// <returns></returns>
        public static async Task checkVideoContent(string sourceId, int sourceType, int type)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("type", type);
            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/words/videoContent/completeVideoContentGenerate", param);
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="type"></param>
        /// <returns></returns>
        public static ToGeneratedVo generateOntQAContent(string sourceId, int sourceType, int type)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("type", type);
            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/videoContent/generateOntQAContent", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<ToGeneratedVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 生成自然、优化原文（异步版本，避免网络异常时卡顿）
        /// </summary>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="type"></param>
        /// <returns></returns>
        public static async Task<ToGeneratedVo> generateOntQAContentAsync(string sourceId, int sourceType, int type)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("type", type);
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/words/videoContent/generateOntQAContent", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<ToGeneratedVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 是否要自动生成自然/优化原文
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static async Task<bool> hasOneVideo(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/words/videoContent/hasOneVideo", param);

            bool res = false;
            if (!string.IsNullOrEmpty(dataStr))
            {
                bool.TryParse(dataStr, out res);
            }

            return res;
        }
    }
}
