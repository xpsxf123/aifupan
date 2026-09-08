package com.jiuyu.replay.generic.bo.power.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/30 10:35
 */
@Data
public class SalesCardStatisticsDataBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    private String startDate;

    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    private String endDate;

    @Schema(description = "销售id")
    @NotNull(message = "销售id不能为空")
    private Long salesId;
}
