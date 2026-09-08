using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    public class UserInfluencerSubscriptionVo
    {
        /// <summary>
        /// 主键ID，雪花算法生成 
        /// 对应字段: id 
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 用户ID 
        /// 对应字段: user_id
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户ID 
        /// 对应字段: tenant_id
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 分组ID，逻辑关联tb_video_user_subscription_group.id ，null表示虚拟默认分组
        /// 对应字段: group_id 
        /// </summary>
        public long? groupId { get; set; }

        /// <summary>
        /// 达人ID，逻辑关联tb_video_influencer.id  
        /// 对应字段: influencer_id
        /// </summary>
        public long? influencerId { get; set; }

        /// <summary>
        /// 行业ID 
        /// 对应字段: industry_id
        /// </summary>
        public long? industryId { get; set; }

        /// <summary>
        /// 是否启用自动提取文案: 0-否, 1-是
        /// 对应字段: is_enabled
        /// </summary>
        public int? isEnabled { get; set; } = 0;

        /// <summary>
        /// 自动提取文案点赞阈值
        /// 对应字段: like_count_threshold 
        /// </summary>
        public int? likeCountThreshold { get; set; }

        /// <summary>
        /// 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月 
        /// 对应字段: update_time_condition
        /// </summary>
        public int? updateTimeCondition { get; set; } = 0;

        /// <summary>
        /// 监控频率（小时），0表示不监控 
        /// 对应字段: monitor_frequency 
        /// </summary>
        public int? monitorFrequency { get; set; } = 24;

        /// <summary>
        /// 最后同步时间
        /// 对应字段: last_sync_time 
        /// </summary>
        public DateTime? lastSyncTime { get; set; }

        /// <summary>
        /// 创建时间
        /// 对应字段: created_date 
        /// </summary>
        public DateTime createdDate { get; set; } = DateTime.Now;

        /// <summary>
        /// 更新时间 
        /// 对应字段: update_date
        /// </summary>
        public DateTime updateDate { get; set; } = DateTime.Now;

        /// <summary>
        /// 对应的达人表
        /// </summary>
        public VideoInfluencerInfoVo videoInfluencerInfoVo { get; set; }
    }
}
