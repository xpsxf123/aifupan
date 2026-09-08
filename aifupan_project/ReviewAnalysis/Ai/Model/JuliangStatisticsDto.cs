using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    public class JuliangStatisticsDto
    {
        /// <summary>
        /// 视频开始时间 自然时间戳
        /// </summary>
        public long startVideoTime { get; set; }

        /// <summary>
        /// 成交件数
        /// </summary>
        public int payComboCntMin { get; set; }

        /// <summary>
        /// 成交件数
        /// </summary>
        public int payComboCntMax { get; set; }

        /// <summary>
        /// 一段时间内的成交件数
        /// </summary>
        public int rangePayComboCnt { get; set; }

        /// <summary>
        /// 成交金额，单位：分
        /// </summary>
        public int payAmtMin { get; set; }

        /// <summary>
        /// 成交金额，单位：分
        /// </summary>
        public int payAmtMax { get; set; }

        /// <summary>
        /// 一段时间内成交金额，单位：分
        /// </summary>
        public int rangePayAmt { get; set; }

        /// <summary>
        /// 新增直播团人数
        /// </summary>
        public int fansClubJoinUcntMin { get; set; }

        /// <summary>
        /// 新增直播团人数
        /// </summary>
        public int fansClubJoinUcntMax { get; set; }

        /// <summary>
        /// 一段时间内新增直播团人数
        /// </summary>
        public int rangeFansClubJoinUcnt { get; set; }

        /// <summary>
        /// 新增粉丝数
        /// </summary>
        public int followAnchorUcntMin { get; set; }

        /// <summary>
        /// 新增粉丝数
        /// </summary>
        public int followAnchorUcntMax { get; set; }

        /// <summary>
        /// 一段时间内新增粉丝数
        /// </summary>
        public int rangeFollowAnchorUcnt { get; set; }

        /// <summary>
        /// 观看人次
        /// </summary>
        public int watchNumMin { get; set; }

        /// <summary>
        /// 观看人次
        /// </summary>
        public int watchNumMax { get; set; }

        /// <summary>
        /// 一段时间内观看人次
        /// </summary>
        public int rangeWatchNum { get; set; }

        /// <summary>
        /// 千川消耗（投放金额）
        /// </summary>
        public decimal qianchuanCostMin { get; set; }

        /// <summary>
        /// 千川消耗（投放金额）
        /// </summary>
        public decimal qianchuanCostMax { get; set; }

        /// <summary>
        /// 一段时间内千川消耗
        /// </summary>
        public decimal rangeQianchuanCost { get; set; }

        /// <summary>
        /// 退款金额（单位：分）
        /// </summary>
        public decimal refundAmtMin { get; set; }

        /// <summary>
        /// 退款金额（单位：分）
        /// </summary>
        public decimal refundAmtMax { get; set; }

        /// <summary>
        /// 一段时间内退款金额
        /// </summary>
        public decimal rangeRefundAmt { get; set; }

        /// <summary>
        /// 整体支付ROI
        /// </summary>
        public decimal totalRoiMin { get; set; }

        /// <summary>
        /// 整体支付ROI
        /// </summary>
        public decimal totalRoiMax { get; set; }
    }
}
