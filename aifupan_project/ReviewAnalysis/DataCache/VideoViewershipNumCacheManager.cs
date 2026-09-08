using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.DataCache
{
    public class VideoViewershipNumCacheManager
    {
        private static ConcurrentDictionary<string, VideoViewershipNum> VideoViewershipNumCaches = new ConcurrentDictionary<string, VideoViewershipNum>();

        /// <summary>
        /// 清空缓存
        /// </summary>
        public static void Clear()
        {
            VideoViewershipNumCaches.Clear();
        }
        /// <summary>
        /// 设置场观人次到缓存
        /// </summary>
        /// <param name="totalOnlineNum"></param>
        public static void SetOnlineNumCache(VideoViewershipNum totalOnlineNum)
        {
            VideoViewershipNumCaches.AddOrUpdate(totalOnlineNum.VideoId, totalOnlineNum, (oldKey, oldValue) => totalOnlineNum);
        }

        /// <summary>
        /// 获取全部场观人次列表
        /// </summary>
        /// <returns></returns>
        public static List<VideoViewershipNum> GetAllOnlineNumList()
        {
            List<VideoViewershipNum> result = new List<VideoViewershipNum>();
            foreach (var item in VideoViewershipNumCaches)
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
        public static List<VideoViewershipNum> GetOnlineNumListByUserId(string userId)
        {
            List<VideoViewershipNum> result = new List<VideoViewershipNum>();
            foreach (var item in VideoViewershipNumCaches)
            {
                VideoViewershipNum totalOnlineNum = item.Value;
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
        public static List<VideoViewershipNum> GetOnlineNumListBySecUid(string secUid)
        {
            List<VideoViewershipNum> result = new List<VideoViewershipNum>();

            List<VideoViewershipNum> totalOnlineNums = GetOnlineNumListByUserId(ReplayHttpUtils.UserId);
            if (totalOnlineNums != null && totalOnlineNums.Count > 0)
            {
                foreach (var item in totalOnlineNums)
                {
                    if (item.SecUid.Equals(secUid))
                    {
                        result.Add(JsonConvert.DeserializeObject<VideoViewershipNum>(JsonConvert.SerializeObject(item)));
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
        public static VideoViewershipNum GetOnlineNumByBatchNumber(string batchNumber)
        {
            List<VideoViewershipNum> onlineNums = GetOnlineNumListByUserId(ReplayHttpUtils.UserId);
            if (onlineNums != null && onlineNums.Count > 0)
            {
                foreach (var item in onlineNums)
                {
                    if (item.BatchNumber.Equals(batchNumber))
                    {
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
        public static VideoViewershipNum GetOnlineNumByVideoId(string videoId)
        {
            foreach (var item in VideoViewershipNumCaches)
            {
                VideoViewershipNum totalOnlineNum = item.Value;
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

            foreach (var item in VideoViewershipNumCaches)
            {
                string json = JsonConvert.SerializeObject(item.Value);
                VideoViewershipNum totalOnlineNum = JsonConvert.DeserializeObject<VideoViewershipNum>(json);
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
                        VideoViewershipNumCaches.AddOrUpdate(totalOnlineNum.VideoId, totalOnlineNum, (oldKey, oldValue) => totalOnlineNum);
                    }
                }
            }
        }
    }
}
