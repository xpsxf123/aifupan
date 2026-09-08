package com.jiuyu.replay.words.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "词语列表项")
public class WordsMarkItemBo {

    /**
     * 词语
     */
    @JsonProperty("Word")
    @Schema(description = "词语")
    private String word;
    /**
     * 开始时间，毫秒
     */
    @JsonProperty("StartTime")
    @Schema(description = "开始时间，毫秒")
    private Long startTime;
    /**
     * 结束时间，毫秒
     */
    @JsonProperty("EndTime")
    @Schema(description = "结束时间，毫秒")
    private Long endTime;


}
