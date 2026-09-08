package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/24 15:17
 */
@Data
@Schema(description = "看板数据的统计")
public class DashboardStatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "3日内到期客户")
    private Integer within3Days = 0;
    @Schema(description = "3日内到期客户率")
    private Double within3DaysRate = 0.0;

    @Schema(description = "7日内到期客户")
    private Integer within7Days = 0;
    @Schema(description = "7日内到期客户率")
    private Double within7DaysRate = 0.0;

    @Schema(description = "已过期3日内客户")
    private Integer expired3Days = 0;

    @Schema(description = "已过期7日内客户")
    private Integer expired7Days = 0;

    @Schema(description = "已过期超过7天")
    private Integer outside7Days = 0;

    @Schema(description = "总客户端人数")
    private Integer totalUserCount;
}
