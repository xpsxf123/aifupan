package com.jiuyu.governance.business.room.pojo.request.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 直播间排班查询请求
 *
 * @author HeHui
 * @date 2026-03-26
 */
@Getter
@Setter
public class LiveRoomScheduleQueryRequest {

    /**
     * 直播间ID
     */
    @NotNull(message = "缺少直播间ID")
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


    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 职位ID
     */
    private Long positionId;

    /**
     * 排班ID
     */
    @JsonIgnore
    private Long scheduleId;
}
