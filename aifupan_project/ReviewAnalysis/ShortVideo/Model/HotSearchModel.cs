using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.Model
{
    public class HotSearchModel
    {

        public long hotSearchId { get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 爆款关键词
        /// </summary>
        public string searchKeyword { get; set; }

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
        /// 重写Equals方法，对比是否是同一个对象
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public override bool Equals(object obj)
        {
            if (obj == null || GetType() != obj.GetType()) return false;
            return hotSearchId == ((HotSearchModel)obj).hotSearchId;
        }
    }
}
