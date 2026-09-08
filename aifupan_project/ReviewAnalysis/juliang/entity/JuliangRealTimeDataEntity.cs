using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    /// <summary>
    /// 巨量实时数据
    /// </summary>
    public class JuliangRealTimeDataEntity
    {
        /// <summary>
        /// 成交件数
        /// </summary>
        public int payComboCnt {  get; set; }
        /// <summary>
        /// 成交金额，单位：分
        /// </summary>
        public int payAmt { get; set; }
        /// <summary>
        /// 新增直播团人数
        /// </summary>
        public int fansClubJoinUcnt { get; set; }
        /// <summary>
        /// 新增粉丝数
        /// </summary>
        public int followAnchorUcnt { get; set; }
        /// <summary>
        /// 观看人次
        /// </summary>
        public int watchNum { get; set; }
        /// <summary>
        /// 采集时间时间戳
        /// </summary>
        public long gatherTimeStamp { get; set; }
        /// <summary>
        /// 采集时间
        /// </summary>
        public string gatherDateTime { get; set; }
        /// <summary>
        /// 整体支付ROI
        /// </summary>
        public decimal totalRoi { get; set; }
        /// <summary>
        /// 千川消耗（投放金额）
        /// </summary>
        public decimal qianchuanCost { get; set; }
        /// <summary>
        /// 退款金额（单位：分）
        /// </summary>
        public decimal refundAmt { get; set; }

    }
}
