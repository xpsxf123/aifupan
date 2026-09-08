using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.vo
{
    public class AnchorVideoInfoVo
    {
        /// <summary>
        /// 主键
        /// </summary>
        [JsonProperty("id")]
        public int Id { get; set; }

        /// <summary>
        /// 视频唯一标识id
        /// </summary>
        [JsonProperty("videoId")]
        public string VideoId { get; set; }

        /// <summary>
        /// 视频文件名称
        /// </summary>
        [JsonProperty("videoName")]
        public string VideoName { get; set; }

        /// <summary>
        /// 开始录制时间 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        [JsonProperty("startTime")]
        public string StartTime { get; set; }


        /// <summary>
        /// 第几段视频（每次直播可以分段录制）
        /// </summary>
        [JsonProperty("paragraph")]
        public int Paragraph { get; set; }


        /// <summary>
        /// 分段类型 0不分段，1按时长分段，2按大小分段
        /// </summary>
        [JsonProperty("subsectionType")]
        public int SubsectionType { get; set; }


        /// <summary>
        /// 每段视频的时长 (单位:秒)
        /// </summary>
        [JsonProperty("duration")]
        public string Duration { get; set; }

        /// <summary>
        /// 视频大小  (单位：M)
        /// </summary>
        [JsonProperty("vedioSizie")]
        public string VedioSizie { get; set; }

        /// <summary>
        /// 结束录制时间 end_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        [JsonProperty("endTime")]
        public string EndTime { get; set; }


        /// <summary>
        /// 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是直播间的id roomId
        /// </summary>
        [JsonProperty("batchNumber")]
        public string BatchNumber { get; set; }

        /// <summary>
        ///视频类型  0 ts,1 flv,2 mp4
        /// </summary>
        [JsonProperty("videoType")]
        public int VideoType { get; set; }

        /// <summary>
        /// 清晰度 definition 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        [JsonProperty("definition")]
        public int Definition { get; set; }

        /// <summary>
        /// 存储路径 
        /// </summary>
        [JsonProperty("storagePath")]
        public string StoragePath { get; set; }

        /// <summary>
        /// 直播源类型 source_type 0 m3u8 1 flv
        /// </summary>
        [JsonProperty("sourceType")]
        public int SourceType { get; set; }

        /// <summary>
        /// 直播源地址 
        /// </summary>
        [JsonProperty("sourceUrl")]
        public string SourceUrl { get; set; }

        /// <summary>
        /// 主播表主键 
        /// </summary>
        [JsonProperty("anchorId")]
        public int AnchorId { get; set; }

        /// <summary>
        /// 直播标题 live_title
        /// </summary>
        [JsonProperty("liveTitle")]
        public string LiveTitle { get; set; }

        /// <summary>
        /// 是否正在录制 is_recording 0否 1是
        /// </summary>
        [JsonProperty("isRecording")]
        public int IsRecording { get; set; }

        /// <summary>
        /// 分析完成时间 analysis_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        [JsonProperty("analysisTime")]
        public string AnalysisTime { get; set; }

        /// <summary>
        /// 分析状态 analysis_status 0：未分析 1：分析中 2：分析完成 3：分析失败
        /// </summary>
        [JsonProperty("analysisStatus")]
        public int AnalysisStatus { get; set; }
        /// <summary>
        /// 分析失败原因
        /// </summary>
        [JsonProperty("errorReason")]
        public string ErrorReason { get; set; }

        /// <summary>
        /// 主播的唯一Id
        /// </summary>
        [JsonProperty("secUid")]
        public string SecUid { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty("tradeId")]
        public string TradeId { get; set; }
        /// <summary>
        /// 平台类型
        /// </summary>
        [JsonProperty("platformType")]
        public string PlatformType { get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        [JsonProperty("userId")]
        public string UserId { get; set; }
        /// <summary>
        /// 是否已上传到服务器 0：否 1：是
        /// </summary>
        [JsonProperty("uploadStatus")]
        public int UploadStatus { get; set; }
        /// <summary>
        /// 是否已标注词语 0：否 1：是
        /// </summary>
        [JsonProperty("isMark")]
        public int IsMark { get; set; }
        /// <summary>
        /// 在线复盘url
        /// </summary>
        [JsonProperty("shareUrl")]
        public string ShareUrl { get; set; }
        /// <summary>
        /// 在线文件的url
        /// </summary>
        [JsonProperty("playUrl")]
        public string PlayUrl { get; set; }
        /// <summary>
        /// 占用云空间的大小，单位：M
        /// </summary>
        [JsonProperty("cloudStore")]
        public int CloudStore { get; set; }
        /// <summary>
        /// 视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
        /// </summary>
        [JsonProperty("deleteStatus")]
        public int DeleteStatus { get; set; }
        /// <summary>
        /// 租户id
        /// </summary>
        [JsonProperty("tenantId")]
        public long TenantId { get; set; }
        /// <summary>
        /// 主播信息
        /// </summary>
        [JsonProperty("anchorInfo")]
        public AnchorInfo AnchorInfo {  get; set; }
    }
}
