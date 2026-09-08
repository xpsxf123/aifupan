package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文字内容")
public class WordsMarkBo {

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
     * 文字内容
     */
    @Schema(description = "文字内容")
    private String content;
    /**
     * 当前是第几段，从1开始
     */
    @Schema(description = "当前是第几段，从1开始")
    private Integer currentSort;
    /**
     * 词语列表
     */
    @Schema(description = "词语列表")
    private List<WordsMarkItemBo> items;
    /**
     * 是否是最后一段 0：否 1：是
     */
    @Schema(description = "是否是最后一段 0：否 1：是")
    private Integer isLast;
    /**
     * 是否是二次重新分析 0：否 1：是
     */
    @Schema(description = "是否是二次重新分析 0：否 1：是")
    private Integer isReAnalysis;
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
