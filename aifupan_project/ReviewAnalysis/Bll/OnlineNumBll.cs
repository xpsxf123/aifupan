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
    public class OnlineNumBll
    {

        /// <summary>
        /// 保存在线人数
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="batchNumber">直播场次号</param>
        /// <param name="peopleNum">在线人数</param>
        public void Save(string secUid, string batchNumber, string peopleNum, string videoId)
        {

            OnlineNumNew oldOnlineNum = OnlineNumCacheManager.GetOnlineNumByVideoId(videoId);

            if (oldOnlineNum != null)
            {
                // 已经存在，修改记录
                //if (string.IsNullOrEmpty(oldOnlineNum.VideoId))
                //{
                //    oldOnlineNum.VideoId = videoId;
                //}
                //else if (!oldOnlineNum.VideoId.Contains(videoId))
                //{
                //    oldOnlineNum.VideoId = oldOnlineNum.VideoId + "_" + videoId;
                //}

                string time = ServerTimeUtils.getCurrentTimeStr();
                oldOnlineNum.PeopleNumData = oldOnlineNum.PeopleNumData + "_" + time + "@" + peopleNum;
                OnlineNumCacheManager.SetOnlineNumCache(oldOnlineNum);

            }
            else
            {
                // 没有存在，新增一条记录
                OnlineNumNew onlineNum = new OnlineNumNew();
                onlineNum.SecUid = secUid;
                onlineNum.BatchNumber = batchNumber;
                onlineNum.UserId = ReplayHttpUtils.UserId;
                onlineNum.VideoId = videoId;
                onlineNum.PeopleNumData = ServerTimeUtils.getCurrentTimeStr() + "@" + peopleNum;
                OnlineNumCacheManager.SetOnlineNumCache(onlineNum);
            }




        }

        public void LoadToCache()
        {
            OnlineNumNew onlineNum = new OnlineNumNew();
            onlineNum.UserId = ReplayHttpUtils.UserId;
            List<OnlineNumNew> onlineNums = onlineNum.GetList();

            if (onlineNums != null && onlineNums.Count > 0)
            {

                foreach (var item in onlineNums)
                {
                    OnlineNumCacheManager.SetOnlineNumCache(item);
                }

                //int i = 0;

                //foreach (var item in onlineNums)
                //{
                //    FileUtils.log(i);
                //    item.DeleteModelById(item.Id);
                //    i++;
                //}

                //int count = onlineNums.Count % 5000 == 0 ? onlineNums.Count / 5000 : (onlineNums.Count / 5000 + 1);

                //for(int j = 0; j < count; j++)
                //{

                //}


                //// 将旧数据转成新数据格式存储
                //List<OnlineNumNew> newOnlineList = new List<OnlineNumNew>();
                //int i = 0;
                //foreach (var item in onlineNums)
                //{
                //    i++;
                //    FileUtils.log($"已完成：{i}==={item}");
                //    if (string.IsNullOrEmpty(item.PeopleNumData))
                //    {
                //        if (newOnlineList.Count > 0)
                //        {
                //            bool exist = false;
                //            foreach (var newOnlineItem in newOnlineList)
                //            {
                //                if (newOnlineItem.BatchNumber.Equals(item.BatchNumber))
                //                {
                //                    // 已经存在，修改记录
                //                    if (string.IsNullOrEmpty(newOnlineItem.VideoId))
                //                    {
                //                        newOnlineItem.VideoId = item.VideoId;
                //                    }
                //                    else if (!newOnlineItem.VideoId.Contains(item.VideoId))
                //                    {
                //                        newOnlineItem.VideoId = newOnlineItem.VideoId + "_" + item.VideoId;
                //                    }
                //                    newOnlineItem.PeopleNumData = newOnlineItem.PeopleNumData + "_" + item.RecordDate + "@" + item.PeopleNum;

                //                    exist = true;
                //                    break;
                //                }
                //            }

                //            if (!exist)
                //            {
                //                item.PeopleNumData = item.RecordDate + "@" + item.PeopleNum;
                //                item.RecordDate = "";
                //                item.PeopleNum = "";
                //                newOnlineList.Add(item);
                //            }

                //        }
                //        else
                //        {
                //            item.PeopleNumData = item.RecordDate + "@" + item.PeopleNum;
                //            item.RecordDate = "";
                //            item.PeopleNum = "";
                //            newOnlineList.Add(item);
                //        }

                //        item.DeleteModelById(item.Id);
                //    }
                //}

                //// 将新数据插入数据库
                //if (newOnlineList.Count > 0)
                //{
                //    foreach (var item in newOnlineList)
                //    {
                //        item.Save();
                //    }

                //    onlineNums = onlineNum.GetList();
                //}
            }

            //if (onlineNums != null && onlineNums.Count > 0)
            //{
            //    foreach (var item in onlineNums)
            //    {
            //        OnlineNumCacheManager.SetOnlineNumCache(item);
            //    }
            //}
        }
    }
}
