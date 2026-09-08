package com.jiuyu.governance.business.room.pojo.response.schedule;

import com.jiuyu.governance.business.rbac.pojo.response.EmployeeInfoResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *  当个人员的排班信息
 * @author HeHui
 * @date 2026-04-10 20:10
 */
@Getter
@Setter
public class EmployeeScheduleInfoResponse extends EmployeeInfoResponse {


    /**
     * 员工排班计划
     */
    private List<LiveRoomSchedulePageResponse> schedulePlans;
}
