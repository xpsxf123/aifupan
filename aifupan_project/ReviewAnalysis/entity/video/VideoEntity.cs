using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.entity.video
{
    public class VideoEntity
    {
        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 视频唯一标识
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 视频名称
        /// </summary>
        public string videoName { get; set; }

        /// <summary>
        /// 开始录制时间
        /// </summary>
        public string startTime { get; set; }

        /// <summary>
        /// 第几段
        /// </summary>
        public int? paragraph { get; set; }

        /// <summary>
        /// 分段类型 0不分段，1按时长分段，2按大小分段
        /// </summary>
        public int? subsectionType { get; set; }

        /// <summary>
        /// 视频时长
        /// </summary>
        public string duration { get; set; }

        /// <summary>
        /// 视频大小，单位：B
        /// </summary>
        public string vedioSizie { get; set; }

        /// <summary>
        /// 结束录制时间
        /// </summary>
        public string endTime { get; set; }

        /// <summary>
        /// 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 视频类型  0 ts,1 flv,2 mp4
        /// </summary>
        public int? videoType { get; set; }

        /// <summary>
        /// 清晰度 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public int? definition { get; set; }

        /// <summary>
        /// 存储路径
        /// </summary>
        public string storagePath { get; set; }

        /// <summary>
        /// 视频url
        /// </summary>
        public string liveUrl { get; set; }

        /// <summary>
        /// 分享地址
        /// </summary>
        public string shareUrl { get; set; }

        /// <summary>
        /// 在线播放地址
        /// </summary>
        public string playUrl { get; set; }

        /// <summary>
        /// 直播源地址
        /// </summary>
        public string sourceUrl { get; set; }

        /// <summary>
        /// 主播表主键
        /// </summary>
        public int? anchorId { get; set; }

        /// <summary>
        /// 是否正在录制 0否 1是 2：停止录制了但是断网没能更新到服务端
        /// </summary>
        public int? isRecording { get; set; }

        /// <summary>
        /// 直播源类型 0 m3u8  1  flv
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 直播标题
        /// </summary>
        public string liveTitle { get; set; }

        /// <summary>
        /// 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误 4：未开启自动分析 5：切片中
        /// </summary>
        public int? analysisStatus { get; set; }

        /// <summary>
        /// 分析时间
        /// </summary>
        public string analysisTime { get; set; }

        /// <summary>
        /// 错误原因
        /// </summary>
        public string errorReason { get; set; }

        /// <summary>
        /// 主播url表的唯一标识
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string tradeId { get; set; }

        /// <summary>
        /// 平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
        /// </summary>
        public string platformType { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public string userId { get; set; }

        /// <summary>
        /// 上传的用户昵称
        /// </summary>
        public string userNickName { get; set; }

        /// <summary>
        /// 类型 1 代表已经录制未上传分享  2 上传分享
        /// </summary>
        public int? type { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 视频上传状态 0：未上传 1：已上传 2：上传中
        /// </summary>
        public int? uploadStatus { get; set; }

        /// <summary>
        /// 是否已标注敏感词 0：未标注 1：已标注
        /// </summary>
        public int? isMark { get; set; }

        /// <summary>
        /// 占用云空间的大小，单位：M
        /// </summary>
        public int? cloudStore { get; set; }

        /// <summary>
        /// 视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
        /// </summary>
        public int? deleteStatus { get; set; }

        /// <summary>
        /// 视频的租户id
        /// </summary>
        public long? tenantId { get; set; }
        /// <summary>
        /// 分析使用的资源 0：智能分析时长 1：文案提取
        /// </summary>
        public int? useAnalysisPropertyType { get; set; }
        /// <summary>
        /// 视频录制的异常状态 0：未出现异常 7：录制网络异常
        /// </summary>
        public int? recordErrorStatus { get; set; }
        /// <summary>
        /// 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
        /// </summary>
        public int? videoSliceType { get; set; }
        /// <summary>
        /// 数据来源 0=正常获取 1=巨量拉取
        /// </summary>
        public int? dataSource { get; set; }
        /// <summary>
        /// 视频是否下载 0=未下载 1=已下载
        /// </summary>
        public int? isDownloaded { get; set; }
        /// <summary>
        /// 视频切片信息
        /// </summary>
        public VideoSliceEntity videoSliceInfo { get; set; }
        /// <summary>
        /// 切片视频所属原视频信息
        /// </summary>
        public VideoEntity parentVideoInfo { get; set; }
        /// <summary>
        /// 原视频下的所有切片信息
        /// </summary>
        public List<VideoSliceEntity> sliceList { get; set; }
        /// <summary>
        /// 云空间备注
        /// </summary>
        public string cloudRemarks { get; set; }
        /// <summary>
        /// 重命名
        /// </summary>
        public string videoRename { get; set; }
        /// <summary>
        /// 云空间重命名
        /// </summary>
        public string cloudRename { get; set; }
        /// <summary>
        /// 是否有AI优化目的 0：否 1：是
        /// </summary>
        public int? hasAiOptimizePurpose { get; set; }
    }
}
