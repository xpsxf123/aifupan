package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 员工业绩按班次维度 BO
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSchedulePerformanceBO extends BasePerformanceMetrics {

    /**
     * 班次业绩ID
     */
    private Long id;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 数据来源：1-系统，2-手动
     */
    private Integer source;
}
