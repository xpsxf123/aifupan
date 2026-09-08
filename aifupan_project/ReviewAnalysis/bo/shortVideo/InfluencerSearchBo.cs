using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    /// <summary>
    /// 达人搜索业务对象
    /// </summary>
    public class InfluencerSearchBo
    {
        /// <summary>
        /// 搜索关键词（抖音号/昵称）
        /// </summary>
        public string searchKeyword { get; set; }

        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 搜索结果达人列表
        /// </summary>
        public List<InfluencerSearchInfoBo> influencerList { get; set; }
    }
}
