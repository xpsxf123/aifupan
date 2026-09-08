package com.jiuyu.governance.business.room.pojo.response.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播间排班分页响应
 *
 * @author HeHui
 * @date 2026-03-27 11:25
 */
@Getter
@Setter
public class LiveRoomSchedulePageResponse extends LiveRoomScheduleResponse {

    /**
     * 直播间名称
     */
    private String liveRoomName;


    /**
     * 排班员工记录ID
     */
    @JsonIgnore
    private Long scheduleEmployeeId;


    /**
     * 员工ID
     */
    @JsonIgnore
    private Long employeeId;

    /**
     * 职位ID
     */
    @JsonIgnore
    private Long positionId;
}
