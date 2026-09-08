package com.jiuyu.governance.business.performance.pojo.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 员工业绩按班次维度响应
 * <p>
 * 使用场景：员工业绩详情页-按班次维度分页列表
 * 每条记录是一个直播班次
 * </p>
 *
 * @author lj
 * @date 2026-03-30
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSchedulePerformanceResponse {

    /**
     * 班次业绩ID
     */
    private Long id;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 直播间名称
     */
    private String liveRoomName;

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

    /**
     * 业绩数据（含原始值对比）
     */
    private PerformanceData performanceData;
}
