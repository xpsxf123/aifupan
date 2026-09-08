using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    /// <summary>
    /// 用户视频关联实体（严格小驼峰格式）
    /// </summary>
    public class VideoUserVideoVo
    {

        /// <summary>
        /// 主键ID（雪花ID）
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 视频唯一标识
        /// </summary>
        public long? videoId { get; set; }

        /// <summary>
        /// 视频标题 
        /// </summary>
        public string videoTitle { get; set; }

        /// <summary>
        /// 租户ID 
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 用户ID
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 来源类型：1-短视频URL 2-本地上传 3-搜达人 4-搜爆款
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 来源id（短视频：用户id；搜达人：达人id；搜爆款：爆款搜索id）
        /// </summary>
        public long? sourceId { get; set; }

        /// <summary>
        /// 文案提取状态：0-未提取 1-待处理 2-处理中 3-已完成 4-失败 
        /// </summary>
        public int? extractStatus { get; set; }

        /// <summary>
        /// 文案提取时间 
        /// </summary>
        public string extractTime { get; set; }

        /// <summary>
        /// 提取失败原因 
        /// </summary>
        public string extractErrorReason { get; set; }

        /// <summary>
        /// AI分析状态：0-未分析 1-待处理 2-处理中 3-已完成 4-失败 
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
        /// 视频URL
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 视频提取文案
        /// </summary>
        public string extractContent { get; set; }

        /// <summary>
        /// 作者标识（user_id 或 influencer_id）
        /// </summary>
        public string authorId { get; set; }

        /// <summary>
        /// 作者昵称/名称 
        /// </summary>
        public string authorName { get; set; }

    }
}
