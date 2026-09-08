using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Qiniu.Util;

namespace ReviewAnalysis.bo.shortVideo
{
    /// <summary>
    /// 爆款搜索业务对象
    /// </summary>
    public class VideoHotSearchBo
    {

        /// <summary>
        /// 搜索关键词
        /// </summary>
        public String searchKeyword {  get; set; }

        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号
        /// </summary>

        public int? platformType { get; set; }

        /// <summary>
        /// 抓取数据类型：1：服务器 2：第三方
        /// </summary>
        public int? snatchDataType { get; set; }

        /// <summary>
        /// 搜索结果视频列表
        /// </summary>

        public List<VideoHotSearchInfoBo> videoList { get; set; }

    }

    /// <summary>
    /// 视频热搜信息业务对象
    /// </summary>
    public class VideoHotSearchInfoBo
    {
        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 平台视频ID 
        /// </summary>
        public string platformVideoId { get; set; }

        /// <summary>
        /// 平台用户ID
        /// </summary>
        public string platformUserId { get; set; }

        /// <summary>
        /// 视频标题 
        /// </summary>
        public string title { get; set; }

        /// <summary>
        /// 视频文件hash值 
        /// </summary>
        public string videoHash { get; set; }

        /// <summary>
        /// 视频描述
        /// </summary>
        public string description { get; set; }

        /// <summary>
        /// 视频封面URL 
        /// </summary>
        public string coverUrl { get; set; }

        /// <summary>
        /// 视频播放URL 
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 作者ID
        /// </summary>
        public string authorId { get; set; }

        /// <summary>
        /// 作者名称 
        /// </summary>
        public string authorName { get; set; }

        /// <summary>
        /// 达人头像URL
        /// </summary>
        public string influencerAvatar { get; set; }

        /// <summary>
        /// 达人粉丝数 
        /// </summary>
        public long? influencerFollowersCount { get; set; }

        /// <summary>
        /// 视频时长(秒)
        /// </summary>
        public long? duration { get; set; }

        /// <summary>
        /// 发布时间 
        /// </summary>
        public string publishTime { get; set; }

        /// <summary>
        /// 点赞数 
        /// </summary>
        public long? likeCount { get; set; }

        /// <summary>
        /// 评论数 
        /// </summary>
        public long? commentCount { get; set; }

        /// <summary>
        /// 分享数 
        /// </summary>
        public long? shareCount { get; set; }

        /// <summary>
        /// 收藏数 
        /// </summary>
        public long? collectCount { get; set; }
    }
}
