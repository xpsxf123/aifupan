package com.jiuyu.governance.business.room.utils;

import cn.hutool.core.util.RandomUtil;
import com.jiuyu.governance.business.room.pojo.request.*;
import com.jiuyu.governance.business.room.pojo.request.schedule.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Room 模块测试数据构建工具类
 *
 * <p>提供以下功能：</p>
 * <ul>
 *     <li>构建直播间相关请求对象</li>
 *     <li>构建排班相关请求对象</li>
 *     <li>生成随机测试数据</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
public class RoomTestDataBuilder {

    /**
     * 构建直播间新增请求
     *
     * @param companyId 公司 ID
     * @param deptId    部门 ID
     * @param teamId    小组 ID
     * @return 直播间新增请求对象
     */
    public static LiveRoomAddRequest buildLiveRoomAddRequest(Long companyId, Long deptId, Long teamId) {
        LiveRoomAddRequest request = new LiveRoomAddRequest();
        request.setPlatform(0); // 抖音
        request.setAnchorNumber("test_anchor_" + RandomUtil.randomString(6));
        request.setCompanyId(companyId);
        request.setDeptId(deptId);
        request.setTeamId(teamId);
        request.setDebutDate(LocalDate.now());
        List<Long> managerUserIds = new ArrayList<>();
        managerUserIds.add(1L);
        request.setManagerUserIds(managerUserIds);
        return request;
    }

    /**
     * 构建直播间修改请求
     *
     * @param roomId    直播间 ID
     * @param companyId 公司 ID
     * @param deptId    部门 ID
     * @param teamId    小组 ID
     * @return 直播间修改请求对象
     */
    public static LiveRoomUpdateRequest buildLiveRoomUpdateRequest(Long roomId, Long companyId, Long deptId, Long teamId) {
        LiveRoomUpdateRequest request = new LiveRoomUpdateRequest();
        request.setId(roomId);
        request.setCompanyId(companyId);
        request.setDeptId(deptId);
        request.setTeamId(teamId);
        List<Long> managerUserIds = new ArrayList<>();
        managerUserIds.add(1L);
        request.setManagerUserIds(managerUserIds);
        return request;
    }

    /**
     * 构建直播间查询请求
     *
     * @param companyId 公司 ID
     * @param deptId    部门 ID
     * @param teamId    小组 ID
     * @param page      页码
     * @param limit     每页数量
     * @return 直播间查询请求对象
     */
    public static LiveRoomQueryRequest buildLiveRoomQueryRequest(Long companyId, Long deptId, Long teamId, int page, int limit) {
        LiveRoomQueryRequest request = new LiveRoomQueryRequest();
        request.setPage(page);
        request.setLimit(limit);
        request.setCompanyId(companyId);
        request.setDeptId(deptId);
        request.setTeamId(teamId);
        return request;
    }

    /**
     * 构建排班配置设置请求
     *
     * @param liveRoomId 直播间 ID
     * @return 排班配置设置请求对象
     */
    public static LiveRoomScheduleAttributeSetRequest buildScheduleAttributeSetRequest(Long liveRoomId) {
        LiveRoomScheduleAttributeSetRequest request = new LiveRoomScheduleAttributeSetRequest();
        request.setId(liveRoomId);
        request.setStartPlan(LocalTime.of(9, 0));
        request.setEndPlan(LocalTime.of(18, 0));
        request.setShiftOptions(List.of(1, 2, 3));
        request.setRestOptions(List.of(30, 60));
        request.setPositionOptions(List.of(1L, 2L));
        return request;
    }

    /**
     * 构建排班新增请求
     *
     * @param liveRoomId 直播间 ID
     * @param workDay    工作日期
     * @return 排班新增请求对象
     */
    public static LiveRoomScheduleAddRequest buildScheduleAddRequest(Long liveRoomId, LocalDate workDay) {
        LiveRoomScheduleAddRequest request = new LiveRoomScheduleAddRequest();
        request.setLiveRoomId(liveRoomId);
        request.setWorkDay(workDay);

        // 设置班次信息
        List<ScheduleSessionRequest> sessions = new ArrayList<>();
        ScheduleSessionRequest session = buildScheduleSessionRequest();
        sessions.add(session);
        request.setSessions(sessions);

        return request;
    }

    /**
     * 构建班次会话请求
     *
     * @return 班次会话请求对象
     */
    public static ScheduleSessionRequest buildScheduleSessionRequest() {
        ScheduleSessionRequest session = new ScheduleSessionRequest();
        session.setStartWork(LocalTime.of(9, 0));
        session.setEndWork(LocalTime.of(17, 0));
        session.setScheduleDuration(480);
        session.setRestDuration(60);
        //session.setRemark("测试班次_" + RandomUtil.randomString(4));

        // 设置排班人员
        List<ScheduleEmployeeRequest> employees = new ArrayList<>();
        ScheduleEmployeeRequest employee = new ScheduleEmployeeRequest();
        employee.setEmployeeId(1L);
        employee.setPositionId(1L);
        employees.add(employee);
        session.setEmployees(employees);

        return session;
    }

    /**
     * 构建排班查询请求
     *
     * @param liveRoomId 直播间 ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 排班查询请求对象
     */
    public static LiveRoomScheduleQueryRequest buildScheduleQueryRequest(Long liveRoomId, LocalDate startDate, LocalDate endDate) {
        LiveRoomScheduleQueryRequest request = new LiveRoomScheduleQueryRequest();
        request.setLiveRoomId(liveRoomId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }

    /**
     * 构建排班分页查询请求
     *
     * @param liveRoomId 直播间 ID
     * @param page       页码
     * @param limit      每页数量
     * @return 排班分页查询请求对象
     */
    public static LiveRoomSchedulePageRequest buildSchedulePageRequest(Long liveRoomId, int page, int limit) {
        LiveRoomSchedulePageRequest request = new LiveRoomSchedulePageRequest();
        request.setLiveRoomId(liveRoomId);
        request.setPage(page);
        request.setLimit(limit);
        return request;
    }

    /**
     * 构建添加排班人员请求
     *
     * @param scheduleId 排班 ID
     * @param employeeId 员工 ID
     * @param positionId 岗位 ID
     * @return 添加排班人员请求对象
     */
    public static ScheduleAddEmployeeRequest buildAddEmployeeRequest(Long scheduleId, Long employeeId, Long positionId) {
        ScheduleAddEmployeeRequest request = new ScheduleAddEmployeeRequest();
        request.setScheduleId(scheduleId);
        request.setEmployeeId(employeeId);
        request.setPositionId(positionId);
        return request;
    }

    /**
     * 构建移除排班人员请求
     *
     * @param scheduleId 排班 ID
     * @param employeeId 员工 ID
     * @return 移除排班人员请求对象
     */
    public static ScheduleRemoveEmployeeRequest buildRemoveEmployeeRequest(Long scheduleId, Long employeeId) {
        ScheduleRemoveEmployeeRequest request = new ScheduleRemoveEmployeeRequest();
        request.setScheduleId(scheduleId);
        request.setEmployeeId(employeeId);
        return request;
    }

    /**
     * 构建修改排班请求
     *
     * @param scheduleId      排班 ID
     * @param startWork       开始时间
     * @param endWork         结束时间
     * @param scheduleDuration 班次时长
     * @param restDuration    休息时长
     * @return 修改排班请求对象
     */
    public static ScheduleUpdateRequest buildUpdateScheduleRequest(Long scheduleId, LocalTime startWork, LocalTime endWork,
                                                                   int scheduleDuration, int restDuration) {
        ScheduleUpdateRequest request = new ScheduleUpdateRequest();
        request.setId(scheduleId);
        request.setStartWork(startWork);
        request.setEndWork(endWork);
        request.setScheduleDuration(scheduleDuration);
        request.setRestDuration(restDuration);
        return request;
    }
}
