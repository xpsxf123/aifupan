using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    /// <summary>
    /// 巨量汇总数据
    /// </summary>
    public class JuliangGatherDataEntity
    {
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 主播secUid
        /// </summary>
        public string secUid { get; set; }
        /// <summary>
        /// 直播场次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 观看的流量结构信息
        /// </summary>
        public List<JuliangFlowSourceEntity> watchFlowList {  get; set; }
        /// <summary>
        /// 支付的流量结构信息
        /// </summary>
        public List<JuliangFlowSourceEntity> payFlowList { get; set; }

        /// <summary>
        /// 观看用户的用户画像
        /// </summary>
        public JuliangUserPortraitEntity watchUserPortrait { get; set; }
        /// <summary>
        /// 下单用户的用户画像
        /// </summary>
        public JuliangUserPortraitEntity payUserPortrait { get; set; }

        /// <summary>
        /// 总观看人次
        /// </summary>
        public int? totalWatchNum { get; set; }

        /// <summary>
        /// 平均在线人数
        /// </summary>
        public int? averageOnlineNum { get; set; }

        /// <summary>
        /// 平均停留时间(秒)
        /// </summary>
        public int? averageResidenceTime { get; set; }

        /// <summary>
        /// 新增粉丝数
        /// </summary>
        public int? incrementFollowerCount { get; set; }

        /// <summary>
        /// 粉丝转化率
        /// </summary>
        public double? convertFanRate { get; set; }

        /// <summary>
        /// 互动率
        /// </summary>
        public double? interactionPercent { get; set; }

        /// <summary>
        /// 销售额(单位:分)
        /// </summary>
        public int? volume { get; set; }

        /// <summary>
        /// 销量
        /// </summary>
        public int? purchaseCount { get; set; }

        /// <summary>
        /// 客单价 单位：分）
        /// </summary>
        public double? customerUnitPrice { get; set; }

        /// <summary>
        /// UV价值
        /// </summary>
        public double? uvValue { get; set; }

        /// <summary>
        /// 带货转换率
        /// </summary>
        public double? goodsConvertRate { get; set; }

        /// <summary>
        /// 是否带货 0：否 1：是
        /// </summary>
        public int? isTakeProduct { get; set; }

        /// <summary>
        /// 千次观看成交金额 （单位：分）
        /// </summary>
        public double? gpm { get; set; }

        /// <summary>
        /// 曝光-观看率(次数)
        /// </summary>
        public double? showWatchCntRatio { get; set; }
        /// <summary>
        /// 投放ROI金额（单位：分），对应服务端 launchRoiAmount
        /// </summary>
        public double? launchRoiAmount { get; set; }
        /// <summary>
        /// 退款金额（单位：分），对应服务端 refundAmount
        /// </summary>
        public double? refundAmount { get; set; }

        /// <summary>
        /// 整体支付ROI(投放ROI)
        /// </summary>
        public double? roi { get; set; }

        /// <summary>
        /// 整体消耗（API字段：stat_cost_for_roi2）
        /// </summary>
        public double? overallCostRoi { get; set; }

        /// <summary>
        /// 净成交ROI（API字段：total_prepay_and_pay_settle_realtime_roi2_1h）
        /// </summary>
        public double? netTransactionRoi { get; set; }

        /// <summary>
        /// 巨量oss实时数据存储地址
        /// </summary>
        public string ossPath { get; set; }

        /// <summary>
        /// 违规警告列表
        /// </summary>
        public List<JuliangViolationEntity> violationList { get; set; }

        /// <summary>
        /// 违规详情列表
        /// </summary>
        public List<JuliangViolationDetailEntity> violationDetailList { get; set; }

        /// <summary>
        /// 是否有违规 0无 1有
        /// </summary>
        public int? hasViolation { get; set; }
    }
}
