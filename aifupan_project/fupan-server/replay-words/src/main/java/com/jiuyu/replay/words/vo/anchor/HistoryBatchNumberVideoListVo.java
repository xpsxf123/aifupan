package com.jiuyu.replay.words.vo.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/18 下午9:16
 */
@Data
public class HistoryBatchNumberVideoListVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "直播批次号")
    private String batchNumber;
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
     * 视频时长
     */
    @Schema(description = "视频时长")
    private Long duration;
    /**
     * 段数
     */
    @Schema(description = "段数")
    private Integer paragraph;
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
     * 直播标题
     */
    @Schema(description = "直播标题")
    private String   liveTitle;
    /**
     * 分析时间
     */
    @Schema(description = "分析时间")
    private String  analysisTime;
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
     * 创建时间
     */
    @Schema(description ="创建时间")
    private Date createDate;
    /**
     * 更新时间
     */
    @Schema(description ="更新时间")
    private Date updateDate;

}
