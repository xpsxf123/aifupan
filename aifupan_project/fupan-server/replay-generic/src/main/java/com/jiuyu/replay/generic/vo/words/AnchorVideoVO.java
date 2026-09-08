package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "分享视频VO")
public class AnchorVideoVO  implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 当前页
     */
    @Schema(description = "当前页")
    private Integer page;
    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数")
    private Integer limit;
    /**
     * 分享视频分析内容
     */
    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;

    @Schema(description = "主播Id")
    private Long anchorUrlId;

    @Schema(description = "搜索")
    private String keyword;

    @Schema(description = "上传视频或录制视频时上传的id 必须上传")
    private String videoId;
    @Schema(description = "视频名称")
    private String videoName;
    @Schema(description = "开始时间")
    private Date startTime;
    @Schema(description = "第几段")
    private Integer paragraph;
    @Schema(description = "分段类型 0不分段，1按时长分段，2按大小分段")
    private Integer subsectionType;
    @Schema(description = "视频时长")
    private Long duration;
    @Schema(description = "视频大小，单位：B")
    private Long vedioSizie;
    @Schema(description = "结束时间")
    private Date endTime ;
    @Schema(description = "归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id")
    private String batchNumber;
    @Schema(description = "视频类型  0 ts,1 flv,2 mp4")
    private Integer videoType;
    @Schema(description = "清晰度 0 标清 1高清 2超清 3 蓝光")
    private Integer definition;
    @Schema(description = "存储路径")
    private String  storagePath;
    @Schema(description = "视频url")
    private String  liveUrl;
    /**
     * 在线复盘url
     */
    @Schema(description = "在线复盘url")
    private String  shareUrl;
    /**
     * 在线播放地址
     */
    @Schema(description ="在线播放地址")
    private String playUrl;
    @Schema(description = "直播源地址")
    private String  sourceUrl;
    @Schema(description = "主播表主键")
    private Integer  anchorId;
    @Schema(description = "是否正在录制 0否 1是")
    private Integer  isRecording;
    @Schema(description = "直播源类型 0 m3u8  1  flv")
    private Integer  sourceType;
    @Schema(description = "直播标题")
    private String   liveTitle;
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer  analysisStatus;
    @Schema(description = "场观人次大于大于等于")
    private Integer totalWatchNum;
    @Schema(description = "销售额区间范围-起始(单位:元)")
    private Integer volumeStart;
    @Schema(description = "销售额区间范围-结束(单位:元)")
    private Integer volumeEnd;
    /**
     * 是否存在数据看板数据 0：不存在 1：存在
     */
    @Schema(description = "是否存在数据看板数据 0：不存在 1：存在")
    private Integer existDataView;


    private String  analysisTime;

    @Schema(description = "错误原因")
    private String  errorReason;
    @Schema(description = " 主播url表的唯一标识")
    private String  secUid;

    @Schema(description ="行业id")
    private Long  tradeId;
    @Schema(description ="平台类型 0：全平台 1：抖音 2：快手 3：视频号")
    private String  platformType;
    @Schema(description ="录制的id")
    private Long userId;

    @Schema(description ="类型 1 代表已经录制未上传分享  2 上传分享")
    private Integer type;

    @Schema(description ="创建时间")
    private Date createDate;

    @Schema(description ="更新时间")
    private Date updateDate;

    @Schema(description ="行业名称")
    private String  tradeName;
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
    private Integer  useAnalysisPropertyType;
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
     * 用户账号
     */
    @Schema(description ="用户账号")
    private String userName;

    /**
     * 分析次数
     */
    @Schema(description = "分析次数")
    private Integer analysisStatusVersion;

    /**
     * 总观看人次
     */
    @Schema(description = "总观看人次")
    private String totalOnlineNum;
    /**
     * 分析出来的关键词总数
     */
    @Schema(description = "分析出来的关键词总数")
    private Integer sensitiveWordTotal;

    /**
     * 未匹配上词库的关键词个数
     */
    @Schema(description = "未匹配上词库的关键词个数")
    private Integer sensitiveWordMark;

    /**
     * 用户销售人员
     */
    @Schema(description = "用户销售人员")
    private String userSales;

    @Schema(description = "版本名称")
    private String packageName;

    @Schema(description = "版本过期时间")
    private Date packageExpiredTime;

    @Schema(description = "账户类型 0：自有账号 1：同行业账号")
    private Integer accountType;

    /**
     * 未在词库的词语列表
     */
    @Schema(description = "未在词库的词语列表")
    private List<AiAnalysisSensitiveRelaInfoVo> notMarkWordList;
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
}

