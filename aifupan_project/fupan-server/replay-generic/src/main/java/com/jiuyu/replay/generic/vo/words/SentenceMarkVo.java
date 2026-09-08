package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文字内容")
public class SentenceMarkVo {

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;
    /**
     * 当前是第几段，从1开始
     */
    @Schema(description = "当前是第几段，从1开始")
    private Integer currentSort;
    /**
     * 文字内容
     */
    @Schema(description = "文字内容")
    private String content;
    /**
     * 词语列表
     */
    @Schema(description = "词语列表")
    private List<WordListItemVo> items;
    /**
     * 关键词/敏感词列表
     */
    @Schema(description = "关键词/敏感词列表")
    private List<WordsMarkVo> wordsList;
}
