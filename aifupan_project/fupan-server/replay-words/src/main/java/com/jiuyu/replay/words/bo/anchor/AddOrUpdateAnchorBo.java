package com.jiuyu.replay.words.bo.anchor;

import com.jiuyu.replay.generic.dto.words.BasicSettingsBaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "添加或修改用户的主播信息参数")
public class AddOrUpdateAnchorBo extends BasicSettingsBaseDto {

    /**
     * 主播唯一标识
     */
    @Schema(description = "主播唯一标识")
    private String secUid;
    /**
     * 主页url
     */
    @Schema(description = "主页url")
    private String homeUrl;
    /**
     * 直播间url
     */
    @Schema(description = "直播间url")
    private String liveUrl;
    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;
    /**
     * 主播头像
     */
    @Schema(description = "主播头像")
    private String anchorAvatar;
    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    @Schema(description = "平台类型 0：抖音 1：快手 2：视频号")
    private Integer platform;
    /**
     * DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
     */
    @Schema(description = "DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定")
    private String platformResource;
    /**
     * 主播userId
     */
    @Schema(description = "主播userId")
    private String anchorUserId;
    /**
     * webSocketId
     */
    @Schema(description = "webSocketId")
    private String webSocketId;
    /**
     * 主播抖音号
     */
    @Schema(description = "主播抖音号")
    private String anchorNumber;
    /**
     * 第三方数据平台是否已经收录 0:否 1:是
     */
    @Schema(description = "第三方数据平台是否已经收录 0:否 1:是")
    private Integer chanmamaInclude;
    /**
     * 在线时,是否自动录制分析
     */
    @Schema(description = "在线时,是否自动录制分析")
    private Integer isAutoRecord;
    /**
     * 是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除
     */
    @Schema(description = "是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除")
    private Integer isRemoveRecord;
    /**
     * 是否开启弹幕监控 0否， 1是
     */
    @Schema(description = "是否开启弹幕监控 0否， 1是")
    private Integer isBarrageMonitoring;
    /**
     * 是否自动上传到云空间 0：否 1：是
     */
    @Schema(description = "是否自动上传到云空间 0：否 1：是")
    private Integer isAutoUploadCloud;
    /**
     * 是否置顶 0：否 1：是
     */
    @Schema(description = "是否置顶 0：否 1：是")
    private Integer isTop;
    /**
     * 是否自动诊断 0：否 1：是
     */
    @Schema(description = "是否自动诊断 0：否 1：是")
    private Integer isAutoDiagnosis;
    /**
     * 加入置顶的时间
     */
    @Schema(description = "加入置顶的时间")
    private String addTopTime;
    /**
     * 最后开始录制时间
     */
    @Schema(description = "最后开始录制时间")
    private String lastRecordTime;
    /**
     * 录制时间，如：06:00:00-19:00:00
     */
    @Schema(description = "录制时间，如：06:00:00-19:00:00")
    private String recordTime;
    /**
     * 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒
     */
    @Schema(description = "上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒")
    private Integer smsTip;
    /**
     * 是否开启数据看板 0：否 1：是
     */
    @Schema(description = "是否开启数据看板 0：否 1：是")
    private Integer isDataViewing;

    /**
     * 文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)
     */
    @Schema(description = "文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)")
    private String folderName;
    /**
     * 主播备注名称
     */
    @Schema(description = "主播备注名称")
    private String remarksName;
    /**
     * 行业ID
     */
    @Schema(description = "行业ID")
    private Long tradeId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 诊断参数
     */
    @Schema(description = "诊断参数")
    private DiagnosisParams diagnosisParams;
    /**
     * 录制的清晰度 -1：跟随系统 0 标清 1高清 2超清 3 蓝光
     */
    @Schema(description = "录制的清晰度 -1：跟随系统 0 标清 1高清 2超清 3 蓝光")
    private Integer recordDefinition;
    /**
     * 录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制(只录一段) 2：时长分段录制
     */
    @Schema(description = "录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制(只录一段) 2：时长分段录制")
    private Integer recordLimitType;
    /**
     * 录制的时长，单位：分钟
     */
    @Schema(description = "录制的时长，单位：分钟")
    private Integer recordLimitValue;
    /**
     * 是否自动分析视频 0：否 1：是
     */
    @Schema(description = "是否自动分析视频 0：否 1：是")
    private Integer isAutoAnalysis;
    /**
     * 授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    @Schema(description = "授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer authJlbyStatus;
    /**
     * 纯录制版是否获取在线人数 0：否 1：是
     */
    @Schema(description = "纯录制版是否获取在线人数 0：否 1：是")
    private Integer pureRecordOnlineNum;

    /**
     * 授权巨量百应状态修改时间
     */
    @Schema(description = "授权巨量百应状态修改时间")
    private Date authJlbyStatusTime;

    /**
     * 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    @Schema(description = "千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer authQcStatus;

    /**
     * 千川授权状态修改时间
     */
    @Schema(description = "千川授权状态修改时间")
    private Date authQcStatusTime;

    /**
     * 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    @Schema(description = "授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer authLifeStatus;

    /**
     * 授权来客状态修改时间
     */
    @Schema(description = "授权来客状态修改时间")
    private Date authLifeStatusTime;

    /**
     * 是否开启数据诊断 0：否 1：是
     */
    @Schema(description = "是否开启数据诊断 0：否 1：是")
    public Integer isDataDiagnosis;

    /**
     * 自动生成数据诊断剩余场次
     */
    @Schema(description = "自动生成数据诊断剩余场次")
    private Integer diagnosisGenerateNum;
    /**
     * 一句话识别引擎模型，如：16k_zh
     */
    @Schema(description = "一句话识别引擎模型，如：16k_zh")
    private String engSerViceType;

    /**
     * 是否按排班录制 0：否 1：是
     */
    @Schema(description = "是否按排班录制 0：否 1：是")
    private Integer isScheduleRecord;

    /**
     * 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制
     */
    @Schema(description = "录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制")
    private Integer recordTimeMode;

    /**
     * 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔
     */
    @Schema(description = "时间点分段录制的时间点列表，格式：09:30,10:40,11:20，多个时间点用逗号分隔")
    private String segmentTimePoints;

    /**
     * 数据诊断参数
     */
    @Schema(description = "数据诊断参数")
    public DiagnosisParams dataDiagnosisParams;

    /**
     * 是否统计业绩 0-否 1-是
     */
    @Schema(description = "是否统计业绩 0-否 1-是")
    private Integer isStatisticsPerformance;

    /**
     * 话术质检开关 0否 1是（null 表示不变更）
     */
    @Schema(description = "话术质检开关 0否 1是，null不变更")
    private Integer isScriptQualityInspection;

    /**
     * 话术还原度开关 0否 1是（null 表示不变更，当前 B3 前传 1 会被拦截）
     */
    @Schema(description = "话术还原度开关 0否 1是，null不变更")
    private Integer isScriptFidelityMonitor;

    /**
     * 互动巡检开关 0否 1是（null 表示不变更）
     */
    @Schema(description = "互动巡检开关 0否 1是，null不变更")
    private Integer isInteractionPatrol;

    /**
     * 标准稿 ID（B3 前透传写库，无实际校验）
     */
    @Schema(description = "标准稿ID")
    private Long standardScriptId;

    /**
     * 自动删除时间 -1不删除 0马上删除 N天后删除，空=跟随全局配置（使用字典 auto_delete_time 的值）
     */
    @Schema(description = "自动删除时间 -1不删除 0马上删除 N天后删除，空=跟随全局配置")
    private String autoDeleteTime;

    /**
     * 删除内容 ts源视频/mp4成品/all都删，空=跟随全局配置（使用字典 delete_content 的值）
     */
    @Schema(description = "删除内容 ts源视频/mp4成品/all都删，空=跟随全局配置")
    private String deleteContent;

    @Data
    public static class DiagnosisParams {

        @Schema(description = "模型id")
        private Long modelId;

        @Schema(description = "问题ids")
        private List<Long> cueWordsIds;
    }
}

