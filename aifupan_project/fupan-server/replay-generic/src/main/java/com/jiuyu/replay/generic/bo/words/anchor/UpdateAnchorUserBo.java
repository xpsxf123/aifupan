package com.jiuyu.replay.generic.bo.words.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tisheng
 * @date 2024/9/14
 * @apinNote
 */
@Data
@Schema(description = "修改用户主播信息")
public class UpdateAnchorUserBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主播唯一标识
     */
    @NotBlank(message = "主播标识不能为空")
    @Schema(description = "主播唯一标识")
    private String secUid;
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
     * 主播账号情况描述
     */
    @Schema(description = "主播账号情况描述")
    private String anchorSituation;
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
     * 账号归属类型 0：自由账号 1：同行账号
     */
    @Schema(description = "账号归属类型 0：自由账号 1：同行账号")
    private Integer accountType;
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
     * 账号阶段，使用字典account_stage的值
     */
    @Schema(description = "账号阶段，使用字典account_stage的值")
    private Integer accountStage;
    /**
     * 账号水平， 使用字典account_water_level的值
     */
    @Schema(description = "账号水平， 使用字典account_water_level的值")
    private Integer accountWaterLevel;
    /**
     * 流量结构，使用字典 account_flow的值
     */
    @Schema(description = "流量结构，使用字典 account_flow的值")
    private Integer accountFlow;
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
     * 纯录制版是否获取在线人数 0：否 1：是
     */
    @Schema(description = "纯录制版是否获取在线人数 0：否 1：是")
    private Integer pureRecordOnlineNum;

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
     * 是否统计业绩 0-否 1-是
     */
    @Schema(description = "是否统计业绩 0-否 1-是")
    private Integer isStatisticsPerformance;

}
