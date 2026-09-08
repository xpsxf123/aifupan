package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员每日统计 BO
 *
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
public class StaffDailyStatsBO {

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 日期
     */
    private LocalDate statsDate;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 人员ID
     */
    private Long employeeId;

    /**
     * 人员名称
     */
    private String employeeName;

    /**
     * 该员工当天参与的场次数
     */
    private Integer scheduleCount;
}
