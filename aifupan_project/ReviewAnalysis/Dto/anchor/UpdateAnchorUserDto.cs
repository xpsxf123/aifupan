using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.anchor
{
    public class UpdateAnchorUserDto
    {
        /// <summary>
        /// 主播唯一标识
        /// </summary>
        public string secUid { get; set; }

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
        /// 主播账号情况描述
        /// </summary>
        public string anchorSituation { get; set; }

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
        /// 账号归属类型 0：自由账号 1：同行账号
        /// </summary>
        public int? accountType { get; set; }

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
        /// 账号阶段，使用字典account_stage的值
        /// </summary>
        public int? accountStage { get; set; }

        /// <summary>
        /// 账号水平， 使用字典account_water_level的值
        /// </summary>
        public int? accountWaterLevel { get; set; }

        /// <summary>
        /// 流量结构，使用字典 account_flow的值
        /// </summary>
        public int? accountFlow { get; set; }

        /// <summary>
        /// 是否自动诊断 0：否 1：是
        /// </summary>
        public int isAutoDiagnosis { get; set; }

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
        /// 录制时间模式 0：按视频时长录制（默认） 1：按北京时间录制
        /// </summary>
        public int? recordTimeMode { get; set; }
        /// <summary>
        /// 是否统计业绩 0-否 1-是
        /// </summary>
        public int? isStatisticsPerformance { get; set; }

        /// <summary>
        /// 授权千川状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int? authQcStatus { get; set; }

        /// <summary>
        /// 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int? authLifeStatus { get; set; }

        /// <summary>
        /// 授权企业号状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中
        /// </summary>
        public int? authEnterpriseStatus { get; set; }

        /// <summary>
        /// 授权主播后台状态 0：未授权 1：已授权 2：授权过期 3：需要刷新 4：授权中
        /// </summary>
        public int? authAnchorLiveStatus { get; set; }

    }
}
