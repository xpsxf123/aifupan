package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tb_anchor_url_user")
public class AnchorUrlUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 主播secUid
     */
    private String anchorUrlSecUid;
    /**
     * 在线时,是否自动录制分析
     */
    private Integer isAutoRecord;
    /**
     * 是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除
     */
    private Integer isRemoveRecord;
    /**
     * 是否开启弹幕监控 0否， 1是
     */
    private Integer isBarrageMonitoring;
    /**
     * 是否自动上传到云空间 0：否 1：是
     */
    private Integer isAutoUploadCloud;
    /**
     * 是否置顶 0：否 1：是
     */
    private Integer isTop;
    /**
     * 是否自动诊断 0：否 1：是
     */
    private Integer isAutoDiagnosis;
    /**
     * 加入置顶的时间
     */
    private String addTopTime;
    /**
     * 最后开始录制时间
     */
    private String lastRecordTime;
    /**
     * 主播账号情况描述
     */
    private String anchorSituation;
    /**
     * 录制时间，如：06:00:00-19:00:00
     */
    private String recordTime;
    /**
     * 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒
     */
    private Integer smsTip;
    /**
     * 是否开启数据看板 0：否 1：是
     */
    private Integer isDataViewing;
    /**
     * 账号归属类型 0：自由账号 1：同行账号
     */
    private Integer accountType;
    /**
     * 文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)
     */
    private String folderName;
    /**
     * 主播备注名称
     */
    private String remarksName;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 最后修改时间
     */
    private Date updateDate;
    /**
     * 是否已删除
     */
    private Integer isDeleted;
    /**
     * 行业id
     */
    private Long tradeId;
    /**
     * 租户id
     */
    private Long tenantId;
    /**
     * 账号阶段，使用字典account_stage的值
     */
    private Integer accountStage;
    /**
     * 账号水平， 使用字典account_water_level的值
     */
    private Integer accountWaterLevel;

    /**
     * 流量结构，使用字典 account_flow的值
     */
    private Integer accountFlow;

    /**
     * 从录制列表移除主播的时间
     */
    private Date deleteDate;
    /**
     * 录制的清晰度 -1：跟随系统 0 标清 1高清 2超清 3 蓝光
     */
    private Integer recordDefinition;
    /**
     * 录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制(只录一段) 2：时长分段录制
     */
    private Integer recordLimitType;
    /**
     * 录制的时长，单位：分钟
     */
    private Integer recordLimitValue;
    /**
     * 是否自动分析视频 0：否 1：是
     */
    private Integer isAutoAnalysis;
    /**
     * 授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authJlbyStatus;
    /**
     * 纯录制版是否获取在线人数 0：否 1：是
     */
    private Integer pureRecordOnlineNum;
    /**
     * 授权巨量百应状态修改时间
     */
    private Date authJlbyStatusTime;
    /**
     * 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authQcStatus;
    /**
     * 千川授权状态修改时间
     */
    private Date authQcStatusTime;
    /**
     * 授权微信视频号状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权
     */
    private Integer authChannelStatus;
    /**
     * 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authLifeStatus;
    /**
     * 授权来客状态修改时间
     */
    private Date authLifeStatusTime;
    /**
     * 是否自动数据诊断  0：否 1：是
     */
    private Integer isDataDiagnosis;
    /**
     * 自动生成数据诊断剩余场次
     */
    private Integer diagnosisGenerateNum;
    /**
     * 一句话识别引擎模型，如：16k_zh
     */
    private String engSerViceType;
    /**
     * 是否按排班录制 0：否 1：是
     */
    private Integer isScheduleRecord;
    /**
     * 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认） 2：按时间点分段录制
     */
    private Integer recordTimeMode;
    /**
     * 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔
     */
    private String segmentTimePoints;
    /**
     * 是否统计业绩 0-否 1-是
     */
    private Integer isStatisticsPerformance;
    /**
     * 话术质检开关 0否 1是
     */
    private Integer isScriptQualityInspection;
    /**
     * 话术还原度开关 0否 1是
     */
    private Integer isScriptFidelityMonitor;
    /**
     * 互动巡检开关 0否 1是
     */
    private Integer isInteractionPatrol;
    /**
     * 当前已确认标准稿ID → tb_standard_script.id
     */
    private Long standardScriptId;
    /**
     * 自动删除时间 -1不删除 0马上删除 N天后删除，空=跟随全局配置（使用字典 auto_delete_time 的值）
     */
    private String autoDeleteTime;
    /**
     * 删除内容 ts源视频/mp4成品/all都删，空=跟随全局配置（使用字典 delete_content 的值）
     */
    private String deleteContent;
}
