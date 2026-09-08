using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class AnchorUrlEntity
    {
        /// <summary>
        /// ID
        /// </summary>
        public long Id { get; set; }

        /// <summary>
        /// 主播唯一标识
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 主页URL
        /// </summary>
        public string HomeUrl { get; set; }

        /// <summary>
        /// 直播间URL
        /// </summary>
        public string LiveUrl { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        public string AnchorName { get; set; }

        /// <summary>
        /// 主播头像
        /// </summary>
        public string AnchorAvatar { get; set; }

        /// <summary>
        /// 平台类型 0：抖音 1：快手 2：视频号
        /// </summary>
        public int Platform { get; set; }

        /// <summary>
        /// DouYinHomeLive(个人主页地址), DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
        /// </summary>
        public string PlatformResource { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }

        /// <summary>
        /// 主播userId
        /// </summary>
        public string AnchorUserId { get; set; }

        /// <summary>
        /// webSocketId
        /// </summary>
        public string WebSocketId { get; set; }

        /// <summary>
        /// 是否从录制列表移除了 0：否 1：是
        /// </summary>
        public int IsRemoveRecord { get; set; }

        /// <summary>
        /// 是否保存弹幕 0保存，1不保存
        /// </summary>
        public int IsBarrageMonitoring { get; set; }

        /// <summary>
        /// 检测在线直播时，是否录制视频 is_auto_record  0否，1是
        /// </summary>
        public int IsAutoRecord { get; set; }

        /// <summary>
        /// 是否自动上传到云空间 0：否 1：是
        /// </summary>
        public int IsAutoUploadCloud { get; set; }

        /// <summary>
        /// 是否置顶 0：否 1：是
        /// </summary>
        public int IsTop { get; set; }

        /// <summary>
        /// 加入置顶的时间
        /// </summary>
        public string AddTopTime { get; set; }
        /// <summary>
        /// 最后开始录制时间
        /// </summary>
        public string LastRecordTime { get; set; }
    }
}
