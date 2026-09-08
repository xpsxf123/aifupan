using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    /// <summary>
    /// 视频热搜视频信息视图对象
    /// </summary>
    public class VideoHotSearchVideoInfoVo
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
        /// 达人头像URL
        /// </summary>
        public string influencerAvatar { get; set; }

        /// <summary>
        /// 达人粉丝数
        /// </summary>
        public long? influencerFollowersCount { get; set; }

        /// <summary>
        /// 视频平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 平台视频ID
        /// </summary>
        public string platformVideoId { get; set; }

        /// <summary>
        /// 视频文件HASH值 mongodb存储关联值内容
        /// </summary>
        public string videoHash { get; set; }

        /// <summary>
        /// 视频标题
        /// </summary>
        public string title { get; set; }

        /// <summary>
        /// 视频描述
        /// </summary>
        public string description { get; set; }

        /// <summary>
        /// 封面图片URL
        /// </summary>
        public string coverUrl { get; set; }

        /// <summary>
        /// 视频播放URL
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 作者ID 如果是本地上传：user_id 平台：influencer_id
        /// </summary>
        public string authorId { get; set; }

        /// <summary>
        /// 作者名称
        /// </summary>
        public string authorName { get; set; }

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

        /// <summary>
        /// 视频时长(秒)
        /// </summary>
        public long? duration { get; set; }

        /// <summary>
        /// 发布时间 (格式如: "2025-08-11 10:30:00")
        /// </summary>
        public string publishTime { get; set; }
    }
}
