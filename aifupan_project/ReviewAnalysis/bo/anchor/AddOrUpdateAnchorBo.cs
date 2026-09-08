using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.vo.common;

namespace ReviewAnalysis.bo.anchor
{
    public class AddOrUpdateAnchorBo : BasicSettingsBaseDto
    {
        /// <summary>
        /// 主播唯一标识
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 主页url
        /// </summary>
        public string homeUrl { get; set; }

        /// <summary>
        /// 直播间url
        /// </summary>
        public string liveUrl { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        public string anchorName { get; set; }

        /// <summary>
        /// 主播头像
        /// </summary>
        public string anchorAvatar { get; set; }

        /// <summary>
        /// 平台类型 0：抖音 1：快手 2：视频号
        /// </summary>
        public int? platform { get; set; }

        /// <summary>
        /// DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
        /// </summary>
        public string platformResource { get; set; }

        /// <summary>
        /// 主播userId
        /// </summary>
        public string anchorUserId { get; set; }

        /// <summary>
        /// webSocketId
        /// </summary>
        public string webSocketId { get; set; }

        /// <summary>
        /// 在线时,是否自动录制分析
        /// </summary>
        public int? isAutoRecord { get; set; }

        /// <summary>
        /// 是否从录制列表移除了 0：否 1：是
        /// </summary>
        public int? isRemoveRecord { get; set; }

        /// <summary>
        /// 是否开启弹幕监控 0否， 1是
        /// </summary>
        public int? isBarrageMonitoring { get; set; }

        /// <summary>
        /// 是否自动上传到云空间 0：否 1：是
        /// </summary>
        public int? isAutoUploadCloud { get; set; }

        /// <summary>
        /// 是否置顶 0：否 1：是
        /// </summary>
        public int? isTop { get; set; }

        /// <summary>
        /// 加入置顶的时间
        /// </summary>
        public string addTopTime { get; set; }

        /// <summary>
        /// 最后开始录制时间
        /// </summary>
        public string lastRecordTime { get; set; }

        /// <summary>
        /// 录制时间，如：06:00:00-19:00:00
        /// </summary>
        public string recordTime { get; set; }

        /// <summary>
        /// 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒
        /// </summary>
        public int? smsTip { get; set; }

        /// <summary>
        /// 是否开启数据看板 0：否 1：是
        /// </summary>
        public int? isDataViewing { get; set; }

        /// <summary>
        /// 文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)
        /// </summary>
        public string folderName { get; set; }

        /// <summary>
        /// 主播备注名称
        /// </summary>
        public string remarksName { get; set; }

        /// <summary>
        /// 行业ID
        /// </summary>
        public string tradeId { get; set; }

        /// <summary>
        /// 是否自动诊断 0：否 1：是
        /// </summary>
        public int isAutoDiagnosis { get; set; }

        /// <summary>
        /// 诊断参数
        /// </summary>
        public DiagnosisParams diagnosisParams { get; set; }

        /// <summary>
        /// 主播url类型 0：抖音号链接 1：直播间链接
        /// </summary>
        public int urlType { get; set; }
        /// <summary>
        /// 主播抖音号/快手自定义id
        /// </summary>
        public string anchorNumber { get; set; }
        /// <summary>
        /// 录制的清晰度 -1：跟随系统 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public int? recordDefinition { get; set; }
        /// <summary>
        /// 录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制(只录一段) 2：时长分段录制
        /// </summary>
        public int? recordLimitType { get; set; }
        /// <summary>
        /// 录制的时长，单位：分钟
        /// </summary>
        public int? recordLimitValue { get; set; }
        /// <summary>
        /// 是否自动分析视频 0：否 1：是
        /// </summary>
        public int? isAutoAnalysis { get; set; }
        /// <summary>
        /// 授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int? authJlbyStatus { get; set; }
        
        /// <summary>
        /// 是否开启数据诊断 0：否 1：是
        /// </summary>
        public int? isDataDiagnosis { get; set; }
        
        /// <summary>
        /// 自动生成数据诊断剩余场次
        /// </summary>
        public int? diagnosisGenerateNum { get; set; }
        
        /// <summary>
        /// 数据诊断参数
        /// </summary>
        public DiagnosisParams dataDiagnosisParams { get; set; }
        /// <summary>
        /// 一句话识别引擎模型，如：16k_zh
        /// </summary>
        public string engSerViceType { get; set; }
        /// <summary>
        /// 纯录制版是否获取在线人数 0：否 1：是
        /// </summary>
        public int? pureRecordOnlineNum { get; set; }
        /// <summary>
        /// 是否按排班录制 0：否 1：是
        /// </summary>
        public int? isScheduleRecord { get; set; }
        /// <summary>
        /// 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认）  2：按时间点分段录制
        /// </summary>
        public int? recordTimeMode { get; set; }

        /// <summary>
        /// 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔
        /// </summary>
        public string segmentTimePoints { get; set; }

        /// <summary>
        /// 是否统计业绩 0-否 1-是
        /// </summary>
        public int? isStatisticsPerformance { get; set; }

        /// <summary>
        /// 自动删除时间 -1不删除 0马上删除 N天后删除，空=跟随全局配置（使用字典 auto_delete_time 的值）
        /// </summary>
        public string autoDeleteTime { get; set; }

        /// <summary>
        /// 删除内容 ts源视频/mp4成品/all都删，空=跟随全局配置（使用字典 delete_content 的值）
        /// </summary>
        public string deleteContent { get; set; }

    }

    public class DiagnosisParams
    {
        /// <summary>
        /// 模型id
        /// </summary>
        public string modelId { get; set; }

        /// <summary>
        /// 问题ids
        /// </summary>
        public List<string> cueWordsIds { get; set; }
    }
}
