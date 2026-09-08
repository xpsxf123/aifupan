using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.bo;

namespace ReviewAnalysis.vo
{
    /// <summary>
    /// 直播间商品信息VO
    /// </summary>
    public class ProductInfoVo
    {
        /// <summary>
        /// 商品ID
        /// </summary>
        public string ProductId { get; set; }

        /// <summary>
        /// 商品标题
        /// </summary>
        public string Title { get; set; }

        /// <summary>
        /// 商品图片URL
        /// </summary>
        public string ImageUri { get; set; }

        /// <summary>
        /// 到手价（市场价）
        /// </summary>
        public decimal MarketPrice { get; set; }

        /// <summary>
        /// 价格文本描述
        /// </summary>
        public string PriceText { get; set; }

        /// <summary>
        /// 库存数量
        /// </summary>
        public int StockCnt { get; set; }

        /// <summary>
        /// 活动库存
        /// </summary>
        public int CampaignStockCnt { get; set; }

        /// <summary>
        /// 累计成交金额
        /// </summary>
        public decimal PayAmt { get; set; }

        /// <summary>
        /// 分钟最高成交金额
        /// </summary>
        public decimal AvgMaxPayAmtMin { get; set; }

        /// <summary>
        /// 近5分钟商品点击次数
        /// </summary>
        public int ProductClickCnt5Min { get; set; }

        /// <summary>
        /// 曝光-成交转化率
        /// </summary>
        public decimal ProductShowPayUcntRatio { get; set; }

        /// <summary>
        /// 未支付订单数
        /// </summary>
        public int UnpayCnt { get; set; }

        /// <summary>
        /// 讲解次数
        /// </summary>
        public int ExplainCnt { get; set; }

        /// <summary>
        /// 是否正在讲解
        /// </summary>
        public bool Explaining { get; set; }

        /// <summary>
        /// 直播间购物车序号
        /// </summary>
        public int RoomCartNum { get; set; }

        /// <summary>
        /// 店铺ID
        /// </summary>
        public long ShopId { get; set; }

        /// <summary>
        /// 推广ID
        /// </summary>
        public string PromotionId { get; set; }

        /// <summary>
        /// 商品上架时间
        /// </summary>
        public long ProductBindTime { get; set; }

        /// <summary>
        /// 数据获取时间
        /// </summary>
        public DateTime FetchTime { get; set; }

        /// <summary>
        /// 转换为上传服务器的商品详情实体
        /// </summary>
        /// <param name="batchNumber">归属批次号（必需）</param>
        /// <param name="videoId">视频唯一标识（必需）</param>
        /// <returns>ProductDetailsBo</returns>
        public bo.ProductDetailsBo ToProductDetailsBo(string batchNumber, string videoId)
        {
            return new bo.ProductDetailsBo
            {
                batchNumber = batchNumber,
                videoId = videoId,
                productId = this.ProductId,
                title = this.Title,
                imageUri = this.ImageUri,
                marketPrice = this.MarketPrice,
                productBindTime = this.ProductBindTime > 0 ? this.ProductBindTime.ToString() : null,
                explainCnt = this.ExplainCnt,
                productShowPayUcntRatio = this.ProductShowPayUcntRatio,
                payAmt = this.PayAmt,
                avgMaxPayAmtMin = this.AvgMaxPayAmtMin
            };
        }
    }
}
