package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 按天维度排班业绩统计 BO
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPerformanceStatsBO extends BasePerformanceMetrics {

    /**
     * 日期
     */
    private LocalDate statsDate;

    /**
     * 场次数量（当天排班场次）
     */
    private Integer scheduleCount;

    /**
     * 直播时长（分钟）
     */
    private Long liveDurationMinutes;
}
