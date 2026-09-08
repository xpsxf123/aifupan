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
    public class OnlineNumCacheManager
    {
        private static ConcurrentDictionary<string, OnlineNumNew> OnlineNumCaches = new ConcurrentDictionary<string, OnlineNumNew>();

        /// <summary>
        /// 清空缓存
        /// </summary>
        public static void Clear()
        {
            OnlineNumCaches.Clear();
        }
        /// <summary>
        /// 设置在线人数到缓存
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void SetOnlineNumCache(OnlineNumNew onlineNum) 
        {

            OnlineNumCaches.AddOrUpdate(onlineNum.VideoId, onlineNum, (oldKey, oldValue) => onlineNum);
        }

        /// <summary>
        /// 获取全部在线人数列表
        /// </summary>
        /// <returns></returns>
        public static List<OnlineNumNew> GetAllOnlineNumList()
        {
            List<OnlineNumNew> result = new List<OnlineNumNew>();
            foreach (var item in OnlineNumCaches)
            {
                result.Add(item.Value);
            }
            return result;
        }

        /// <summary>
        /// 根据用户id获取在线人数列表
        /// </summary>
        /// <param name="userId">用户id</param>
        /// <returns></returns>
        public static List<OnlineNumNew> GetOnlineNumListByUserId(string userId)
        {
            List<OnlineNumNew> result = new List<OnlineNumNew>();
            foreach (var item in OnlineNumCaches)
            {
                OnlineNumNew onlineNum = item.Value;
                if (onlineNum.UserId.Equals(userId))
                {
                    result.Add(onlineNum);
                }
            }
            return result;
        }


        /// <summary>
        /// 根据直播场次号获取在线人数信息
        /// </summary>
        /// <param name="batchNumber">直播场次号</param>
        /// <returns></returns>
        public static OnlineNumNew GetOnlineNumByBatchNumber(string batchNumber)
        {
            List<OnlineNumNew> onlineNums = GetOnlineNumListByUserId(ReplayHttpUtils.UserId);
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
        /// 根据视频id获取在线人数信息
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns></returns>
        public static OnlineNumNew GetOnlineNumByVideoId(string videoId)
        {
            foreach (var item in OnlineNumCaches)
            {
                OnlineNumNew onlineNum = item.Value;
                if (onlineNum.VideoId.Contains(videoId))
                {
                    return onlineNum;
                }
            }
            return null;
        }

        /// <summary>
        /// 从缓存更新/添加数据到数据库
        /// </summary>
        public static void SetOnlineNumToDataBaseFromCache()
        {

            foreach (var item in OnlineNumCaches)
            {
                string json = JsonConvert.SerializeObject(item.Value);
                OnlineNumNew onlineNum = JsonConvert.DeserializeObject<OnlineNumNew>(json);
                if (onlineNum.Id > 0)
                {
                    onlineNum.Update();
                }
                else
                {
                    if (onlineNum.Id == 0)
                    {
                        int id = onlineNum.Save();
                        //重新填充实体
                        onlineNum.Id = id;
                        OnlineNumCaches.AddOrUpdate(onlineNum.VideoId, onlineNum, (oldKey, oldValue) => onlineNum);
                    }
                }
            }
        }
    }
}
