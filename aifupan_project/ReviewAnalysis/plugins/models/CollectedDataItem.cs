using System;

namespace ReviewAnalysis.plugins.models
{
    /// <summary>
    /// 采集数据缓存项
    /// 用于缓存各平台采集的原始数据
    /// </summary>
    public class CollectedDataItem
    {
        /// <summary>
        /// 平台ID (juliang, qianchuan, enterprise, life, anchorLive)
        /// </summary>
        public string PlatformId { get; set; }

        /// <summary>
        /// 原始JSON数据
        /// </summary>
        public string RawJson { get; set; }

        /// <summary>
        /// 采集时间
        /// </summary>
        public DateTime CollectTime { get; set; }

        /// <summary>
        /// 直播间ID
        /// </summary>
        public string RoomId { get; set; }

        /// <summary>
        /// 视频ID（可选）
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 创建采集数据项
        /// </summary>
        public static CollectedDataItem Create(string platformId, string rawJson, string secUid, string roomId, string videoId = null)
        {
            return new CollectedDataItem
            {
                PlatformId = platformId,
                RawJson = rawJson,
                CollectTime = DateTime.Now,
                SecUid = secUid,
                RoomId = roomId,
                VideoId = videoId
            };
        }
    }
}
