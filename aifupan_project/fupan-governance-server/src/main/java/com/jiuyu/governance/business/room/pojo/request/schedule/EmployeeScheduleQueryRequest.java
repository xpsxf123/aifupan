package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 个人排班查询请求参数
 *
 * @author HeHui
 * @date 2026-03-27 11:54
 */
@Getter
@Setter
public class EmployeeScheduleQueryRequest {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 开始日期
     */
    @NotNull(message = "请选择开始日期")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "请选择结束日期")
    private LocalDate endDate;
}
