package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：付费到期客户看板统计
 * @date ：2026/7/2 10:00
 */
@Data
@Schema(description = "付费到期客户看板统计")
public class PaidDashboardStatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "15天内到期客户")
    private Integer within15Days = 0;

    @Schema(description = "30天内到期客户")
    private Integer within30Days = 0;

    @Schema(description = "60天内到期客户")
    private Integer within60Days = 0;

    @Schema(description = "90天内到期客户")
    private Integer within90Days = 0;

    @Schema(description = "已过期7天客户")
    private Integer expired7Days = 0;

    @Schema(description = "已过期15天客户")
    private Integer expired15Days = 0;

    @Schema(description = "已过期30天客户")
    private Integer expired30Days = 0;

    @Schema(description = "已过期60天客户")
    private Integer expired60Days = 0;

    @Schema(description = "总客户端人数")
    private Integer totalUserCount;
}
