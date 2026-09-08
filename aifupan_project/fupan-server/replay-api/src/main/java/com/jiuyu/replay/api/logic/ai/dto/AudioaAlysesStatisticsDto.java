package com.jiuyu.replay.api.logic.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 音频分析统计数据传输对象
 *
 * @author liaoxin
 * @date 2025-01-27
 */
@Data
@Schema(description = "音频分析统计数据传输对象")
public class AudioaAlysesStatisticsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频开始时间 从0开始
     */
    @Schema(description = "视频开始时间 从0开始")
    private Long startTempTime;

    /**
     * 视频结束时间 从0开始
     */
    @Schema(description = "视频结束时间 从0开始")
    private Long endTempTime;

    /**
     * 视频开始时间 自然时间戳
     */
    @Schema(description = "视频开始时间 自然时间戳")
    private Long startVideoTime;

    /**
     * 视频结束时间 自然时间戳
     */
    @Schema(description = "视频结束时间 自然时间戳")
    private Long endVideoTime;

    /**
     * 视频的自然开始时间
     */
    @Schema(description = "视频的自然开始时间")
    private Date startTime;

    /**
     * 视频的自然结束时间
     */
    @Schema(description = "视频的自然结束时间")
    private Date endTime;

    /**
     * 视频的开始时间 HH:mm:ss
     */
    @Schema(description = "视频的开始时间 HH:mm:ss")
    private String textStart;

    /**
     * 视频的结束时间 HH:mm:ss
     */
    @Schema(description = "视频的结束时间 HH:mm:ss")
    private String textEnd;

    /**
     * 一段的在线人数
     */
    @Schema(description = "一段的在线人数")
    private String renShu;

    /**
     * 一段的内容
     */
    @Schema(description = "一段的内容")
    private String content;
}