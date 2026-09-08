using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class AnchorVideoDto
    {
        /// <summary>
        /// 观看人数
        /// </summary>
        public int viewersNum;

        /// <summary>
        /// 敏感词数量
        /// </summary>
        public int SensitiveNum { get; set; }
        /// <summary>
        /// 关键词数量
        /// </summary>
        public int CruxNum { get; set; }
        /// <summary>
        /// 主播信息
        /// </summary>
        public AnchorInfo AnchorInfo { get; set; }
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 视频唯一标识id
        /// </summary>
        public string VideoId { get; set; }
        /// <summary>
        /// 视频文件名称
        /// </summary>
        public string VideoName { get; set; }

        /// <summary>
        /// 开始录制时间 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string StartTime { get; set; }


        /// <summary>
        /// 第几段视频（每次直播可以分段录制）
        /// </summary>
        public int Paragraph { get; set; }


        /// <summary>
        /// 分段类型 0不分段，1按时长分段，2按大小分段
        /// </summary>
        public int SubsectionType { get; set; }


        /// <summary>
        /// 每段视频的时长 (单位:秒)
        /// </summary>
        public string Duration { get; set; }

        /// <summary>
        /// 视频大小  (单位：M)
        /// </summary>
        public string VedioSizie { get; set; }

        /// <summary>
        /// 结束录制时间 end_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string EndTime { get; set; }


        /// <summary>
        /// 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是直播间的id roomId
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        ///视频类型  0 ts,1 flv,2 mp4
        /// </summary>
        public int VideoType { get; set; }

        /// <summary>
        /// 清晰度 definition 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public int Definition { get; set; }

        /// <summary>
        /// 存储路径 
        /// </summary>
        public string StoragePath { get; set; }

        /// <summary>
        /// 直播源类型 source_type 0 m3u8 1 flv
        /// </summary>
        public int SourceType { get; set; }

        /// <summary>
        /// 直播源地址 
        /// </summary>
        public string SourceUrl { get; set; }

        /// <summary>
        /// 主播表主键 
        /// </summary>
        public int AnchorId { get; set; }

        /// <summary>
        /// 直播标题 live_title
        /// </summary>
        public string LiveTitle { get; set; }

        /// <summary>
        /// 是否正在录制 is_recording 0否 1是
        /// </summary>
        public int IsRecording { get; set; }

        /// <summary>
        /// 分析完成时间 analysis_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string AnalysisTime { get; set; }

        /// <summary>
        /// 分析状态 analysis_status 0：未分析 1：分析中 2：分析完成
        /// </summary>
        public int AnalysisStatus { get; set; }

        /// <summary>
        /// 分析失败原因
        /// </summary>
        public string ErrorReason { get; set; }

        /// <summary>
        /// 主播唯一标识
        /// </summary>
        public string SecUid { get; set; }
        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }
        /// <summary>
        /// 平台类型
        /// </summary>
        public string PlatformType { get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }
        /// <summary>
        /// 是否已上传到服务器 0：否 1：是
        /// </summary>
        public int UploadStatus { get; set; }
        /// <summary>
        /// 是否能标注词语 0：否 1：是
        /// </summary>
        public int IsMark { get; set; }
        /// <summary>
        /// 在线复盘url
        /// </summary>
        public string ShareUrl { get; set; }
        /// <summary>
        /// 在线文件的url
        /// </summary>
        public string PlayUrl { get; set; }
    }
}
