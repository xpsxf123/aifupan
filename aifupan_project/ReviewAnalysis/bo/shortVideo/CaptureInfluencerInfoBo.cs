using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    public class CaptureInfluencerInfoBo
    {
        /// <summary>
        /// 搜索关键词（抖音号/昵称）
        /// </summary>
        public string searchKeyword { get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public long? tradeId { get; set; }

        /// <summary>
        /// 分组id
        /// </summary>
        public long? groupId { get; set; }

        /// <summary>
        /// 点赞数大于
        /// </summary>
        public int? likeUpwards { get; set; }
    }
}
