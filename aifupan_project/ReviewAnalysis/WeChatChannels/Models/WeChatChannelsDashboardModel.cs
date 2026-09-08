using Newtonsoft.Json;
using System;
using System.Collections.Generic;

namespace ReviewAnalysis.WeChatChannels.Models
{
    /// <summary>
    /// 微信视频号直播大屏模型
    /// </summary>
    public class WeChatChannelsDashboardModel
    {
        /// <summary>
        /// 直播数据总览(数据看板)
        /// </summary>
        [JsonProperty("dataViewing", NullValueHandling = NullValueHandling.Ignore)]
        public LiveVideoDataViewingResponse DataViewing { get; set; }

        /// <summary>
        /// 直播数据分钟级行数据
        /// </summary>
        [JsonProperty("minuteRows", NullValueHandling = NullValueHandling.Ignore)]
        public List<LiveDashboardMinuteResponse> MinuteRows { get; set; }
    }

    /// <summary>
    /// 直播数据总览(数据看板)
    /// </summary>
    public class LiveVideoDataViewingResponse
    {
        public string VideoId { get; set; }

        public string OssPath { get; set; }

        /// <summary>
        /// 主播抖音号
        /// </summary>
        [JsonProperty("anchorNumber", NullValueHandling = NullValueHandling.Ignore)]
        public string AnchorNumber { get; set; }

        /// <summary>
        /// 平均在线人数
        /// </summary>
        [JsonProperty("averageOnlineNum", NullValueHandling = NullValueHandling.Ignore)]
        public int? AverageOnlineNum { get; set; }

        /// <summary>
        /// 平均停留时间(秒)
        /// </summary>
        [JsonProperty("averageResidenceTime", NullValueHandling = NullValueHandling.Ignore)]
        public int? AverageResidenceTime { get; set; }

        /// <summary>
        /// 直播ID
        /// </summary>
        [JsonProperty("batchNumber", NullValueHandling = NullValueHandling.Ignore)]
        public string BatchNumber { get; set; }

        /// <summary>
        /// 粉丝转化率
        /// </summary>
        [JsonProperty("convertFanRate", NullValueHandling = NullValueHandling.Ignore)]
        public double? ConvertFanRate { get; set; }

        /// <summary>
        /// 数据抓取时间
        /// </summary>
        [JsonProperty("crawlTime", NullValueHandling = NullValueHandling.Ignore)]
        public string CrawlTime { get; set; }

        /// <summary>
        /// 客单价区间范围-结束（单位：元）
        /// </summary>
        [JsonProperty("customerUnitPriceEnd", NullValueHandling = NullValueHandling.Ignore)]
        public double? CustomerUnitPriceEnd { get; set; }

        /// <summary>
        /// 客单价区间范围-起始（单位：元）
        /// </summary>
        [JsonProperty("customerUnitPriceStart", NullValueHandling = NullValueHandling.Ignore)]
        public double? CustomerUnitPriceStart { get; set; }

        /// <summary>
        /// 带货转换率区间范围-结束
        /// </summary>
        [JsonProperty("goodsConvertRateEnd", NullValueHandling = NullValueHandling.Ignore)]
        public double? GoodsConvertRateEnd { get; set; }

        /// <summary>
        /// 带货转换率区间范围-起始
        /// </summary>
        [JsonProperty("goodsConvertRateStart", NullValueHandling = NullValueHandling.Ignore)]
        public double? GoodsConvertRateStart { get; set; }

        /// <summary>
        /// 千次观看成交金额范围-结束 （单位：元）
        /// </summary>
        [JsonProperty("gpmEnd", NullValueHandling = NullValueHandling.Ignore)]
        public double? GpmEnd { get; set; }

        /// <summary>
        /// 千次观看成交金额范围-起始 （单位：元）
        /// </summary>
        [JsonProperty("gpmStart", NullValueHandling = NullValueHandling.Ignore)]
        public double? GpmStart { get; set; }

        /// <summary>
        /// 新增粉丝数
        /// </summary>
        [JsonProperty("incrementFollowerCount", NullValueHandling = NullValueHandling.Ignore)]
        public int? IncrementFollowerCount { get; set; }

        /// <summary>
        /// 互动率
        /// </summary>
        [JsonProperty("interactionPercent", NullValueHandling = NullValueHandling.Ignore)]
        public double? InteractionPercent { get; set; }

        /// <summary>
        /// 是否带货 0：否 1：是
        /// </summary>
        [JsonProperty("isTakeProduct", NullValueHandling = NullValueHandling.Ignore)]
        public int? IsTakeProduct { get; set; }

        /// <summary>
        /// 成交流量结构
        /// </summary>
        [JsonProperty("payFlowList", NullValueHandling = NullValueHandling.Ignore)]
        public PayFlowStructure[] PayFlowList { get; set; }

        /// <summary>
        /// 成交用户画像
        /// </summary>
        [JsonProperty("payUserPortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PayUserPortraitData PayUserPortrait { get; set; }

        /// <summary>
        /// 销量区间范围-结束
        /// </summary>
        [JsonProperty("purchaseCountEnd", NullValueHandling = NullValueHandling.Ignore)]
        public int? PurchaseCountEnd { get; set; }

        /// <summary>
        /// 销量区间范围-起始
        /// </summary>
        [JsonProperty("purchaseCountStart", NullValueHandling = NullValueHandling.Ignore)]
        public int? PurchaseCountStart { get; set; }

        /// <summary>
        /// 请求id
        /// </summary>
        [JsonProperty("requestId", NullValueHandling = NullValueHandling.Ignore)]
        public string RequestId { get; set; }

        /// <summary>
        /// 视频号授权信息ID
        /// </summary>
        [JsonProperty("secUid", NullValueHandling = NullValueHandling.Ignore)]
        public string SecUid { get; set; }

        /// <summary>
        /// 总观看人次
        /// </summary>
        [JsonProperty("totalWatchNum", NullValueHandling = NullValueHandling.Ignore)]
        public int? TotalWatchNum { get; set; }

        /// <summary>
        /// uv价值区间范围-结束
        /// </summary>
        [JsonProperty("uvValueEnd", NullValueHandling = NullValueHandling.Ignore)]
        public double UvValueEnd { get; set; }

        /// <summary>
        /// uv价值区间范围-起始
        /// </summary>
        [JsonProperty("uvValueStart", NullValueHandling = NullValueHandling.Ignore)]
        public double UvValueStart { get; set; }

        /// <summary>
        /// 关联的数据看盘id
        /// </summary>
        [JsonProperty("videoDataViewingId", NullValueHandling = NullValueHandling.Ignore)]
        public long VideoDataViewingId { get; set; }

        /// <summary>
        /// 销售额区间范围-结束(单位:元)
        /// </summary>
        [JsonProperty("volumeEnd", NullValueHandling = NullValueHandling.Ignore)]
        public int? VolumeEnd { get; set; }

        /// <summary>
        /// 销售额区间范围-起始(单位:元)
        /// </summary>
        [JsonProperty("volumeStart", NullValueHandling = NullValueHandling.Ignore)]
        public int? VolumeStart { get; set; }

        /// <summary>
        /// 看播流量结构
        /// </summary>
        [JsonProperty("watchFlowList", NullValueHandling = NullValueHandling.Ignore)]
        public WatchFlowStructure[] WatchFlowList { get; set; }

        /// <summary>
        /// 看播用户画像
        /// </summary>
        [JsonProperty("watchUserPortrait", NullValueHandling = NullValueHandling.Ignore)]
        public WatchUserPortraitData WatchUserPortrait { get; set; }
    }

    /// <summary>
    /// 成交流量结构
    /// </summary>
    public class PayFlowStructure
    {
        /// <summary>
        /// 流量渠道名称
        /// </summary>
        [JsonProperty("channelName", NullValueHandling = NullValueHandling.Ignore)]
        public string ChannelName { get; set; }

        [JsonProperty("ratio", NullValueHandling = NullValueHandling.Ignore)]
        public double Ratio { get; set; }

        /// <summary>
        /// 添加子流量结构支持
        /// </summary>
        [JsonProperty("subFlow", NullValueHandling = NullValueHandling.Ignore)]
        public SubFlowData[] SubFlow { get; set; }
    }


    public class SubFlowData
    {
        [JsonProperty("channelName", NullValueHandling = NullValueHandling.Ignore)]
        public string ChannelName { get; set; }

        [JsonProperty("ratio", NullValueHandling = NullValueHandling.Ignore)]
        public double Ratio { get; set; }
    }

    /// <summary>
    /// 成交用户画像
    /// </summary>
    public class PayUserPortraitData
    {
        [JsonProperty("agePortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PortraitItem[] AgePortrait { get; set; }

        [JsonProperty("genderPortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PortraitItem[] GenderPortrait { get; set; }

        [JsonProperty("provincePortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PortraitItem[] ProvincePortrait { get; set; }
    }

    /// <summary>
    /// 用户画像项目
    /// </summary>
    public class PortraitItem
    {
        [JsonProperty("label", NullValueHandling = NullValueHandling.Ignore)]
        public string Label { get; set; }

        [JsonProperty("value", NullValueHandling = NullValueHandling.Ignore)]
        public double Value { get; set; }
    }

    /// <summary>
    /// 看播流量结构
    /// </summary>
    public class WatchFlowStructure
    {
        /// <summary>
        /// 流量渠道名称
        /// </summary>
        [JsonProperty("channelName", NullValueHandling = NullValueHandling.Ignore)]
        public string ChannelName { get; set; }

        [JsonProperty("ratio", NullValueHandling = NullValueHandling.Ignore)]
        public double Ratio { get; set; }

        /// <summary>
        /// 添加子流量结构支持
        /// </summary>
        [JsonProperty("subFlow", NullValueHandling = NullValueHandling.Ignore)]
        public SubFlowData[] SubFlow { get; set; }
    }

    /// <summary>
    /// 看播用户画像
    /// </summary>
    public class WatchUserPortraitData
    {
        [JsonProperty("agePortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PortraitItem[] AgePortrait { get; set; }

        [JsonProperty("genderPortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PortraitItem[] GenderPortrait { get; set; }

        [JsonProperty("provincePortrait", NullValueHandling = NullValueHandling.Ignore)]
        public PortraitItem[] ProvincePortrait { get; set; }
    }

    /// <summary>
    /// 视频号直播分钟级别数据
    /// </summary>
    public class LiveDashboardMinuteResponse
    {
        /// <summary>
        /// 新增粉丝人数
        /// </summary>
        [JsonProperty("fansClubJoinUcnt", NullValueHandling = NullValueHandling.Ignore)]
        public long FansClubJoinUcnt { get; set; }

        /// <summary>
        /// 新增关注主播人数
        /// </summary>
        [JsonProperty("followAnchorUcnt", NullValueHandling = NullValueHandling.Ignore)]
        public long FollowAnchorUcnt { get; set; }

        /// <summary>
        /// 采集时间
        /// </summary>
        [JsonProperty("gatherDateTime", NullValueHandling = NullValueHandling.Ignore)]
        public DateTime GatherDateTime { get; set; }

        /// <summary>
        /// 采集时间戳
        /// </summary>
        [JsonProperty("gatherTimeStamp", NullValueHandling = NullValueHandling.Ignore)]
        public long GatherTimeStamp { get; set; }

        /// <summary>
        /// 当前在线人数
        /// </summary>
        [JsonProperty("maxOnlineWatchNum", NullValueHandling = NullValueHandling.Ignore)]
        public long MaxOnlineWatchNum { get; set; }

        /// <summary>
        /// 成交金额
        /// </summary>
        [JsonProperty("payAmt", NullValueHandling = NullValueHandling.Ignore)]
        public long PayAmt { get; set; }

        /// <summary>
        /// 成交组成
        /// </summary>
        [JsonProperty("payComboCnt", NullValueHandling = NullValueHandling.Ignore)]
        public long PayComboCnt { get; set; }

        /// <summary>
        /// 累计观看人数
        /// </summary>
        [JsonProperty("watchNum", NullValueHandling = NullValueHandling.Ignore)]
        public long WatchNum { get; set; }
    }
}