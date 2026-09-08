package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * 员工业绩按天维度统计 BO
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDailyPerformanceBO extends BasePerformanceMetrics {

    /**
     * 统计日期
     */
    private LocalDate statsDate;

    /**
     * 场次数量（当天总排班场次）
     */
    private Integer scheduleCount;

    /**
     * 直播时长（分钟）
     */
    private Long liveDurationMinutes;

    /**
     * 直播间ID列表（逗号分隔）
     */
    private String liveRoomIds;
}
