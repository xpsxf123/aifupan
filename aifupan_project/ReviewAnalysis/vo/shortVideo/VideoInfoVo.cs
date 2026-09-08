using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    /// <summary>
    /// 视频信息实体类 
    /// </summary>
    public class VideoInfoVo
    {

        /// <summary>
        /// 主键ID（雪花ID）
        /// </summary>
        public long? Id { get; set; }

        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传 
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
        /// 发布时间 
        /// </summary>
        public string publishTime { get; set; }

        /// <summary>
        /// 发布时间戳
        /// </summary>
        public long publicDateTime { get; set; }

        /// <summary>
        /// 文案提取状态: 0-未提取 1-已提取 
        /// </summary>
        public int? extractStatus { get; set; }

        /// <summary>
        /// 文案提取时间 
        /// </summary>
        public string extractTime { get; set; }

        /// <summary>
        /// AI分析状态: 0-未分析 1-已分析 
        /// </summary>
        public int? analysisStatus { get; set; }

        /// <summary>
        /// AI分析时间
        /// </summary>
        public string analysisTime { get; set; }

        /// <summary>
        /// 创建时间 
        /// </summary>
        public string createdDate { get; set; }

        /// <summary>
        /// 更新时间 
        /// </summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 是否删除: 0-未删除, 1-已删除
        /// </summary>
        public int? isDeleted { get; set; }

    }
}
