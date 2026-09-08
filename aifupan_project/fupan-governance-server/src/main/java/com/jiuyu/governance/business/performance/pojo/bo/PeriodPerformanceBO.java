package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 时段业绩聚合统计BO
 * 用于接收SQL层面的时段聚合查询结果（一次查询返回一个时段的所有统计数据）
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PeriodPerformanceBO extends BasePerformanceMetrics {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 场次数量（COUNT DISTINCT session_id）
     */
    private Integer sessionCount;

    /**
     * 直播时长（分钟）
     */
    private Integer duration;
}
