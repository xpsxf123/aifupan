using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.sliceVideo;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class VideoSliceApi
    {

        /// <summary>
        /// 保存视频切片信息-异步
        /// </summary>
        /// <param name="videoSliceEntity">视频切片信息</param>
        /// <returns></returns>
        public static async Task SaveVideoSliceAsync(VideoSliceEntity videoSliceEntity)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/videoSlice/saveVideoSlice", videoSliceEntity);

        }

        /// <summary>
        /// 保存切片相关数据（websocket、数据看板等）-异步
        /// </summary>
        /// <param name="videoSliceEntity">视频切片信息</param>
        /// <returns></returns>
        public static async Task SaveSliceCorrelationData(SaveSliceCorrelationDataBo saveSliceCorrelationDataBo)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/videoSlice/saveSliceCorrelationData", saveSliceCorrelationDataBo);

        }
    }
}
