using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo
{
    /// <summary>
    /// 直播间商品详情-上传服务器实体类
    /// </summary>
    public class ProductDetailsBo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 归属批次号（必需）
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 视频唯一标识（必需）
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 商品ID（必需）
        /// </summary>
        public string productId { get; set; }

        /// <summary>
        /// 商品标题（必需）
        /// </summary>
        public string title { get; set; }

        /// <summary>
        /// 商品图片URL
        /// </summary>
        public string imageUri { get; set; }

        /// <summary>
        /// 到手价（元）
        /// </summary>
        public decimal? marketPrice { get; set; }

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
        public long? explainCnt { get; set; }

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
        public decimal? productShowClickUcntRatio { get; set; }

        /// <summary>
        /// 曝光-成交转化率
        /// </summary>
        public decimal? productShowPayUcntRatio { get; set; }

        /// <summary>
        /// 点击-成交转化率
        /// </summary>
        public decimal? productClickPayUcntRatio { get; set; }

        /// <summary>
        /// 商品千次曝光成交金额（元）
        /// </summary>
        public decimal? gpm { get; set; }

        /// <summary>
        /// 累计成交金额（元）
        /// </summary>
        public decimal? payAmt { get; set; }

        /// <summary>
        /// 分钟最高成交金额（元）
        /// </summary>
        public decimal? avgMaxPayAmtMin { get; set; }

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
        public decimal? createPayUcntRatio { get; set; }

        /// <summary>
        /// 预售订单数
        /// </summary>
        public long? payDepositPreOrderCnt { get; set; }

        /// <summary>
        /// 预售定金金额（元）
        /// </summary>
        public decimal? presaleDepayDeamt { get; set; }

        /// <summary>
        /// 预售全款金额（元）
        /// </summary>
        public decimal? payDepositPreOrderAmt { get; set; }

        /// <summary>
        /// 退款订单数
        /// </summary>
        public long? refundCnt { get; set; }

        /// <summary>
        /// 退款金额（元）
        /// </summary>
        public decimal? realRefundAmt { get; set; }

        /// <summary>
        /// 退款率
        /// </summary>
        public decimal? refundRate { get; set; }

        /// <summary>
        /// 统计曲线
        /// </summary>
        public string statisticsCurve { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public string updateDate { get; set; }
    }
}
