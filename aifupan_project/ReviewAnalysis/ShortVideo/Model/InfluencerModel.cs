using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.Model
{
    public class InfluencerModel
    {
        /// <summary>
        /// 达人id
        /// </summary>
        public long influencerId { get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 达人的secUid
        /// </summary>
        public string platformUserId { get; set; }

        /// <summary>
        /// 账号
        /// </summary>
        public string platformAccount {  get; set; }

        /// <summary>
        /// 最近同步时间
        /// </summary>
        public string lastSyncTime { get; set; }

        /// <summary>
        /// 处理状态 0带处理，1处理中，2处理完成, 3处理失败
        /// </summary>
        public int status { get; set; } = 0;

        /// <summary>
        /// 错误原因
        /// </summary>
        public string errorReason { get; set; }

        /// <summary>
        /// 是否启用自动提取文案: 0-否, 1-是
        /// 对应字段: is_enabled
        /// </summary>
        public int isEnabled { get; set; } = 0;

        /// <summary>
        /// 自动提取文案点赞阈值
        /// 对应字段: like_count_threshold 
        /// </summary>
        public int? likeCountThreshold { get; set; }

        /// <summary>
        /// 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月 
        /// 对应字段: update_time_condition
        /// </summary>
        public int updateTimeCondition { get; set; } = 0;

        /// <summary>
        /// 监控频率（小时），0表示不监控 
        /// 对应字段: monitor_frequency 
        /// </summary>
        public int monitorFrequency { get; set; } = 24;


        /// <summary>
        /// 重写Equals方法，对比是否是同一个对象
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public override bool Equals(object obj)
        {
            if (obj == null || GetType() != obj.GetType()) return false;
            return influencerId == ((InfluencerModel)obj).influencerId;
        }
    }
}
