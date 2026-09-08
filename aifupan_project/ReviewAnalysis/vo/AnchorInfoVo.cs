using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.vo
{
    public class AnchorInfoVo
    {
        /// <summary>
        /// 主键
        /// </summary>
        [JsonProperty("id")]
        public int Id { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        [JsonProperty("anchorName")]
        public string AnchorName { get; set; }

        /// <summary>
        /// 主播头像地址
        /// </summary>
        [JsonProperty("anchorAvatar")]
        public string AnchorAvatar { get; set; }

        /// <summary>
        /// DouYinLive 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
        /// </summary>
        [JsonProperty("anchorPlatform")]
        public string AnchorPlatform { get; set; }

        /// <summary>
        /// 主播个人主页 
        /// </summary>
        [JsonProperty("homeUrl")]
        public string HomeUrl { get; set; }

        /// <summary>
        /// 直播地址
        /// </summary>
        [JsonProperty("liveUrl")]
        public string LiveUrl { get; set; }

        ///<<summary>
        /// app分享直播地址
        /// </summary>       
        [JsonProperty("appShareUrl")]
        public string AppShareUrl { get; set; }


        /// <summary>
        /// 直播状态 0未检测 2直播中，4未直播 
        /// </summary>
        [JsonProperty("liveStatus")]
        public int LiveStatus { get; set; }

        /// <summary>
        /// 录播状态 record_status 0未开始录制，1正在录制 2手动停止录制，3录制完成，4手动开启
        /// </summary>
        [JsonProperty("recordStatus")]
        public int RecordStatus { get; set; }

        /// <summary>
        /// 主播在各平台的唯一标识
        /// </summary>
        [JsonProperty("secUid")]
        public string SecUid { get; set; }

        /// <summary>
        /// 检测在线直播时，是否录制视频 is_auto_record  0否，1是
        /// </summary>
        [JsonProperty("isAutoRecord")]
        public int IsAutoRecord { get; set; }

        /// <summary>
        /// 主播直播时的在线人数 
        /// </summary>
        [JsonProperty("onlineNumber")]
        public string OnlineNumber { get; set; }

        /// <summary>
        /// 当前正在直播的批次编号,batch_number 每次开启录播就会更新
        /// </summary>
        [JsonProperty("batchNumber")]
        public string BatchNumber { get; set; }

        /// <summary>
        /// 添加主播的时间
        /// </summary>
        [JsonProperty("addTime")]
        public string AddTime { get; set; }

        /// <summary>
        /// 开始录制时间
        /// </summary>
        [JsonProperty("startTime")]
        public string StartTime { get; set; }

        /// <summary>
        /// 最新一次的直播流地址
        /// </summary>
        [JsonProperty("streamUrl")]
        public string StreamUrl { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty("tradeId")]
        public string TradeId { get; set; }

        /// <summary>
        /// 主播userId
        /// </summary>
        [JsonProperty("anchorUserId")]
        public string AnchorUserId { get; set; }

        /// <summary>
        /// webSocketId
        /// </summary>
        [JsonProperty("webSocketId")]
        public string WebSocketId { get; set; }

        /// <summary>
        /// 是否从录制列表移除了 0：否 1：是
        /// </summary>
        [JsonProperty("isRemoveRecord")]
        public int IsRemoveRecord { get; set; }

        /// <summary>
        /// 直播源地址
        /// </summary>
        [JsonProperty("sourceUrl")]
        public string SourceUrl { get; set; }
    }
}
