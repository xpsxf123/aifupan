package com.jiuyu.governance.business.room.pojo.response.schedule;

import com.jiuyu.governance.business.room.pojo.bo.LiveRoomInfo;
import com.jiuyu.governance.business.room.pojo.bo.RoomSchedulesRawBo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 人员排班列表
 *
 * @author HeHui
 * @date 2026-04-10 20:03
 */
@Getter
@Setter
public class EmployeeSchedulePageResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 员工名称
     */
    private String name;

    /**
     * 头像
     */
    private String userAvatar;


    /**
     * 所属岗位ID
     */
    private Long positionId;

    /**
     * 岗位名称
     */
    private String positionName;


    /**
     * 员工直播间信息
     */
    private List<LiveRoomInfo> roomInfos;


    /**
     * 该人员当天的排班信息列表
     */
    private List<RoomSchedulesRawBo> thatDaySchedules;

    /**
     * 该人员明天的排班信息列表
     */
    private List<RoomSchedulesRawBo> tomorrowSchedules;



    /**
     * 该人员本周是否工作
     */
    private Boolean thisWeekWork =  false;

    /**
     * 该人员下周是否工作
     */
    private Boolean nextWeekWork =  false;

    /**
     * 该人员本月是否工作
     */
    private Boolean thisMonthWork =  false;
}
