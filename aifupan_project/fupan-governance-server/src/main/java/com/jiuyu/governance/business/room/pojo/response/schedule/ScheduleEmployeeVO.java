package com.jiuyu.governance.business.room.pojo.response.schedule;

import lombok.Getter;
import lombok.Setter;

/**
 * 排班人员VO
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class ScheduleEmployeeVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;
}