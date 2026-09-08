package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 视频列表项。
 *
 * @author fupan-server
 */
@Data
public class VideoItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    private Integer platform;

    /**
     * 视频唯一标识（后续视频数据接口用它定位）
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
     * 主播 secUid
     */
    private String secUid;

    /**
     * 主播名称
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
     * 行业 id
     */
    private Long tradeId;

    /**
     * 行业名称
     */
    private String tradeName;

    /**
     * 自有/同行 code + 文案
     */
    private Integer accountType;
    private String accountTypeLabel;

    /**
     * 切片类型 code + 文案（0原视频/1复盘切片/2短视频切片）
     */
    private Integer videoSliceType;
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
     * 在线播放地址（云端视频播放 URL；本地未上传时为空）
     */
    private String playUrl;

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
     * 分析状态：0未分析 1分析中 2完成 3错误
     */
    private Integer analysisStatus;


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
