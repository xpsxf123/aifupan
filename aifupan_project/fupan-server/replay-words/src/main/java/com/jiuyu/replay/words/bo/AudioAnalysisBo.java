package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author tisheng
 * @date 2024/9/6
 * @apinNote
 */
@Data
@Schema(description = "")
public class AudioAnalysisBo  {
    /**
     * 视频ID
     */
    @Schema(description = "视频ID")
    private String videoId;

    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
}
