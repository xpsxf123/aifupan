package com.jiuyu.governance.openfeign.replay.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频列表项（6.4 响应 data.list 元素）
 *
 * @author HeHui
 * @date 2026-06-14
 */
@Getter
@Setter
public class AnchorVideoListItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    private Integer platform;

    /**
     * 视频唯一标识（后续视频接口用它定位）
     */
    private String videoId;

    /**
     * 批次编号
     */
    private String batchNumber;

    /**
     * 第几段
     */
    private Integer paragraph;

    /**
     * 直播间 secUid
     */
    private String secUid;

    /**
     * 直播间名称
     */
    private String anchorName;

    /**
     * 视频名称
     */
    private String videoName;

    /**
     * 直播标题
     */
    private String liveTitle;

    /**
     * 行业 ID。序列化为字符串：值可能超出 JS Number 安全整数范围，避免前端精度丢失。
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long tradeId;

    /**
     * 行业名称
     */
    private String tradeName;

    /**
     * 归属类型 code
     */
    private Integer accountType;

    /**
     * 归属类型文案
     */
    private String accountTypeLabel;

    /**
     * 切片类型 code
     */
    private Integer videoSliceType;

    /**
     * 切片类型文案
     */
    private String videoSliceTypeLabel;

    /**
     * 录制开始时间
     */
    private String startTime;

    /**
     * 录制结束时间
     */
    private String endTime;

    /**
     * 时长（秒）
     */
    private Long duration;

    /**
     * 在线播放地址（本地未上传时为空）
     */
    private String playUrl;

    /**
     * 分析状态：0未分析 1分析中 2完成 3错误
     */
    private Integer analysisStatus;

    /**
     * 视频类型  0 ts,1 flv,2 mp4
     */
    private Integer videoType;
    /**
     * 清晰度 0 标清 1高清 2超清 3 蓝光
     */
    private Integer definition;
    /**
     * 存储路径
     */
    private String storagePath;


    /**
     * 是否有数据看板
     */
    private Boolean hasDashboard;

    /**
     * 是否有投放
     */
    private Boolean hasRoi;

    /**
     * 是否有弹幕
     */
    private Boolean hasBarrages;

    /**
     * 是否有在线曲线
     */
    private Boolean hasChartData;

    /**
     * 观看人数
     */
    private String watchNum;

    /**
     * 弹幕总数
     */
    private Integer totalBarrageNum;

    /**
     * 最高在线人数
     */
    private Integer onlineMaxNum;

    /**
     * 重命名
     */
    private String videoRename;
}
