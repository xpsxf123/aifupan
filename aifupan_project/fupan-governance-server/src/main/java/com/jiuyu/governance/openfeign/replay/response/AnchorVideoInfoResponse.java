package com.jiuyu.governance.openfeign.replay.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;

/**
 * 爱复盘视频信息响应（对应 fupan-server AnchorVideoInfoVo 的简单字段）
 *
 * <p>仅包含简单类型字段，嵌套对象（如 anchorInfo、basicSettingsVo 等）未映射，
 * 由 Jackson 在反序列化时自动忽略。使用时按需调用 getter（如 getTenantId()）。</p>
 *
 * @author AI Assistant
 * @date 2026-08-03
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnchorVideoInfoResponse {

    /** ID */
    private Long id;

    /** 视频唯一标识 */
    private String videoId;


    /** 视频名称 */
    private String videoName;

    /** 开始录制时间 */
    private Date startTime;

    /** 第几段 */
    private Integer paragraph;

    /** 分段类型 0不分段，1按时长分段，2按大小分段 */
    private Integer subsectionType;

    /** 视频时长 */
    private Long duration;

    /** 视频大小，单位：B */
    private Long vedioSizie;

    /** 结束录制时间 */
    private Date endTime;

    /** 归属批次号 */
    private String batchNumber;

    /** 视频类型 0 ts,1 flv,2 mp4 */
    private Integer videoType;

    /** 清晰度 0 标清 1高清 2超清 3 蓝光 */
    private Integer definition;

    /** 存储路径 */
    private String storagePath;

    /** 视频url */
    private String liveUrl;

    /** 分享地址 */
    private String shareUrl;

    /** 在线播放地址 */
    private String playUrl;

    /** 直播源地址 */
    private String sourceUrl;

    /** 主播表主键 */
    private Integer anchorId;

    /** 是否正在录制 0否 1是 */
    private Integer isRecording;

    /** 直播源类型 0 m3u8 1 flv */
    private Integer sourceType;

    /** 直播标题 */
    private String liveTitle;

    /** 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误 */
    private Integer analysisStatus;

    /** 分析时间 */
    private String analysisTime;

    /** 错误原因 */
    private String errorReason;

    /** 主播url表的唯一标识 */
    private String secUid;

    /** 行业id */
    private Long tradeId;

    /** 平台类型 */
    private String platformType;

    /** 用户id */
    private Long userId;

    /** 上传的用户昵称 */
    private String userNickName;

    /** 类型 1 代表已经录制未上传分享 2 上传分享 */
    private Integer type;

    /** 创建时间 */
    private Date createDate;

    /** 更新时间 */
    private Date updateDate;

    /** 视频上传状态 0：未上传 1：已上传 */
    private Integer uploadStatus;

    /** 是否已标注敏感词 0：未标注 1：已标注 */
    private Integer isMark;

    /** 占用云空间的大小，单位：M */
    private Integer cloudStore;

    /** 视频删除状态 */
    private Integer deleteStatus;

    /** 视频的租户id */
    private Long tenantId;

    /** 分析使用的资源 0：智能分析时长 1：文案提取 */
    private Integer useAnalysisPropertyType;

    /** 视频录制的异常状态 */
    private Integer recordErrorStatus;

    /** 本地视频文件删除状态 */
    private Integer localVideoStatus;

    /** 视频切片类型 */
    private Integer videoSliceType;

    /** 观看人数 */
    private Integer viewersNum;

    /** 观察人数 */
    private Integer observationNum;

    /** 最高在线 */
    private Integer onlineMaxNum;

    /** 销售额区间范围-起始(单位:元) */
    private Integer volumeStart;

    /** 销售额区间范围-结束(单位:元) */
    private Integer volumeEnd;

    /** 是否存在弹幕 0：否 1：是 */
    private Integer existBarrage;

    /** 是否存在数据看板 0：否 1：是 */
    private Integer existDataBoard;

    /** 是否存在诊断报告 0：否 1：是 */
    private Integer hasDiagnosisReport;

    /** 最新诊断报告文件名称 */
    private String diagnosisOssName;

    /** 是否有数据诊断报告 */
    private Integer hasDataDiagnosisReport;

    /** 最新数据诊断报告文件名称 */
    private String dataDiagnosisOssName;

    /** 数据诊断已读状态 0未读 1已读 */
    private Integer isDataDiagnosisRead;

    /** 是否有笔记小结 */
    private Integer notesSummary;

    /** 笔记是否存在 */
    private Integer existsNotes;

    /** 批注是否存在 */
    private Integer existsMark;

    /** 是否有AI优化目的 0：否 1：是 */
    private Integer hasAiOptimizePurpose;

    /** 是否设置了星标 0：否 1：是 */
    private Integer hasStar;

    /** 云空间备注 */
    private String cloudRemarks;

    /** 重命名 */
    private String videoRename;

    /** 云空间重命名 */
    private String cloudRename;

    /** 数据来源 0=正常获取 1=巨量拉取 */
    private Integer dataSource;

    /** 视频是否下载 0=未下载 1=已下载 */
    private Integer isDownloaded;
}
