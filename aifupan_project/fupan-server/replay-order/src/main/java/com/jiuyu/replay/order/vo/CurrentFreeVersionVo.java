package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/25 下午2:31
 */
@Data
public class CurrentFreeVersionVo {

    @Schema(description = "ai语音分析时长")
    private Long aiAnalysisTime = 0L;

    @Schema(description = "AI算力字数")
    private Long aiTokenNum = 0L;

    @Schema(description = "文案提取时长")
    private Long textExtractionNum = 0L;

}
