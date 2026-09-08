using System;
using System.Collections.Generic;
using System.Linq;
using Newtonsoft.Json;

namespace ReviewAnalysis.plugins.models
{
    /// <summary>
    /// 统一数据上传模型
    /// 用于所有平台数据的统一格式上传
    /// </summary>
    public class UnifiedDataModel
    {
        /// <summary>
        /// 场观（累计观看人数）
        /// </summary>
        public long? viewCount { get; set; }

        /// <summary>
        /// 销售额（累计，单位：元）
        /// </summary>
        public double? salesRevenue { get; set; }

        /// <summary>
        /// 退款（累计，单位：元）
        /// </summary>
        public double? refund { get; set; }

        /// <summary>
        /// 投放（累计，单位：元）
        /// </summary>
        public double? investment { get; set; }

        /// <summary>
        /// 退款数量（累计）
        /// </summary>
        public int? refundQuantity { get; set; }

        /// <summary>
        /// 销售单量
        /// </summary>
        public int? payComboCnt { get; set; }

        /// <summary>
        /// 净销售额（累计，单位：元）
        /// 计算公式：净销售额 = 销售额 - 退款
        /// </summary>
        public double? netSales { get; set; }

        /// <summary>
        /// 退款率（精确到小数点后两位）
        /// 计算公式：退款率 = 退款数量 / 销售单量 * 100%
        /// </summary>
        public double? refundRate { get; set; }

        /// <summary>
        /// 投资回报率（精确到小数点后两位）
        /// 计算公式：ROI = 销售额 / 投放 * 100%
        /// </summary>
        public double? roi { get; set; }

        /// <summary>
        /// 千次成交（精确到小数点后两位）
        /// 计算公式：千次成交 = 成交金额 / 场观 * 1000
        /// </summary>
        public double? thousandSales { get; set; }

        /// <summary>
        /// 曝光次数
        /// </summary>
        public long? exposureCount { get; set; }

        /// <summary>
        /// 涨粉人数
        /// </summary>
        public int? followCount { get; set; }

        /// <summary>
        /// 点击-成交率（精确到小数点后两位）
        /// 计算公式：点击-成交率 = 区间订单量 / 区间点击量
        /// </summary>
        public double? clickPaymentRate { get; set; }
        
        /// <summary>
        /// 曝光-观看率(次数)
        /// </summary>
        public double? showWatchCntRatio { get; set; }

        /// <summary>
        /// 互动率（精确到小数点后两位）
        /// 计算公式：互动率 = 区间互动次数 / 区间观看人数
        /// </summary>
        public double? interactionRate { get; set; }

        /// <summary>
        /// 最高在线
        /// </summary>
        public int? maxOnline { get; set; }

        /// <summary>
        /// 带货转化率（精确到小数点后两位）
        /// 计算公式：带货转化率 = 成交单量 / 观看人数 * 100%
        /// </summary>
        public double? conversionRate { get; set; }

        /// <summary>
        /// UV价值（单位：元）
        /// 计算公式：UV价值 = 销售额 / 观看人数
        /// </summary>
        public double? uvValue { get; set; }

        /// <summary>
        /// 涨粉率（精确到小数点后两位）
        /// 计算公式：涨粉率 = 涨粉人数 / 观看人数 * 100%
        /// </summary>
        public double? followRate { get; set; }

        /// <summary>
        /// 直播批次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 视频开始时间
        /// </summary>
        public string startTime { get; set; }

        /// <summary>
        /// 视频结束时间
        /// </summary>
        public string endTime { get; set; }

        /// <summary>
        /// 主播 secUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 平台类型：0-抖音，1-快手，2-视频号
        /// </summary>
        public int platform { get; set; }

        /// <summary>
        /// 直播过程数据
        /// </summary>
        public List<OceanEngineProcessBo> oceanEngineProcessList { get; set; }

        /// <summary>
        /// 商品列表
        /// </summary>
        public List<VideoProductRequest> productList { get; set; }

        /// <summary>
        /// 平均在线人数（从巨量 online_user_cnt 取值）
        /// </summary>
        public int? averageOnlineNum { get; set; }

        /// <summary>
        /// 整体消耗（单位：元）
        /// </summary>
        public double? overallCostRoi { get; set; }

        /// <summary>
        /// 净成交ROI（精确到小数点后两位）
        /// </summary>
        public double? netTransactionRoi { get; set; }

        /// <summary>
        /// 平均停留时长/秒（从巨量 avg_watch_duration 取值）
        /// </summary>
        public int? averageResidenceTime { get; set; }

        /// <summary>
        /// 客单价（单位：元）= 销售额 / 成交单量
        /// </summary>
        public double? customerUnitPrice { get; set; }

        /// <summary>
        /// 计算衍生字段
        /// </summary>
        public void CalculateDerivedFields()
        {
            // 当 API 返回的 salesRevenue 为 0 但商品有销售数据时，从商品汇总补充
            if ((salesRevenue??0) <= 0 && productList != null && productList.Count > 0)
            {
                var productTotal = productList.Sum(p => p.payAmt ?? 0);
                if (productTotal > 0)
                {
                    salesRevenue = productTotal;
                }
            }

            // 当 API 返回的 refundQuantity 为 null/0 但商品有退款数据时，从商品汇总补充
            if ((refundQuantity ?? 0) <= 0 && productList != null && productList.Count > 0)
            {
                var refundTotal = productList.Sum(p => p.refundCnt ?? 0);
                if (refundTotal > 0)
                {
                    refundQuantity = (int)refundTotal;
                }
            }

            // 净销售额 = 销售额 - 退款
            if ((netSales??0) <= 0 && salesRevenue.HasValue)
            {
                netSales = salesRevenue.Value - (refund ?? 0);
            }

            // 退款率 = 退款数量 / 销售单量 * 100%
            if ((refundRate??0) <= 0 && refundQuantity.HasValue && payComboCnt.HasValue && payComboCnt.Value > 0)
            {
                refundRate = Math.Round((double)refundQuantity.Value / payComboCnt.Value * 100, 2);
            }

            // ROI = 销售额 / 投放 * 100%
            if ((roi??0) <= 0 && salesRevenue.HasValue && investment.HasValue && investment.Value > 0)
            {
                roi = Math.Round(salesRevenue.Value / investment.Value, 2);
            }

            // 千次成交 = 成交金额 / 场观 * 1000
            if ((thousandSales??0) <= 0 && salesRevenue.HasValue && viewCount.HasValue && viewCount.Value > 0)
            {
                thousandSales = Math.Round(salesRevenue.Value / viewCount.Value * 1000, 2);
            }

            // 带货转化率 = 成交单量 / 观看人数 * 100%
            if ((conversionRate??0) <= 0 && payComboCnt.HasValue && viewCount.HasValue && viewCount.Value > 0)
            {
                conversionRate = Math.Round((double)payComboCnt.Value / viewCount.Value * 100, 2);
            }

            // UV价值 = 销售额 / 观看人数
            if ((uvValue??0) <= 0 && salesRevenue.HasValue && viewCount.HasValue && viewCount.Value > 0)
            {
                uvValue = Math.Round(salesRevenue.Value / viewCount.Value, 2);
            }

            // 客单价 = 销售额 / 成交单量
            if ((customerUnitPrice??0) <= 0 && salesRevenue.HasValue && payComboCnt.HasValue && payComboCnt.Value > 0)
            {
                customerUnitPrice = Math.Round(salesRevenue.Value / payComboCnt.Value, 2);
            }

            // 涨粉率 = 涨粉人数 / 观看人数 * 100%
            if ((followRate??0) <= 0 && followCount.HasValue && viewCount.HasValue && viewCount.Value > 0)
            {
                followRate = Math.Round((double)followCount.Value / viewCount.Value * 100, 2);
            }
        }
    }

    /// <summary>
    /// 直播过程数据
    /// </summary>
    public class OceanEngineProcessBo
    {
        /// <summary>
        /// 采集时间-格式为 yyyy-MM-dd HH:mm:ss
        /// </summary>
        public string gatherDateTime { get; set; }

        /// <summary>
        /// 总观看人次
        /// </summary>
        public long? viewCount { get; set; }

        /// <summary>
        /// 成交金额 单位：元
        /// </summary>
        public double? salesRevenue { get; set; }

        /// <summary>
        /// 退款金额 单位：元
        /// </summary>
        public double? refund { get; set; }

        /// <summary>
        /// 投放金额 单位：元
        /// </summary>
        public double? investment { get; set; }

        /// <summary>
        /// 新增直播团人数
        /// </summary>
        public int? fansClubJoinUcnt { get; set; }

        /// <summary>
        /// 新增粉丝数（原followAnchorUcnt，与统一模型对齐）
        /// </summary>
        public int? followCount { get; set; }

        /// <summary>
        /// 成交单量
        /// </summary>
        public int? payComboCnt { get; set; }

        /// <summary>
        /// 退款数量（累计）
        /// </summary>
        public int? refundQuantity { get; set; }

        /// <summary>
        /// 曝光次数
        /// </summary>
        public long? exposureCount { get; set; }

        /// <summary>
        /// 在线人数
        /// </summary>
        public int? onlineCount { get; set; }

        /// <summary>
        /// 点击成交率（精确到小数点后两位）
        /// </summary>
        public double? clickPaymentRate { get; set; }

        /// <summary>
        /// 互动率（精确到小数点后两位）
        /// </summary>
        public double? interactionRate { get; set; }
    }

    /// <summary>
    /// 商品数据
    /// </summary>
    public class VideoProductRequest
    {
        /// <summary>
        /// 直播批次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 商品ID（爱复盘的商品ID）
        /// </summary>
        public string productId { get; set; }

        /// <summary>
        /// 商品标题
        /// </summary>
        public string title { get; set; }

        /// <summary>
        /// 商品图片URL
        /// </summary>
        public string imageUri { get; set; }

        /// <summary>
        /// 到手价（元）
        /// </summary>
        public double? marketPrice { get; set; }

        /// <summary>
        /// 直播间上架时间
        /// </summary>
        public string productBindTime { get; set; }

        /// <summary>
        /// 直播间下架时间
        /// </summary>
        public string productDownTime { get; set; }

        /// <summary>
        /// 讲解次数
        /// </summary>
        public int? explainCnt { get; set; }

        /// <summary>
        /// 商品曝光人数
        /// </summary>
        public long? productShowUcnt { get; set; }

        /// <summary>
        /// 商品点击人数
        /// </summary>
        public long? productClickUcnt { get; set; }

        /// <summary>
        /// 曝光-点击转化率
        /// </summary>
        public double? productShowClickUcntRatio { get; set; }

        /// <summary>
        /// 曝光-成交转化率
        /// </summary>
        public double? productShowPayUcntRatio { get; set; }

        /// <summary>
        /// 点击-成交转化率
        /// </summary>
        public double? productClickPayUcntRatio { get; set; }

        /// <summary>
        /// 商品千次曝光成交金额（元）
        /// </summary>
        public double? gpm { get; set; }

        /// <summary>
        /// 累计成交金额（元）
        /// </summary>
        public double? payAmt { get; set; }

        /// <summary>
        /// 分钟最高成交金额（元）
        /// </summary>
        public double? avgMaxPayAmtMin { get; set; }

        /// <summary>
        /// 累计成交件数
        /// </summary>
        public long? payComboCnt { get; set; }

        /// <summary>
        /// 累计成交订单数
        /// </summary>
        public long? payCnt { get; set; }

        /// <summary>
        /// 创建订单数
        /// </summary>
        public long? createCnt { get; set; }

        /// <summary>
        /// 订单支付率
        /// </summary>
        public double? createPayUcntRatio { get; set; }

        /// <summary>
        /// 预售订单数
        /// </summary>
        public long? payDepositPreOrderCnt { get; set; }

        /// <summary>
        /// 预售定金金额（元）
        /// </summary>
        public double? presaleDepayDeamt { get; set; }

        /// <summary>
        /// 预售全款金额（元）
        /// </summary>
        public double? payDepositPreOrderAmt { get; set; }

        /// <summary>
        /// 退款订单数
        /// </summary>
        public long? refundCnt { get; set; }

        /// <summary>
        /// 退款金额（元）
        /// </summary>
        public double? realRefundAmt { get; set; }

        /// <summary>
        /// 退款率
        /// </summary>
        public double? refundRate { get; set; }

        /// <summary>
        /// 曝光观看率 — 商品曝光人数/直播间观看人数
        /// </summary>
        public double? productViewShowRatio { get; set; }

        /// <summary>
        /// 成交单价（元）
        /// </summary>
        public double? avgPayAmtPerOrder { get; set; }

        /// <summary>
        /// 排序，从1开始
        /// </summary>
        public int? sort { get; set; }

        /// <summary>
        /// 商品的过程数据
        /// </summary>
        public List<CommodityProcessDataBo> commodityProcessDataList { get; set; }
    }

    /// <summary>
    /// 商品过程数据
    /// </summary>
    public class CommodityProcessDataBo
    {
        /// <summary>
        /// 日期时间
        /// </summary>
        public string dateTime { get; set; }

        /// <summary>
        /// 商品曝光人数
        /// </summary>
        public long? productShowUcnt { get; set; }

        /// <summary>
        /// 商品点击人数
        /// </summary>
        public long? productClickUcnt { get; set; }

        /// <summary>
        /// 直播间累计观看人数
        /// </summary>
        public int? viewCount { get; set; }

        /// <summary>
        /// 讲解次数
        /// </summary>
        public int? explainCnt { get; set; }

        /// <summary>
        /// 成交件数
        /// </summary>
        public int? payComboCnt { get; set; }

        /// <summary>
        /// 成交金额（元）
        /// </summary>
        public double? payAmt { get; set; }

        /// <summary>
        /// 商品千次曝光成交金额（元）
        /// </summary>
        public double? gpm { get; set; }

        /// <summary>
        /// 分钟最高成交金额（元）
        /// </summary>
        public double? avgMaxPayAmtMin { get; set; }

        /// <summary>
        /// 创建订单数
        /// </summary>
        public long? createCnt { get; set; }

        /// <summary>
        /// 订单支付率
        /// </summary>
        public double? createPayUcntRatio { get; set; }

        /// <summary>
        /// 预售订单数
        /// </summary>
        public long? payDepositPreOrderCnt { get; set; }

        /// <summary>
        /// 预售定金金额（元）
        /// </summary>
        public double? presaleDepayDeamt { get; set; }

        /// <summary>
        /// 预售全款金额（元）
        /// </summary>
        public double? payDepositPreOrderAmt { get; set; }

        /// <summary>
        /// 退款订单数
        /// </summary>
        public long? refundCnt { get; set; }

        /// <summary>
        /// 退款金额（元）
        /// </summary>
        public double? realRefundAmt { get; set; }
    }
}
