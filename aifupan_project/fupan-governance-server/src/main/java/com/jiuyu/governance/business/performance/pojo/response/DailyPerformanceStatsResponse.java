package com.jiuyu.governance.business.performance.pojo.response;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * 按天维度排班业绩统计响应
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@SuperBuilder
public class DailyPerformanceStatsResponse extends BasePerformanceMetrics {

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

    /**
     * 人员统计列表
     */
    private List<StaffDailyStats> staffList;
}
