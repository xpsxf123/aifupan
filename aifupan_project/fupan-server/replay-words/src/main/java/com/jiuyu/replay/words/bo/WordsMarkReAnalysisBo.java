package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WordsMarkReAnalysisBo {

    /**
     * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     */
    @Schema(description = "平台类型 0：全平台 1：抖音 2：快手 3：视频号")
    private Integer platformType;
    /**
     * 行业id 1表示全行业
     */
    @Schema(description = "行业id 1表示全行业")
    private Long tradeId;
    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
    /**
     * 类型 0：视频 1：文件
     */
    @Schema(description = "类型 0：视频 1：文件")
    private Integer type;
    /**
     * 用户id(前端不需要传)
     */
    @Schema(description = "用户id(前端不需要传)")
    private Long userId;
}
