package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 班次排班人员请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class ScheduleEmployeeRequest {

    /**
     * 员工ID
     */
    @NotNull(message = "请选择员工")
    private Long employeeId;

    /**
     * 岗位ID
     */
    @NotNull(message = "请选择岗位")
    private Long positionId;
}