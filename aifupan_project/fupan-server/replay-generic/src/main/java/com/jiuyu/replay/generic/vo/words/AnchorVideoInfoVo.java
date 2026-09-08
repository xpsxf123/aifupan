package com.jiuyu.replay.generic.vo.words;

import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AnchorVideoInfoVo {
    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 视频唯一标识
     */
    @Schema(description = "视频唯一标识")
    private String videoId;
    /**
     * 视频名称
     */
    @Schema(description = "视频名称")
    private String videoName;
    /**
     * 开始录制时间
     */
    @Schema(description = "开始录制时间")
    private Date startTime;
    /**
     * 第几段
     */
    @Schema(description = "第几段")
    private Integer paragraph;
    /**
     * 分段类型 0不分段，1按时长分段，2按大小分段
     */
    @Schema(description = "分段类型 0不分段，1按时长分段，2按大小分段")
    private Integer subsectionType;
    /**
     * 视频时长
     */
    @Schema(description = "视频时长")
    private Long duration;
    /**
     * 视频大小，单位：B
     */
    @Schema(description = "视频大小，单位：B")
    private Long vedioSizie;
    /**
     * 结束录制时间
     */
    @Schema(description = "结束录制时间")
    private Date endTime ;
    /**
     * 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id
     */
    @Schema(description = "归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id")
    private String batchNumber;
    /**
     * 视频类型  0 ts,1 flv,2 mp4
     */
    @Schema(description = "视频类型  0 ts,1 flv,2 mp4")
    private Integer videoType;
    /**
     * 清晰度 0 标清 1高清 2超清 3 蓝光
     */
    @Schema(description = "清晰度 0 标清 1高清 2超清 3 蓝光")
    private Integer definition;
    /**
     * 存储路径
     */
    @Schema(description = "存储路径")
    private String  storagePath;
    /**
     * 视频url
     */
    @Schema(description = "视频url")
    private String  liveUrl;
    /**
     * 分享地址
     */
    @Schema(description = "分享地址")
    private String  shareUrl;
    /**
     * 在线播放地址
     */
    @Schema(description ="在线播放地址")
    private String playUrl;
    /**
     * 直播源地址
     */
    @Schema(description = "直播源地址")
    private String  sourceUrl;
    /**
     * 主播表主键
     */
    @Schema(description = "主播表主键")
    private Integer  anchorId;
    /**
     * 是否正在录制 0否 1是
     */
    @Schema(description = "是否正在录制 0否 1是")
    private Integer  isRecording;
    /**
     * 直播源类型 0 m3u8  1  flv
     */
    @Schema(description = "直播源类型 0 m3u8  1  flv")
    private Integer  sourceType;
    /**
     * 直播标题
     */
    @Schema(description = "直播标题")
    private String   liveTitle;
    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer  analysisStatus;
    /**
     * 分析时间
     */
    @Schema(description = "分析时间")
    private String  analysisTime;
    /**
     * 错误原因
     */
    @Schema(description = "错误原因")
    private String  errorReason;
    /**
     * 主播url表的唯一标识
     */
    @Schema(description = " 主播url表的唯一标识")
    private String  secUid;
    /**
     * 行业id
     */
    @Schema(description ="行业id")
    private Long  tradeId;
    /**
     * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     */
    @Schema(description ="平台类型 0：全平台 1：抖音 2：快手 3：视频号")
    private String  platformType;
    /**
     * 用户id
     */
    @Schema(description ="用户id")
    private Long userId;
    /**
     * 上传的用户昵称
     */
    @Schema(description = "上传的用户昵称")
    private String userNickName;
    /**
     * 类型 1 代表已经录制未上传分享  2 上传分享
     */
    @Schema(description ="类型 1 代表已经录制未上传分享  2 上传分享")
    private Integer type;
    /**
     * 创建时间
     */
    @Schema(description ="创建时间")
    private Date createDate;
    /**
     * 更新时间
     */
    @Schema(description ="更新时间")
    private Date updateDate;
    /**
     * 视频上传状态 0：未上传 1：已上传
     */
    @Schema(description ="视频上传状态 0：未上传 1：已上传")
    private Integer uploadStatus;
    /**
     * 是否已标注敏感词 0：未标注 1：已标注
     */
    @Schema(description ="是否已标注敏感词 0：未标注 1：已标注")
    private Integer isMark;
    /**
     * 占用云空间的大小，单位：M
     */
    @Schema(description ="占用云空间的大小，单位：M")
    private Integer cloudStore;
    /**
     * 视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
     */
    @Schema(description ="视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除")
    private Integer deleteStatus;
    /**
     * 视频的租户id
     */
    @Schema(description ="视频的租户id")
    private Long tenantId;
    /**
     * 分析使用的资源 0：智能分析时长 1：文案提取
     */
    @Schema(description ="分析使用的资源 0：智能分析时长 1：文案提取")
    private Integer useAnalysisPropertyType;
    /**
     * 视频录制的异常状态 0：未出现异常 7：录制网络异常
     */
    @Schema(description ="视频录制的异常状态 0：未出现异常 7：录制网络异常")
    private Integer  recordErrorStatus;
    /**
     * 本地视频文件删除状态 0：未删除 1：已删除
     */
    @Schema(description ="本地视频文件删除状态 0：未删除 1：已删除")
    private Integer localVideoStatus;
    /**
     * 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description ="视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频")
    private Integer videoSliceType;
    /**
     * 观看人数
     */
    @Schema(description ="观看人数")
    private Integer viewersNum;
    /**
     * 观看人数
     */
    @Schema(description ="观看人数")
    private Integer observationNum;
    /**
     * 最高在线
     */
    @Schema(description = "最高在线")
    private Integer onlineMaxNum;
    /**
     * 销售额区间范围-起始(单位:元)
     */
    @Schema(description = "销售额区间范围-起始(单位:元)")
    private Integer volumeStart;
    /**
     * 销售额区间范围-结束(单位:元)
     */
    @Schema(description = "销售额区间范围-结束(单位:元)")
    private Integer volumeEnd;
    /**
     * 主播信息
     */
    @Schema(description ="主播信息")
    private AnchorUrlInfoVo anchorInfo;

    @Schema(description = "基础设置")
    private BasicSettingsVo basicSettingsVo;
//    /**
//     * 用户主播信息
//     */
//    @Schema(description ="用户主播信息")
//    private AnchorUrlUserVo anchorUserInfo;
    /**
     * 分析信息
     */
    @Schema(description = "分析信息")
    private VideoAnalysisRecordInfoVo recordInfo;
    /**
     * 是否存在弹幕 0：否 1：是
     */
    @Schema(description = "是否存在弹幕 0：否 1：是")
    private Integer existBarrage;
    /**
     * 是否存在数据看板 0：否 1：是
     */
    @Schema(description = "是否存在数据看板 0：否 1：是")
    private Integer existDataBoard;
    /**
     * 是否存在诊断报告 0：否 1：是
     */
    @Schema(description = "是否存在诊断报告 0：否 1：是")
    private Integer hasDiagnosisReport;
    /**
     * 最新诊断报告文件名称
     */
    @Schema(description = "最新诊断报告文件名称")
    private String  diagnosisOssName;
    /**
     * 是否有数据诊断报告
     */
    @Schema(description = "是否有数据诊断报告")
    private Integer hasDataDiagnosisReport;
    /**
     * 最新数据诊断报告文件名称
     */
    @Schema(description = "最新数据诊断报告文件名称")
    private String dataDiagnosisOssName;

    /**
     * 数据诊断已读状态 0未读 1已读
     */
    @Schema(description = "数据诊断已读状态 0未读 1已读")
    private Integer isDataDiagnosisRead;


    /**
     * 是否有笔记小结
     */
    private Integer notesSummary = 0;

    /**
     * 笔记是否存在
     */
    private Integer existsNotes = 0;

    /**
     * 批注是否存在
     */
    private Integer existsMark = 0;

    /**
     * 切片视频的切片信息
     */
    @Schema(description = "切片视频的切片信息")
    private VideoSliceVo videoSliceInfo;

    /**
     * 切片视频所属原视频信息
     */
    @Schema(description = "切片视频所属原视频信息")
    private AnchorVideoInfoVo parentVideoInfo;

    /**
     * 原视频下的所有切片信息
     */
    @Schema(description = "原视频下的所有切片信息")
    private List<VideoSliceVo> sliceList;

    /**
     * 是否有AI优化目的 0：否 1：是
     */
    @Schema(description = "是否有AI优化目的 0：否 1：是")
    private Integer hasAiOptimizePurpose;

    /**
     * 是否设置了星标 0：否 1：是
     */
    @Schema(description = "是否设置了星标 0：否 1：是")
    private Integer hasStar;

    /**
     * 星标信息
     */
    @Schema(description = "星标信息")
    private SourceStarVo sourceStarInfo;

    /**
     * 云空间备注
     */
    @Schema(description ="云空间备注")
    private String cloudRemarks;
    /**
     * 重命名
     */
    @Schema(description ="重命名")
    private String videoRename;
    /**
     * 云空间重命名
     */
    @Schema(description ="云空间重命名")
    private String cloudRename;

    /**
     * 数据来源 0=正常获取 1=巨量拉取
     */
    @Schema(description = "数据来源 0=正常获取 1=巨量拉取")
    private Integer dataSource;
    /**
     * 视频是否下载 0=未下载 1=已下载
     */
    @Schema(description = "视频是否下载 0=未下载 1=已下载")
    private Integer isDownloaded;
}
