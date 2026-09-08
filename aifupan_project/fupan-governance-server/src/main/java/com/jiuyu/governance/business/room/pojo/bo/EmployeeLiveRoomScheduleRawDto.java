package com.jiuyu.governance.business.room.pojo.bo;

import com.jiuyu.governance.business.room.handler.WorkTimeHandler;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 员工个人排班 原始信息
 *
 * @author HeHui
 * @date 2026-04-02 17:43
 */
@Getter
@Setter
public class EmployeeLiveRoomScheduleRawDto {

    /**
     * ID 排班ID
     */
    private Long id;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 班次归属天
     */
    private LocalDate workDay;

    /**
     * 轮班开始时间
     */
    private Integer startWork;

    /**
     * 轮班结束时间
     */
    private Integer endWork;

    /**
     * 班次时长
     */
    private Integer scheduleDuration;

    /**
     * 休息时长
     */
    private Integer restDuration;

    /**
     * 备注
     */
    private String remark;


    /**
     * 直播间名称
     */
    private String liveRoomName;


    /**
     * 排班员工记录ID
     */
    private Long scheduleEmployeeId;


    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 职位ID
     */
    private Long positionId;


    public LiveRoomSchedulePageResponse toPageInfo() {
        LiveRoomSchedulePageResponse response = new LiveRoomSchedulePageResponse();
        response.setId(id);
        response.setLiveRoomId(liveRoomId);
        response.setWorkDay(workDay);
        response.setStartWork(WorkTimeHandler.parseTime(startWork));
        response.setEndWork(WorkTimeHandler.parseTime(endWork));
        response.setScheduleDuration(scheduleDuration);
        response.setRestDuration(restDuration);
        response.setRemark(remark);
        response.setLiveRoomName(liveRoomName);
        response.setScheduleEmployeeId(scheduleEmployeeId);
        response.setEmployeeId(employeeId);
        response.setPositionId(positionId);
        return response;
    }


}
