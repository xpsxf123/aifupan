using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.oceanEngineData
{
    /// <summary>
    /// 视频数据查看混淆视图对象
    /// 对应Java类: VideoDataViewingConfuseVo
    /// </summary>
    public class VideoDataViewingConfuseVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 用户id 
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

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
        /// 销售额区间范围-起始(单位:元)
        /// </summary>
        public int? volumeStart { get; set; }

        /// <summary>
        /// 销售额区间范围-结束(单位:元)
        /// </summary>
        public int? volumeEnd { get; set; }

        /// <summary>
        /// 销量区间范围-起始 
        /// </summary>
        public int? purchaseCountStart { get; set; }

        /// <summary>
        /// 销量区间范围-结束
        /// </summary>
        public int? purchaseCountEnd { get; set; }

        /// <summary>
        /// 客单价区间范围-起始（单位：元）
        /// </summary>
        public double? customerUnitPriceStart { get; set; }

        /// <summary>
        /// 客单价区间范围-结束（单位：元）
        /// </summary>
        public double? customerUnitPriceEnd { get; set; }

        /// <summary>
        /// uv价值区间范围-起始 
        /// </summary>
        public double? uvValueStart { get; set; }

        /// <summary>
        /// uv价值区间范围-结束 
        /// </summary>
        public double? uvValueEnd { get; set; }

        /// <summary>
        /// 带货转换率区间范围-起始 
        /// </summary>
        public double? goodsConvertRateStart { get; set; }

        /// <summary>
        /// 带货转换率区间范围-结束 
        /// </summary>
        public double? goodsConvertRateEnd { get; set; }

        /// <summary>
        /// 数据状态 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：自动生成但主播未下播 7：已收录但直播列表为空 8：正确数据整理中
        /// </summary>
        public int? dataStatus { get; set; }

        /// <summary>
        /// 关联的数据看盘id
        /// </summary>
        public long? videoDataViewingId { get; set; }

        /// <summary>
        /// 是否带货 0：否 1：是
        /// </summary>
        public int? isTakeProduct { get; set; }

        /// <summary>
        /// 直播场次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 请求id 
        /// </summary>
        public string requestId { get; set; }

        /// <summary>
        /// 主播抖音号 
        /// </summary>
        public string anchorNumber { get; set; }

        /// <summary>
        /// 数据抓取时间
        /// </summary>
        public string crawlTime { get; set; }

        /// <summary>
        /// 创建时间 
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 最后修改时间 
        /// </summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 是否已删除 
        /// </summary>
        public int? isDeleted { get; set; }

        /// <summary>
        /// 看播流量结构
        /// </summary>
        public string watchFlowList { get; set; }

        /// <summary>
        /// 成交流量结构
        /// </summary>
        public string payFlowList { get; set; }

        /// <summary>
        /// 成交用户画像 
        /// </summary>
        public string payUserPortrait { get; set; }

        /// <summary>
        /// 看播用户画像
        /// </summary>
        public string watchUserPortrait { get; set; }

        /// <summary>
        /// 巨量oss存储地址
        /// </summary>
        public string ossPath { get; set; }

        /// <summary>
        /// 数据来源类型 0：蝉妈妈 1：巨量百应
        /// </summary>
        public int? dataSourceType { get; set; }

        /// <summary>
        /// 主播secUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 千次观看成交金额范围-起始 （单位：元）
        /// </summary>
        public double? gpmStart { get; set; }

        /// <summary>
        /// 千次观看成交金额范围-结束 （单位：元）
        /// </summary>
        public double? gpmEnd { get; set; }

        /// <summary>
        /// 曝光-观看率
        /// </summary>
        public double? showWatchCntRatio { get; set; }

        /// <summary>
        /// ROI
        /// </summary>
        public double? roi { get; set; }

        /// <summary>
        /// 投放消耗（元）
        /// </summary>
        public double? launchRoiAmount { get; set; }

        /// <summary>
        /// 退款金额（元）
        /// </summary>
        public double? refundAmount { get; set; }

        /// <summary>
        /// 整体支付ROI
        /// </summary>
        public double? overallCostRoi { get; set; }

        /// <summary>
        /// 净成交ROI
        /// </summary>
        public double? netTransactionRoi { get; set; }
    }
}
