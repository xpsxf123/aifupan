using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.anchor
{
    public class AnchorYesterdayRecordItemVo
    {
        /// <summary>
        /// 录制的时间
        /// </summary>
        public string recordDate { get; set; }
        /// <summary>
        /// 场观人数 -1：未开启数据看板 -2：视频时长不足50分钟
        /// </summary>
        public string observationNum { get; set; }
        /// <summary>
        /// 销售额范围区间-起始，单位：元 -1：未开启数据看板 -2：视频时长不足50分钟
        /// </summary>
        public int volumeStart { get; set; }
        /// <summary>
        /// 销售额范围区间-结束，单位：元 -1：未开启数据看板 -2：视频时长不足50分钟
        /// </summary>
        public int volumeEnd { get; set; }
        /// <summary>
        /// 录制的视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 直播场次号
        /// </summary>
        public string batchNumber { get; set; }
    }
}
