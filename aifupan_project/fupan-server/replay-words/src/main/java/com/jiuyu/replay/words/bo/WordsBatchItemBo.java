package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "关键词信息")
public class WordsBatchItemBo {

    /**
     * 关键词
     */
    @Schema(description = "关键词")
    private String words;
    /**
     * 相似词列表
     */
    @Schema(description = "相似词列表")
    private List<String> similarWords;
}
