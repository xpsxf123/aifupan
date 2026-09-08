using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.DataCache
{
    public class VideoCacheManager
    {

        /// <summary>
        /// 视频列表
        /// </summary>
        private static ConcurrentDictionary<string, VideoEntity> VideoCaches = new ConcurrentDictionary<string, VideoEntity>();

        /// <summary>
        /// 清空缓存
        /// </summary>
        public static void VideoCachesClear()
        {
            VideoCaches.Clear();
        }

        /// <summary>
        /// 根据视频唯一标识id获取视频信息
        /// </summary>
        /// <param name="videoId">视频唯一标识id</param>
        /// <returns></returns>
        public static VideoEntity GetVideosByVideoId(string videoId)
        {
            foreach (var item in VideoCaches)
            {
                if (item.Value.videoId == videoId)
                {
                    return item.Value;
                }
            }
            return null;
        }

        /// <summary>
        /// 将数据同步到视频信息缓存
        /// </summary>
        /// <param name="video"></param>
        public static void SetVideoToCache(VideoEntity video)
        {
            if (video != null)
            {

                VideoCaches.AddOrUpdate(video.videoId, video, (oldKey, oldValue) => video);
            }
        }

        /// <summary>
        /// 从缓存删除视频信息
        /// </summary>
        public static void DelVideoCache(VideoEntity video)
        {
            VideoCaches.TryRemove(video.videoId, out VideoEntity _);
        }

        /// <summary>
        /// 根据主播secUid获取视频列表
        /// </summary>
        /// <param name="secUid">主播Id</param>
        /// <returns></returns>
        public static List<VideoEntity> ListVideoBySecUid(string secUid)
        {
            List<VideoEntity> result = new List<VideoEntity>();
            foreach (var item in VideoCaches)
            {
                if (item.Value.secUid == secUid)
                {
                    result.Add(item.Value);
                }
            }
            List<VideoEntity> list = result.OrderByDescending(a => DateTime.Parse(a.startTime)).ToList();
            return list;
        }

        /// <summary>
        /// 根据录制状态获取视频列表
        /// </summary>
        /// <param name="recordingStatus">录制状态 0否 1是 2：停止录制了但是断网没能更新到服务端</param>
        /// <returns></returns>
        public static List<VideoEntity> ListVideoByRecordStatus(int recordingStatus)
        {
            List<VideoEntity> result = new List<VideoEntity>();
            foreach (var item in VideoCaches)
            {
                if (item.Value.isRecording == recordingStatus)
                {
                    result.Add(item.Value);
                }
            }
            List<VideoEntity> list = result.OrderByDescending(a => DateTime.Parse(a.startTime)).ToList();
            return list;
        }
    }
}
