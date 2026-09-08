package com.jiuyu.governance.business.room.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeQueryRequest;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.business.room.pojo.bo.LiveRoomInfo;
import com.jiuyu.governance.business.room.pojo.bo.RoomSchedulesRawBo;
import com.jiuyu.governance.business.room.pojo.request.schedule.EmployeeSchedulePageQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.EmployeeScheduleQueryRequest;
import com.jiuyu.governance.business.room.pojo.response.schedule.EmployeeSchedulePageResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import com.jiuyu.governance.business.room.handler.LiveRoomScheduleRangeMergeHandler;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 企业端 - 个人排班API
 *
 * @author HeHui
 * @date 2026-03-27 11:51
 */
@RestController
@RequestMapping("/api/governance/employee-schedule")
@GovernanceUser
@RequiredArgsConstructor
@Slf4j
public class EmployeeScheduleController {

    private final LiveRoomScheduleService roomScheduleService;

    private final EmployeeService employeeService;

    private final DataPermissionsHandler permissionsHandler;


    private final LiveRoomService roomService;


    /**
     * 员工排班列表
     *
     * @param request    查询请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @PostMapping("/list")
    public ApiResponse<List<LiveRoomSchedulePageResponse>> planList(@RequestBody @Validated EmployeeScheduleQueryRequest request, AccessUser accessUser) {
        Long employeeId = request.getEmployeeId() == null ? accessUser.userId() : request.getEmployeeId();
        if (employeeId.equals(accessUser.userId())) {
            return ApiResponse.success(roomScheduleService.employeeScheduleList(request, accessUser.currentTenantId(), accessUser.userId()));
        }
        return permissionsHandler.run(accessUser, OauthConstant.EMPLOYEE, employeeId, () -> {
            return ApiResponse.success(roomScheduleService.employeeScheduleList(request, accessUser.currentTenantId(), employeeId));
        });
    }


    /**
     * 员工排班列表 - 拆分每天
     *
     * @param request    查询请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @PostMapping("/list-spit-day")
    public ApiResponse<List<LiveRoomSchedulePageResponse>> planListSplitDay(@RequestBody @Validated EmployeeScheduleQueryRequest request, AccessUser accessUser) {
        ApiResponse<List<LiveRoomSchedulePageResponse>> apiResponse = this.planList(request, accessUser);
        if (apiResponse.failed()) {
            return apiResponse;
        }
        return ApiResponse.success(LiveRoomScheduleRangeMergeHandler.splitByDay(apiResponse.getData()));
    }


    /**
     * 个人排班分页查询
     * <p>
     * 根据查询条件分页获取员工信息，并关联查询每个员工的排班数据和直播间信息。
     * 对于有排班的员工，返回当天和明天的排班及对应的直播间信息；
     * 对于无排班的员工，返回最近10天参与过的直播间信息。
     * </p>
     *
     * @param request    分页查询请求参数，包含员工姓名、职位ID、公司/部门/团队ID等筛选条件
     * @param accessUser 当前访问用户信息，用于获取租户ID和权限控制
     *
     * @return 分页响应结果，包含员工基本信息、排班数据和直播间信息
     */
    @PostMapping("/page")
    public ApiResponse<PageData<EmployeeSchedulePageResponse>> list(@RequestBody EmployeeSchedulePageQueryRequest request, AccessUser accessUser) {
        // 构建员工查询请求，设置筛选条件和分页参数
        EmployeeQueryRequest employeeQueryRequest = new EmployeeQueryRequest();
        employeeQueryRequest.setName(request.getEmployeeName());
        employeeQueryRequest.setId(request.getEmployeeId());
        employeeQueryRequest.setPositionIds(request.getPositionId() == null ? null : List.of(request.getPositionId()));
        employeeQueryRequest.setTenantId(accessUser.currentTenantId());
        employeeQueryRequest.setPage(request.getPage());
        employeeQueryRequest.setLimit(request.getLimit());
        if (request.getCompanyId() != null) {
            employeeQueryRequest.setCompanyIds(List.of(request.getCompanyId()));
        }
        if (request.getDeptId() != null) {
            employeeQueryRequest.setDeptIds(List.of(request.getDeptId()));
        }
        if (request.getTeamId() != null) {
            employeeQueryRequest.setTeamIds(List.of(request.getTeamId()));
        }
        // 执行员工分页查询，并将实体转换为响应对象
        PageData<EmployeeSchedulePageResponse> pageData = employeeService.pageQueryEmployee(employeeQueryRequest, accessUser).conversion(employee -> {
            EmployeeSchedulePageResponse employeeSchedule = new EmployeeSchedulePageResponse();
            employeeSchedule.setId(employee.getId());
            employeeSchedule.setName(employee.getName());
            employeeSchedule.setUserAvatar(employee.getUserAvatar());
            employeeSchedule.setPositionId(employee.getPositionId());
            employeeSchedule.setPositionName(employee.getPositionName());
            return employeeSchedule;
        });

        // 填充员工的排班数据和直播间信息
        if (EmptyUtil.isNotEmpty(pageData.getList())) {
            List<Long> employeeIds = pageData.getList().stream().map(EmployeeSchedulePageResponse::getId).toList();
            LocalDate start = LocalDate.now();
            LocalDate tomorrow = start.plusDays(1);
            // 今天是周几
            int nowWeek = start.getDayOfWeek().getValue();
            Map<Long, List<RoomSchedulesRawBo>> employeeScheduleMap = roomScheduleService.getEmployeeSchedules(employeeIds, new Range<>(start, tomorrow));
            // 将排班数据按日期分组后设置到对应的直播间响应对象中
            pageData.getList().forEach(employee -> {
                List<RoomSchedulesRawBo> roomSchedulesRawBos = employeeScheduleMap.get(employee.getId());
                if (EmptyUtil.isEmpty(roomSchedulesRawBos)) {
                    return;
                }
                employee.setThatDaySchedules(roomSchedulesRawBos.stream().filter(schedule -> schedule.getWorkDay().equals(start)).toList());
                employee.setTomorrowSchedules(roomSchedulesRawBos.stream().filter(schedule -> schedule.getWorkDay().equals(tomorrow)).toList());
                if (EmptyUtil.isNotEmpty(employee.getThatDaySchedules())) {
                    employee.setThisWeekWork(true);
                    employee.setThisMonthWork(true);
                }
                if (EmptyUtil.isNotEmpty(employee.getTomorrowSchedules()) && nowWeek == 7) {
                    employee.setNextWeekWork(true);
                    employee.setThisMonthWork(true);
                }
            });

            // 将员工分为有排班和无排班两组，分别处理直播间信息
            List<EmployeeSchedulePageResponse> haveSchedulesList = pageData.getList().stream().filter(employee -> EmptyUtil.isNotEmpty(employee.getThatDaySchedules()) || EmptyUtil.isNotEmpty(employee.getTomorrowSchedules())).toList();
            List<EmployeeSchedulePageResponse> noSchedulesList = pageData.getList().stream().filter(employee -> EmptyUtil.isEmpty(employee.getThatDaySchedules()) && EmptyUtil.isEmpty(employee.getTomorrowSchedules())).toList();

            // 处理有排班员工的直播间信息：提取两天排班涉及的所有直播间ID，批量查询直播间详情
            if (EmptyUtil.isNotEmpty(haveSchedulesList)) {
                Function<List<RoomSchedulesRawBo>, Stream<Long>> buildRoomId = roomSchedules -> {
                    if (EmptyUtil.isEmpty(roomSchedules)) {
                        return Stream.empty();
                    }
                    return roomSchedules.stream().map(RoomSchedulesRawBo::getRoomId);
                };
                Map<Long, List<Long>> employeeRoomMap = haveSchedulesList.stream().collect(Collectors.toMap(EmployeeSchedulePageResponse::getId, e -> {
                    return Stream.concat(buildRoomId.apply(e.getTomorrowSchedules()), buildRoomId.apply(e.getThatDaySchedules())).distinct().toList();
                }));
                Map<Long, LiveRoomInfo> roomMap = roomService.getRoomInfos(employeeRoomMap.values().stream().flatMap(List::stream).distinct().toList()).stream().collect(Collectors.toMap(LiveRoomInfo::getId, Function.identity()));
                haveSchedulesList.forEach(e -> {
                    e.setRoomInfos(employeeRoomMap.getOrDefault(e.getId(), List.of()).stream().map(roomMap::get).toList());
                });
            }

            // 处理无排班员工的直播间信息：查询最近10天参与过的直播间
            if (EmptyUtil.isNotEmpty(noSchedulesList)) {
                List<Long> noSchedulesEmployeeIds = noSchedulesList.stream().map(EmployeeSchedulePageResponse::getId).toList();
                Map<Long, List<LiveRoomInfo>> employeeRoomMap = roomService.getEmployeeJoinRooms(noSchedulesEmployeeIds, null, LocalDate.now().minusDays(10));
                noSchedulesList.forEach(e -> {
                    e.setRoomInfos(employeeRoomMap.get(e.getId()));
                });
            }
        }

        return pageData.toResult();
    }
}
