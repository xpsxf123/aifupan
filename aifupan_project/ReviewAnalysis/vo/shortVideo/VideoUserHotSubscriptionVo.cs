using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    /// <summary>
    /// 用户视频热搜订阅实体（转换时间：2025-09-02 16:17）
    /// </summary>
    public class VideoUserHotSubscriptionVo
    {
        /// <summary>
        /// 主键ID（雪花ID）
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 用户ID
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户ID
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 分组ID（null表示虚拟默认分组）
        /// </summary>
        public long? groupId { get; set; }

        /// <summary>
        /// 关键词
        /// </summary>
        public string keyword { get; set; }

        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 行业ID 
        /// </summary>
        public long? industryId { get; set; }

        /// <summary>
        /// 订阅点赞阈值 
        /// </summary>
        public int? subscriptionLikeCountThreshold { get; set; }

        /// <summary>
        /// 是否启用自动同步文案: 0-否, 1-是 
        /// </summary>
        public int? isEnabled { get; set; }

        /// <summary>
        /// 自动提取文案点赞阈值
        /// </summary>
        public int? likeCountThreshold { get; set; }

        /// <summary>
        /// 作品更新时间阈值: 0-全部,1-近一周,2-近半月,3-近一月,4-近三月,5-近六月 
        /// </summary>
        public int? updateTimeCondition { get; set; }

        /// <summary>
        /// 监控频率(小时)
        /// </summary>
        public int? monitorFrequency { get; set; }

        /// <summary>
        /// 最后同步时间（格式：yyyy-MM-dd HH:mm:ss）
        /// </summary>
        public string lastSyncTime { get; set; }

        /// <summary>
        /// 创建时间（格式：yyyy-MM-dd HH:mm:ss）
        /// </summary>
        public string createdDate { get; set; }

        /// <summary>
        /// 更新时间（格式：yyyy-MM-dd HH:mm:ss）
        /// </summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 是否删除: 0-未删除, 1-已删除
        /// </summary>
        public int? isDeleted { get; set; }
    }
}
