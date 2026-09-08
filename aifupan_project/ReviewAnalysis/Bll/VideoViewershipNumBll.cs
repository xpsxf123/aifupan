using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll
{
    public class VideoViewershipNumBll
    {

        /// <summary>
        /// 添加或修改总观看人次
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="batchNumber">直播场次号</param>
        /// <param name="ViewershipNum">在线人数</param>
        public void SaveOrUpdate(string secUid, string batchNumber, string ViewershipNum, string barrageNum, string videoId, string videoStartTime,string videoEndTime)
        {

            VideoViewershipNum totalOnlineNum = new VideoViewershipNum();
            totalOnlineNum.GetModelByBatchNumber(batchNumber, videoId);
            if (totalOnlineNum != null && !string.IsNullOrEmpty(totalOnlineNum.VideoId))
            {
                // 修改
                totalOnlineNum.ViewershipNum = ViewershipNum;
                totalOnlineNum.UpdateDate = ServerTimeUtils.getCurrentTimeStr();        
                totalOnlineNum.BarrageNum = barrageNum;
                VideoViewershipNumCacheManager.SetOnlineNumCache(totalOnlineNum);
            }
            else
            {
                VideoViewershipNum onlineNum = new VideoViewershipNum();
                onlineNum.SecUid = secUid;
                onlineNum.BatchNumber = batchNumber;
                onlineNum.ViewershipNum = ViewershipNum;
                onlineNum.BarrageNum = barrageNum;
                onlineNum.VideoId = videoId;
                onlineNum.UserId = ReplayHttpUtils.UserId;
                onlineNum.UpdateDate = ServerTimeUtils.getCurrentTimeStr();
                onlineNum.CreateDate = ServerTimeUtils.getCurrentTimeStr();
                VideoViewershipNumCacheManager.SetOnlineNumCache(onlineNum);
            }

        }

        public void LoadToCache()
        {
            VideoViewershipNum totalOnlineNum = new VideoViewershipNum();
            totalOnlineNum.UserId = ReplayHttpUtils.UserId;
            List<VideoViewershipNum> totalOnlineNums = totalOnlineNum.GetList();

            if(totalOnlineNums != null && totalOnlineNums.Count > 0)
            {
                foreach (var item in totalOnlineNums)
                {
                    if(string.IsNullOrEmpty(item.CreateDate))
                    {

                        item.CreateDate = item.UpdateDate;
                    }
                    VideoViewershipNumCacheManager.SetOnlineNumCache(item);
                }
            }
        }
    }
}
