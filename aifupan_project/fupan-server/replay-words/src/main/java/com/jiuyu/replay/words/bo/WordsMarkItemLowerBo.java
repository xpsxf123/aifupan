package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "词语列表项")
public class WordsMarkItemLowerBo {

    /**
     * 词语
     */
    @Schema(description = "词语")
    private String word;
    /**
     * 开始时间，毫秒
     */
    @Schema(description = "开始时间，毫秒")
    private Long startTime;
    /**
     * 结束时间，毫秒
     */
    @Schema(description = "结束时间，毫秒")
    private Long endTime;


}
