using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    public class VideoUserHotSubscriptionAllVo
    {

        /// <summary>
        /// 订阅ID
        /// </summary>
        public long? id;

        /// <summary>
        /// 用户ID
        /// </summary>
        public long? userId;

        /// <summary>
        /// 租户ID
        /// </summary>
        public long? tenantId;

        /// <summary>
        /// 分组ID 逻辑关联tb_video_user_subscription_group.id，null表示虚拟默认分组
        /// </summary>
        public long? groupId;

        /// <summary>
        /// 关键词
        /// </summary>
        public string keyword;

        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号
        /// </summary>
        public int? platformType;

        /// <summary>
        /// 行业ID
        /// </summary>
        public long? industryId;

        /// <summary>
        /// 订阅点赞阈值
        /// </summary>
        public int? subscriptionLikeCountThreshold;

        /// <summary>
        /// 是否启用自动同步文案: 0-否, 1-是
        /// </summary>
        public int? isEnabled;

        /// <summary>
        /// 自动提取文案点赞阈值
        /// </summary>
        public int? likeCountThreshold;

        /// <summary>
        /// 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月
        /// </summary>
        public int? updateTimeCondition;

        /// <summary>
        /// 监控频率(小时)
        /// </summary>
        public int? monitorFrequency;

        /// <summary>
        /// 最后同步时间
        /// </summary>
        public string lastSyncTime;

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createdDate;

        /// <summary>
        /// 更新时间
        /// </summary>
        public string updateDate;

    }
}
