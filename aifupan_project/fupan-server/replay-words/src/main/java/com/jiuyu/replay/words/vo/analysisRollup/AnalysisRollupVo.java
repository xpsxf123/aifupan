package com.jiuyu.replay.words.vo.analysisRollup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/2 19:51
 */
@Data
public class AnalysisRollupVo {

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "最小时间")
    private Date minTime;

    @Schema(description = "最大时间")
    private Date maxTime;

    @Schema(description = "数量")
    private Long count;
}
