package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RecordNeedsWordVo {

    /**
     * 记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语
     */
    @Schema(description = "记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语")
    private Integer recordNeedsNum;
    /**
     * 匹配中的限定词
     */
    @Schema(description = "匹配中的限定词")
    private String restrictWord;
    /**
     * 限定词范围
     */
    @Schema(description = "限定词范围")
    private Integer restrictRange;
}
