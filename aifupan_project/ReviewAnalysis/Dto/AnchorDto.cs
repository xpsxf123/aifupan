using Newtonsoft.Json;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo.anchor;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class AnchorDto
    {
        /// <summary>
        /// 主播的主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        public string AnchorName { get; set; }

        /// <summary>
        /// 主播头像地址
        /// </summary>
        public string AnchorAvatar { get; set; }

        /// <summary>
        /// DouYinLive 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
        /// </summary>
        public string AnchorPlatform { get; set; }

        /// <summary>
        /// 主播个人主页 
        /// </summary>
        public string HomeUrl { get; set; }

        /// <summary>
        /// 直播地址
        /// </summary>
        public string LiveUrl { get; set; }

        ///<<summary>
        /// app分享直播地址
        /// </summary>       
        public string AppShareUrl { get; set; }


        /// <summary>
        /// 直播状态 0未检测 2直播中，4未直播 
        /// </summary>
        public int LiveStatus { get; set; }

        /// <summary>
        /// 录播状态 record_status 0未开始检测，1正在录制 2手动停止，3录制完成
        /// </summary>
        public int RecordStatus { get; set; }

        /// <summary>
        /// 主播在各平台的唯一标识
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 检测在线直播时，是否录制视频 is_auto_record  0否，1是
        /// </summary>
        public int IsAutoRecord { get; set; }

        /// <summary>
        /// 主播直播时的在线人数 
        /// </summary>
        public string OnlineNumber { get; set; }

        /// <summary>
        /// 当前正在直播的批次编号,batch_number 每次开启录播就会更新
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        ///当前录制的段数（第几段）
        /// </summary>
        public int Paragraph { get; set; }

        /// <summary>
        /// 时：分：秒  比如1小时20分34秒
        /// </summary>
        public string Duration { get; set; }

        /// <summary>
        /// 视频大小  (单位：M)
        /// </summary>
        public string VedioSizie { get; set; }

        /// <summary>
        /// 开始录制时间
        /// </summary>
        public string StartTime { get; set; }

        /// <summary>
        /// 当前的录制开始时间
        /// </summary>
        public string CurrentRecordStartTime { get; set; }

        /// <summary>
        /// 当前段落是否正则录制0否1是
        /// </summary>
        public int CurrentRecordStatus { get; set; }

        /// <summary>
        /// 主播的添加时间
        /// </summary>
        public string AddTime { get; set; }

        /// <summary>
        /// 当前主播今天的录制的视频总数
        /// </summary>
        //public int TodayRecordTotal { get; set; }

        /// <summary>
        /// 当前主播的录制总数
        /// </summary>
        //public int RecordTotal { get; set; }

        /// <summary>
        /// 总共的录制大小(M)
        /// </summary>
        //public string RecordSizeTotal { get; set; } 

        /// <summary>
        /// 今天录制的大小 （M）
        /// </summary>
        //public string RecordTodaySizeTotal { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }

        /// <summary>
        /// 是否保存弹幕 0保存，1不保存
        /// </summary>
        public int IsBarrageMonitoring { get; set; } = 0;

        /// <summary>
        /// 昨天场次数量
        /// </summary>
        //public int YesterdaySessionNum { get; set; }

        /// <summary>
        /// 昨天场次平均人数
        /// </summary>
        //public int YesterdaySessionAverageNum { get; set; }

        /// <summary>
        /// 环比增长 0.15表示增长15%  -0.123表示下降12.3%
        /// </summary>
        //public double YesterdaySessionRatio { get; set; }

        /// <summary>
        /// 昨日场次列表
        /// </summary>
        //public List<TotalOnlineNum> SessionList { get; set; }

        /// <summary>
        /// 是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除
        /// </summary>
        public int IsRemoveRecord { get; set; }

        /// <summary>
        /// 是否自动上传到云空间 0：否 1：是
        /// </summary>
        public int IsAutoUploadCloud { get; set; }

        /// <summary>
        /// 是否置顶 0：否 1：是
        /// </summary>
        public int IsTop { get; set; }

        /// <summary>
        /// 加入置顶的时间
        /// </summary>
        public string AddTopTime { get; set; }

        /// <summary>
        /// 最后开始录制时间
        /// </summary>
        public string LastRecordTime { get; set; }

        /// <summary>
        /// 昨日录制数据记录的时间戳
        /// </summary>
        public long? YesterdayRecordTime { get; set; }

        /// <summary>
        /// 昨日录制列表
        /// </summary>
        public List<AnchorYesterdayRecordItemVo> YesterdayRecordList { get; set; }

        /// <summary>
        /// 昨日录制数量
        /// </summary>
        public int YesterdayRecordNum { get; set; }
        /// <summary>
        /// 昨日平均场观
        /// </summary>
        public int YesterdayAverageObservationNum { get; set; }
        /// <summary>
        /// 昨日平均销售额区间范围-起始
        /// </summary>
        public int YesterdayAverageVolumeStart { get; set; }
        /// <summary>
        /// 昨日平均销售额区间范围-结束
        /// </summary>
        public int YesterdayAverageVolumeEnd { get; set; }

        /// <summary>
        /// 前日录制列表
        /// </summary>
        public List<AnchorYesterdayRecordItemVo> DayBeforeRecordList { get; set; }

        /// <summary>
        /// 前日录制数量
        /// </summary>
        public int DayBeforeRecordNum { get; set; }

        /// <summary>
        /// 前日平均场观
        /// </summary>
        public int DayBeforeAverageObservationNum { get; set; }
        /// <summary>
        /// 前日平均销售额区间范围-起始
        /// </summary>
        public int DayBeforeAverageVolumeStart { get; set; }
        /// <summary>
        /// 前日平均销售额区间范围-结束
        /// </summary>
        public int DayBeforeAverageVolumeEnd { get; set; }

        /// <summary>
        /// 是否开启数据看板 0：否 1：是
        /// </summary>
        public int IsDataViewing { get; set; }
        /// <summary>
        /// 录制时间，如：06:00:00-19:00:00
        /// </summary>
        public string RecordTime { get; set; }
        /// <summary>
        /// 是否在录制时间段内
        /// </summary>
        public bool InRecordTime { get; set; }
        /// <summary>
        /// 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒
        /// </summary>
        public int SmsTip { get; set; }

        /// <summary>
        /// 主播备注名称
        /// </summary>
        public string RemarksName { get; set; }

        /// <summary>
        /// 是否自动诊断 0：否 1：是
        /// </summary>
        public int? isAutoDiagnosis { get; set; }

        /// <summary>
        /// 诊断参数
        /// </summary>
        public DiagnosisParams diagnosisParams { get; set; }
        
        /// <summary>
        /// 是否开启数据诊断 0：否 1：是
        /// </summary>
        public int? isDataDiagnosis { get; set; }

        /// <summary>
        /// 数据诊断参数
        /// </summary>
        public DiagnosisParams dataDiagnosisParams { get; set; }

        /// <summary>
        /// 巨量百应授权状态 0：未授权 1：已授权 2：授权过期
        /// </summary>
        public int juliangAuthStatus;
        /// <summary>
        /// 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int qianchuanAuthStatus { get; set; }

        /// <summary>
        /// 来客授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int lifeAuthStatus { get; set; }

        /// <summary>
        /// 企业号授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 6：子账号无权限
        /// </summary>
        public int enterpriseAuthStatus { get; set; }

        /// <summary>
        /// 主播后台授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中
        /// </summary>
        public int anchorLiveAuthStatus { get; set; }

        /// <summary>
        /// 平台类型 0：抖音 1：快手 2：视频号
        /// </summary>
        public int platform { get; set; }
        /// <summary>
        /// 从录制列表移除主播的时间
        /// </summary>
        public string deleteDate { get; set; }
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
        /// 微信视频号授权状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权
        /// </summary>
        [JsonProperty("authChannelStatus")]
        public int WeChatChannelsAuthStatus { get; set; }
        
        
        
        
        
        
        /// <summary>
        /// 账号归属类型 0：自有账号 1：同行账号
        /// </summary>
        public int AccountType { get; set; }
        
        /// <summary>
        /// 首播日期
        /// </summary>
        public string premiereDate { get; set; }
        
        /// <summary>
        /// 账号阶段，使用字典account_stage的值
        /// </summary>
        public int? accountStage { get; set; }
        
        /// <summary>
        /// 账号水平， 使用字典account_water_level的值
        /// </summary>
        public int? accountWaterLevel { get; set; }

        /// <summary>
        /// 浏览结构，使用字典 account_flow的值
        /// </summary>
        public int? accountFlow { get; set; }
        
        /// <summary>
        /// 直播目标，使用字典living_target的值
        /// </summary>
        public int? livingTarget { get; set; }
        
        /// <summary>
        /// 直播方式，使用字典living_modality的值
        /// </summary>
        public int? livingModality { get; set; }
        
        /// <summary>
        /// 营销方式，使用字典marketing的值
        /// </summary>
        public int? marketing { get; set; }
        
        /// <summary>
        /// 优化方向，使用字典optimize_direction的值
        /// </summary>
        public string optimizeDirection { get; set; }
        
        /// <summary>
        /// 学习方向，使用字典learning的值
        /// </summary>
        public string learning { get; set; }
        
        /// <summary>
        /// 直播模式，使用字典living_mode的值
        /// </summary>
        public int? livingMode { get; set; }
        
        /// <summary>
        /// 主播账号情况描述
        /// </summary>
        public string AnchorSituation { get; set; }
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
        /// 排班状态：0 = 未排班（isScheduleRecord != 1），1 = 不在排班时间内（已排班但当前不在排班时间窗口内），2 = 在排班时间内（已排班且当前在排班时间窗口内）
        /// </summary>
        public int ScheduleStatus { get; set; }
    }
}
