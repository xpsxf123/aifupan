using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.anchor
{
    public class AnchorUrlInfoVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 主播唯一标识
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 主页url
        /// </summary>
        public string homeUrl { get; set; }

        /// <summary>
        /// 直播间url
        /// </summary>
        public string liveUrl { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        public string anchorName { get; set; }

        /// <summary>
        /// 主播头像
        /// </summary>
        public string anchorAvatar { get; set; }

        /// <summary>
        /// 平台类型 0：抖音 1：快手 2：视频号
        /// </summary>
        public int platform { get; set; }

        /// <summary>
        /// DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
        /// </summary>
        public string platformResource { get; set; }

        /// <summary>
        /// 主播userId
        /// </summary>
        public string anchorUserId { get; set; }

        /// <summary>
        /// webSocketId
        /// </summary>
        public string webSocketId { get; set; }

        /// <summary>
        /// 主播抖音号
        /// </summary>
        public string anchorNumber { get; set; }
    }
}
