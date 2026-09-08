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
    public class TotalOnlineNumBll
    {

        /// <summary>
        /// 添加或修改总观看人次
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="batchNumber">直播场次号</param>
        /// <param name="PeopleNum">在线人数</param>
        public void SaveOrUpdate(string secUid, string batchNumber, string PeopleNum, string videoId,string videoStartTime,string videoEndTime)
        {

            TotalOnlineNum totalOnlineNum = new TotalOnlineNum();
            totalOnlineNum.GetModelByBatchNumber(batchNumber);
            if (totalOnlineNum != null && !string.IsNullOrEmpty(totalOnlineNum.BatchNumber))
            {
                // 修改
                totalOnlineNum.PeopleNum = PeopleNum;
                totalOnlineNum.RecordDate = ServerTimeUtils.getCurrentTimeStr();

                if (string.IsNullOrEmpty(totalOnlineNum.VideoId))
                {
                   // totalOnlineNum.VideoId = videoId;
                }
                else if (!totalOnlineNum.VideoId.Contains(videoId))
                {
                   // totalOnlineNum.VideoId = totalOnlineNum.VideoId + "_" + videoId;
                }

                TotalOnlineNumCacheManager.SetOnlineNumCache(totalOnlineNum);
            }
            else
            {
                TotalOnlineNum onlineNum = new TotalOnlineNum();
                onlineNum.SecUid = secUid;
                onlineNum.BatchNumber = batchNumber;
                onlineNum.PeopleNum = PeopleNum;
                onlineNum.UserId = ReplayHttpUtils.UserId;
                onlineNum.RecordDate = ServerTimeUtils.getCurrentTimeStr();
                onlineNum.VideoId = videoId;
                onlineNum.StartRecordDate = videoStartTime;
                TotalOnlineNumCacheManager.SetOnlineNumCache(onlineNum);
            }

        }

        public void LoadToCache()
        {
            TotalOnlineNum totalOnlineNum = new TotalOnlineNum();
            totalOnlineNum.UserId = ReplayHttpUtils.UserId;
            List<TotalOnlineNum> totalOnlineNums = totalOnlineNum.GetList();

            if(totalOnlineNums != null && totalOnlineNums.Count > 0)
            {
                foreach (var item in totalOnlineNums)
                {
                    if(string.IsNullOrEmpty(item.StartRecordDate))
                    {

                        item.StartRecordDate = item.RecordDate;
                    }
                    TotalOnlineNumCacheManager.SetOnlineNumCache(item);
                }
            }
        }
    }
}
