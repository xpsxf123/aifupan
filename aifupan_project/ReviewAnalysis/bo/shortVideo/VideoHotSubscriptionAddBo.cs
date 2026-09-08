using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    /// <summary>
    /// 视频热搜订阅添加业务对象 
    /// </summary>
    public class VideoHotSubscriptionAddBo
    {
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
        /// 分组ID 
        /// </summary>
        public long? groupId { get; set; }

        /// <summary>
        /// 点赞大于（订阅条件）
        /// </summary>
        public int? likeCountMin { get; set; }

        /// <summary>
        /// 是否启用自动同步文案: 0-否, 1-是
        /// </summary>
        public int? autoSyncEnabled { get; set; }

        /// <summary>
        /// 点赞大于（自动提取文案条件）
        /// </summary>
        public int? likeCountThreshold { get; set; }

        /// <summary>
        /// 自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月 
        /// </summary>
        public int? updateTimeCondition { get; set; }
    }
}
