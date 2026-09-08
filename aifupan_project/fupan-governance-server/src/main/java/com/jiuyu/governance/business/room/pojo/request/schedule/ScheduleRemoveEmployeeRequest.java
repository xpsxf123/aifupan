package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 排班移除人员请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class ScheduleRemoveEmployeeRequest {

    /**
     * 排班ID
     */
    @NotNull(message = "缺少排班ID")
    private Long scheduleId;

    /**
     * 直播间ID
     */
    @NotNull(message = "缺少直播间ID")
    private Long liveRoomId;

    /**
     * 员工ID
     */
    @NotNull(message = "缺少员工ID")
    private Long employeeId;
}
