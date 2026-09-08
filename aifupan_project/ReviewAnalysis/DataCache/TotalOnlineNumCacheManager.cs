using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Security.Policy;


namespace ReviewAnalysis.DataCache
{
    public class TotalOnlineNumCacheManager
    {
        private static ConcurrentDictionary<string, TotalOnlineNum> TotalOnlineNumCaches = new ConcurrentDictionary<string, TotalOnlineNum>();

        /// <summary>
        /// 清空缓存
        /// </summary>
        public static void Clear()
        {
            TotalOnlineNumCaches.Clear();
        }

        /// <summary>
        /// 设置场观人次到缓存
        /// </summary>
        /// <param name="totalOnlineNum"></param>
        public static void SetOnlineNumCache(TotalOnlineNum totalOnlineNum) 
        {

            TotalOnlineNumCaches.AddOrUpdate(totalOnlineNum.BatchNumber, totalOnlineNum, (oldKey, oldValue) => totalOnlineNum);
        }

        /// <summary>
        /// 获取全部场观人次列表
        /// </summary>
        /// <returns></returns>
        public static List<TotalOnlineNum> GetAllOnlineNumList()
        {
            List<TotalOnlineNum> result = new List<TotalOnlineNum>();
            foreach (var item in TotalOnlineNumCaches)
            {
                result.Add(item.Value);
            }
            return result;
        }

        /// <summary>
        /// 根据用户id获取场观人次列表
        /// </summary>
        /// <param name="userId">用户id</param>
        /// <returns></returns>
        public static List<TotalOnlineNum> GetOnlineNumListByUserId(string userId)
        {
            List<TotalOnlineNum> result = new List<TotalOnlineNum>();
            foreach (var item in TotalOnlineNumCaches)
            {
                TotalOnlineNum totalOnlineNum = item.Value;
                if (totalOnlineNum.UserId.Equals(userId))
                {
                    result.Add(totalOnlineNum);
                }
            }
            return result;
        }

        /// <summary>
        /// 根据主播SecUid获取场观人次列表
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns></returns>
        public static List<TotalOnlineNum> GetOnlineNumListBySecUid(string secUid)
        {
            List<TotalOnlineNum> result = new List<TotalOnlineNum>();

            List<TotalOnlineNum> totalOnlineNums = GetOnlineNumListByUserId(ReplayHttpUtils.UserId);
            if(totalOnlineNums != null && totalOnlineNums.Count > 0)
            {
                foreach (var item in totalOnlineNums)
                {
                    if (item.SecUid.Equals(secUid))
                    {
                        result.Add(JsonConvert.DeserializeObject<TotalOnlineNum>(JsonConvert.SerializeObject(item)));
                    }
                }
            }

            return result;
        }


        /// <summary>
        /// 根据直播场次号获取场观人次信息
        /// </summary>
        /// <param name="batchNumber">直播场次号</param>
        /// <returns></returns>
        public static TotalOnlineNum GetOnlineNumByBatchNumber(string batchNumber)
        {
            List<TotalOnlineNum> onlineNums = GetOnlineNumListByUserId(ReplayHttpUtils.UserId);
            if(onlineNums != null && onlineNums.Count > 0)
            {
                foreach (var item in onlineNums)
                {
                    if (item.BatchNumber.Equals(batchNumber)) {
                        return item;
                    }
                }
            }
            
            return null;
        }

        /// <summary>
        /// 根据视频id获取场观人次信息
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns></returns>
        public static TotalOnlineNum GetOnlineNumByVideoId(string videoId)
        {
            foreach (var item in TotalOnlineNumCaches)
            {
                TotalOnlineNum totalOnlineNum = item.Value;
                if (totalOnlineNum.VideoId.Contains(videoId))
                {
                    return totalOnlineNum;
                }
            }
            return null;
        }

        /// <summary>
        /// 从缓存更新/添加数据到数据库
        /// </summary>
        public static void SetOnlineNumToDataBaseFromCache()
        {

            foreach (var item in TotalOnlineNumCaches)
            {
                string json = JsonConvert.SerializeObject(item.Value);
                TotalOnlineNum totalOnlineNum = JsonConvert.DeserializeObject<TotalOnlineNum>(json);
                if (totalOnlineNum.Id > 0)
                {
                    totalOnlineNum.Update();
                }
                else
                {
                    if (totalOnlineNum.Id == 0)
                    {
                        int id = totalOnlineNum.Save();
                        //重新填充实体
                        totalOnlineNum.Id = id;
                        TotalOnlineNumCaches.AddOrUpdate(totalOnlineNum.BatchNumber, totalOnlineNum, (oldKey, oldValue) => totalOnlineNum);
                    }
                }
            }
        }
    }
}
