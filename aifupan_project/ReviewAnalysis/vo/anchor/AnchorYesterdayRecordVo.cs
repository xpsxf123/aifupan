using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.anchor
{
    public class AnchorYesterdayRecordVo
    {
        /// <summary>
        /// 主播secUid
        /// </summary>
        public string secUid {  get; set; }
        /// <summary>
        /// 昨日录制列表
        /// </summary>
        public List<AnchorYesterdayRecordItemVo> yesterdayRecordList { get; set; }
        /// <summary>
        /// 昨日录制数量
        /// </summary>
        public int yesterdayRecordNum { get; set; }
        /// <summary>
        /// 昨日平均场观
        /// </summary>
        public int yesterdayAverageObservationNum { get; set; }
        /// <summary>
        /// 昨日平均销售额区间范围-起始
        /// </summary>
        public int yesterdayAverageVolumeStart { get; set; }
        /// <summary>
        /// 昨日平均销售额区间范围-结束
        /// </summary>
        public int yesterdayAverageVolumeEnd { get; set; }
        /// <summary>
        /// 前日录制列表
        /// </summary>
        public List<AnchorYesterdayRecordItemVo> dayBeforeRecordList { get; set; }

        /// <summary>
        /// 前日录制数量
        /// </summary>
        public int? dayBeforeRecordNum { get; set; }

        /// <summary>
        /// 前日平均场观
        /// </summary>
        public int? dayBeforeAverageObservationNum { get; set; }
        /// <summary>
        /// 前日平均销售额区间范围-起始
        /// </summary>
        public int? dayBeforeAverageVolumeStart { get; set; }
        /// <summary>
        /// 前日平均销售额区间范围-结束
        /// </summary>
        public int? dayBeforeAverageVolumeEnd { get; set; }
    }
}
