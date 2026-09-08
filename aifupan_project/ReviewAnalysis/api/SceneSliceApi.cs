using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class SceneSliceApi
    {

        /// <summary>
        /// 获取场景切片状态
        /// </summary>
        /// <param name="videoId">视频ID</param>
        /// <returns>状态VO，失败返回null</returns>
        public static SceneSliceStatusVo GetStatus(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string responseBody = HttpUtils.SendServerPost(
                ReplayHttpUtils.BaseUrl + "/ai/sceneSlice/getStatus", param);

            if (!string.IsNullOrEmpty(responseBody))
            {
                return JsonConvert.DeserializeObject<SceneSliceStatusVo>(responseBody);
            }

            return null;
        }

        /// <summary>
        /// 保存场景切片记录（客户端上传截图后调用，服务端创建记录并触发AI分析）
        /// </summary>
        /// <param name="videoId">视频ID</param>
        /// <param name="ossKey">OSS文件key</param>
        /// <param name="sliceSeconds">截取秒数</param>
        /// <returns>是否成功</returns>
        public static bool Save(string videoId, string ossKey, int sliceSeconds)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("ossKey", ossKey);
            param.Add("sliceSeconds", sliceSeconds);

            string responseBody = HttpUtils.SendServerPost(
                ReplayHttpUtils.BaseUrl + "/ai/sceneSlice/save", param);

            return !string.IsNullOrEmpty(responseBody);
        }

    }
}
