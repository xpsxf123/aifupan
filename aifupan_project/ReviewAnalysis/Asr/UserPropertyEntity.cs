using Newtonsoft.Json;

namespace ReviewAnalysis.Asr
{
    public class UserPropertyEntity
    {
        /// <summary>
        /// ai语音分析时长-剩余
        /// </summary>
        [JsonProperty(PropertyName = "aiAnalysisTime")]
        public double AiAnalysisTime { get; set; }

        /// <summary>
        /// ai语音分析时长-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useAiAnalysisTime")]
        public double UseAiAnalysisTime { get; set; }

        /// <summary>
        /// ai语音分析时长-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalAiAnalysisTime")]
        public double TotalAiAnalysisTime { get; set; }

        /// <summary>
        /// 视频标注时长-剩余
        /// </summary>
        [JsonProperty(PropertyName = "videoTaggingTime")]
        public long VideoTaggingTime { get; set; }

        /// <summary>
        /// 视频标注时长-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useVideoTaggingTime")]
        public long UseVideoTaggingTime { get; set; }

        /// <summary>
        /// 视频标注时长-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalVideoTaggingTime")]
        public long TotalVideoTaggingTime { get; set; }

        /// <summary>
        /// 文本标注字数-剩余
        /// </summary>
        [JsonProperty(PropertyName = "textTaggingWordCount")]
        public long TextTaggingWordCount { get; set; }

        /// <summary>
        /// 文本标注字数-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useTextTaggingWordCount")]
        public long UseTextTaggingWordCount { get; set; }

        /// <summary>
        /// 文本标注字数-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalTextTaggingWordCount")]
        public long TotalTextTaggingWordCount { get; set; }

        /// <summary>
        /// 监控数-剩余
        /// </summary>
        [JsonProperty(PropertyName = "monitorNum")]
        public int MonitorNum { get; set; }

        /// <summary>
        /// 监控数-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useMonitorNum")]
        public int UseMonitorNum { get; set; }

        /// <summary>
        /// 监控数-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalMonitorNum")]
        public int TotalMonitorNum { get; set; }

        /// <summary>
        /// 主播数-剩余
        /// </summary>
        [JsonProperty(PropertyName = "anchorNum")]
        public int AnchorNum { get; set; }

        /// <summary>
        /// 主播数-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useAnchorNum")]
        public int UseAnchorNum { get; set; }

        /// <summary>
        /// 主播数-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalAnchorNum")]
        public int TotalAnchorNum { get; set; }

        /// <summary>
        /// 存储空间-剩余
        /// </summary>
        [JsonProperty(PropertyName = "storageNum")]
        public long StorageNum { get; set; }

        /// <summary>
        /// 存储空间-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useStorageNum")]
        public long UseStorageNum { get; set; }

        /// <summary>
        /// 存储空间-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalStorageNum")]
        public long TotalStorageNum { get; set; }

        /// <summary>
        /// 子账号数-剩余
        /// </summary>
        [JsonProperty(PropertyName = "subAccountCount")]
        public long SubAccountCount { get; set; }

        /// <summary>
        /// 子账号数-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useSubAccountCount")]
        public long UseSubAccountCount { get; set; }

        /// <summary>
        /// 子账号数-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalSubAccountCount")]
        public long TotalSubAccountCount { get; set; }

		/// <summary>
        /// 主播弹幕监控位-剩余
        /// </summary>
        [JsonProperty(PropertyName = "anchorBarrageNum")]
        public long AnchorBarrageNum { get; set; }



        /// <summary>
        /// 主播弹幕监控位-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useAnchorBarrageNum")]
        public long UseAnchorBarrageNum { get; set; }

        /// <summary>
        /// 主播弹幕监控位-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalAnchorBarrageNum")]
        public long TotalAnchorBarrageNum { get; set; }

        /// <summary>
        /// 弹幕显示条数-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalBarrageNum")]
        public long TotalBarrageNum { get; set; }

        /// <summary>
        /// AI分析文本数量-剩余
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "aiTokenNum")]
        public long? AiTokenNum{ get; set; }


        /// <summary>
        /// AI分析文本数量-已使用
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "useAiTokenNum")]
        public long? UseAiTokenNum{ get; set; }

        /// <summary>
        /// AI分析文本数量-总数
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "totalAiTokenNum")]
        public long? TotalAiTokenNum{ get; set; }

        /// <summary>
        /// 图片识别数量-剩余
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "imgIdentifyNum")]
        public long? ImgIdentifyNum{ get; set; }

        /// <summary>
        /// 图片识别数量-已使用
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "useImgIdentifyNum")]
        public long? UseImgIdentifyNum{ get; set; }

        /// <summary>
        /// 图片识别数量-总数
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "totalImgIdentifyNum")]
        public long? TotalImgIdentifyNum{ get; set; }

        /// <summary>
        /// 数据看板获取-剩余
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "dataBoardNum")]
        public long? DataBoardNum{ get; set; }

        /// <summary>
        /// 数据看板-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useDataBoardNum")]
        public long? UseDataBoardNum{ get; set; }

        /// <summary>
        /// 数据看板-总数
        /// </summary>
        /// <param name=""></param>
        [JsonProperty(PropertyName = "totalDataBoardNum")]
        public long? TotalDataBoardNum{ get; set; }

        /// <summary>
        /// 短信条数-剩余
        /// </summary>
        [JsonProperty(PropertyName = "smsMessageNum")]
        public long? SmsMessageNum{ get; set; }

        /// <summary>
        /// 短信条数-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useSmsMessageNum")]
        public long? UseSmsMessageNum{ get; set; }

        /// <summary>
        /// 短信条数-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalSmsMessageNum")]
        public long? TotalSmsMessageNum{ get; set; }

        /// <summary>
        /// 文案提取时长-剩余
        /// </summary>
        [JsonProperty(PropertyName = "textExtractionNum")]
        public long? TextExtractionNum{ get; set; }

        /// <summary>
        /// 文案提取时长-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useTextExtractionNum")]
        public long? UseTextExtractionNum{ get; set; }

        /// <summary>
        /// 文案提取时长-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalTextExtractionNum")]
        public long? TotalTextExtractionNum{ get; set; }

        /// <summary>
        /// 巨量监控位-剩余
        /// </summary>
        [JsonProperty(PropertyName = "rpaAmountNum")]
        public long? rpaAmountNum{ get; set; }

        /// <summary>
        /// 巨量监控位-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useRpaAmountNum")]
        public long? useRpaAmountNum{ get; set; }

        /// <summary>
        /// 巨量监控位-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalRpaAmountNum")]
        public long? totalRpaAmountNum{ get; set; }

        /// <summary>
        /// 提取文案条数-剩余
        /// </summary>
        public long? shortVideoNum{ get; set; }

        /// <summary>
        /// 提取文案条数-已使用
        /// </summary>
        public long? useShortVideoNum{ get; set; }

        /// <summary>
        /// 提取文案条数-总数
        /// </summary>
        public long? totalShortVideoNum{ get; set; }

        /// <summary>
        /// 搜索爆款数量-剩余
        /// </summary>
        public long? searchHotVideoNum{ get; set; }

        /// <summary>
        /// 搜索爆款数量-已使用
        /// </summary>
        public long? useSearchHotVideoNum{ get; set; }

        /// <summary>
        /// 搜索爆款数量-总数
        /// </summary>
        public long? totalSearchHotVideoNum{ get; set; }

        /// <summary>
        /// 搜索达人数量-剩余
        /// </summary>
        public long? searchInfluencerNum{ get; set; }

        /// <summary>
        /// 搜索达人数量-已使用
        /// </summary>
        public long? useSearchInfluencerNum{ get; set; }

        /// <summary>
        /// 搜索达人数量-总数
        /// </summary>
        public long? totalSearchInfluencerNum{ get; set; }

        /// <summary>
        /// 订阅爆款数量-剩余
        /// </summary>
        public long? subscribeHotVideoNum{ get; set; }

        /// <summary>
        /// 订阅爆款数量-已使用
        /// </summary>
        public long? useSubscribeHotVideoNum{ get; set; }

        /// <summary>
        /// 订阅爆款数量-总数
        /// </summary>
        public long? totalSubscribeHotVideoNum{ get; set; }

        /// <summary>
        /// 订阅达人数量-剩余
        /// </summary>
        public long? subscribeInfluencerNum{ get; set; }

        /// <summary>
        /// 订阅达人数量-已使用
        /// </summary>
        public long? useSubscribeInfluencerNum{ get; set; }

        /// <summary>
        /// 订阅达人数量-总数
        /// </summary>
        public long? totalSubscribeInfluencerNum{ get; set; }

        /// <summary>
        /// 账号订阅达人数量-剩余
        /// </summary>
        public long? userSubscribeInfluencerNum{ get; set; }

        /// <summary>
        /// 账号订阅达人数量-已使用
        /// </summary>
        public long? useUserSubscribeInfluencerNum{ get; set; }

        /// <summary>
        /// 账号订阅达人数量-总数
        /// </summary>
        public long? totalUserSubscribeInfluencerNum{ get; set; }

        /// <summary>
        /// 账号订阅爆款数量-剩余
        /// </summary>
        public long? userSubscribeHotVideoNum{ get; set; }

        /// <summary>
        /// 账号订阅爆款数量-已使用
        /// </summary>
        public long? useUserSubscribeHotVideoNum{ get; set; }

        /// <summary>
        /// 账号订阅爆款数量-总数
        /// </summary>
        public long? totalUserSubscribeHotVideoNum{ get; set; }

        /// <summary>
        /// 快手监控位-剩余
        /// </summary>
        [JsonProperty(PropertyName = "kuaishouMonitorNum")]
        public long? kuaishouMonitorNum;

        /// <summary>
        /// 快手监控位-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useKuaishouMonitorNum")]
        public long? useKuaishouMonitorNum;

        /// <summary>
        /// 快手监控位-总数
        /// </summary>
        [JsonProperty(PropertyName = "totalKuaishouMonitorNum")]
        public long? totalKuaishouMonitorNum;

        /// <summary>
        /// 微信视频号监控位-剩余
        /// </summary>
        [JsonProperty(PropertyName = "channelMonitorNum")]
        public long? WeChatChannelMonitorNum { get; set; }

        /// <summary>
        /// 微信视频号监控位-已使用
        /// </summary>
        [JsonProperty(PropertyName = "useChannelMonitorNum")]
        public long? UseWeChatChannelsMonitorNum { get; set; }

        /// <summary>
        /// 微信视频号监控位-总计
        /// </summary>
        [JsonProperty(PropertyName = "totalChannelMonitorNum")]
        public long? TotalWeChatChannelsMonitorNum { get; set; }
    }
}
