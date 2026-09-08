using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.vo.shortVideo;

namespace ReviewAnalysis.bo.shortVideo
{
    /// <summary>
    /// 搜爆款数据同步请求对象
    /// </summary>
    public class VideoHotSearchSyncDataBo
    {

        /// <summary>
        /// 关键词
        /// </summary>
        public string keyword {  get; set; }

        /// <summary>
        /// 
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 视频列表
        /// </summary>
        public List<VideoHotSearchSyncVideoVo> videoList { get; set; }
    }

    public class VideoHotSearchSyncVideoVo : VideoInfoVo
    {
        /// <summary>
        /// 达人平台类型: 1-抖音, 2-快手, 3-视频号
        /// </summary>
        public int? influencerPlatformType { get; set; }

        /// <summary>
        /// 达人平台用户ID
        /// </summary>
        public string influencerPlatformUserId { get; set; }

        /// <summary>
        /// 达人昵称
        /// </summary>
        public string influencerNickname { get; set; }

        /// <summary>
        /// 达人头像
        /// </summary>
        public string influencerAvatar { get; set; }

        /// <summary>
        /// 达人粉丝数
        /// </summary>
        public long? influencerFollowersCount { get; set; }
    }
}
