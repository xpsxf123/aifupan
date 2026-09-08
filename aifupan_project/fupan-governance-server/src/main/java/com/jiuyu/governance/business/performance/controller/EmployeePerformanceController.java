package com.jiuyu.governance.business.performance.controller;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.oauth.client.annotation.RequiredLogin;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.constants.DataSource;
import com.jiuyu.governance.business.performance.pojo.request.EmployeePerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeeDailyPerformanceRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeePeriodStatsRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeeSchedulePerformanceRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeeTrendRequest;
import com.jiuyu.governance.business.performance.pojo.response.DailyPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeePerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeeDailyPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeeSchedulePerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeeSummaryResponse;
import com.jiuyu.governance.business.performance.pojo.response.PerformancePeriodStatsResponse;
import com.jiuyu.governance.business.performance.pojo.response.export.EmployeeDailyPerformanceExport;
import com.jiuyu.governance.business.performance.pojo.response.export.EmployeeSchedulePerformanceExport;
import com.jiuyu.governance.business.performance.service.EmployeePerformanceService;
import com.jiuyu.governance.business.performance.utils.DataUtil;
import com.jiuyu.governance.plugins.excel.ExcelTemplate;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 人员业绩统计控制器
 * <p>
 * 提供人员维度的业绩统计功能，支持组织架构筛选和员工信息筛选
 * </p>
 *
 * @author lj
 * @date 2026-03-30
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/performance/employee")
@RequiredArgsConstructor
public class EmployeePerformanceController {

    private final EmployeePerformanceService employeePerformanceService;

    /**
     * 人员业绩分页查询
     * <p>
     * 按人员维度统计业绩数据，聚合该员工所有直播间的业绩
     * 返回员工信息和对应今天、昨天、本周、上周、本月、上月的业绩数据
     * </p>
     *
     * @param request    分页与条件请求参数
     * @param accessUser 当前登录用户上下文
     * @return 人员业绩分页数据
     */
    @PostMapping("/page")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ApiResponse<PageData<EmployeePerformanceResponse>> pageEmployeePerformance(
            @Valid @RequestBody EmployeePerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(employeePerformanceService.pageQueryEmployeePerformance(request, accessUser));
    }

    /**
     * 人员业绩时段统计
     * <p>
     * 查询指定员工的六时段业绩统计（聚合该员工所有直播间）
     * </p>
     *
     * @param request    请求参数（employeeId, liveRoomId）
     * @param accessUser 当前登录用户上下文
     * @return 六时段业绩统计 包含：今天、昨天、本周、上周、本月、上月
     */
    @PostMapping("/period-stats")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ApiResponse<PerformancePeriodStatsResponse> getPeriodStats(
            @Valid @RequestBody EmployeePeriodStatsRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeePerformanceService.getEmployeePeriodStats(request, accessUser.currentTenantId()));
    }

    /**
     * 人员业绩汇总统计
     * <p>
     * 统计指定员工所有直播间的总场观和销售额
     * </p>
     *
     * @param request    请求参数（employeeId, liveRoomId）
     * @param accessUser 当前登录用户上下文
     * @return 业绩汇总
     */
    @PostMapping("/summary")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ApiResponse<EmployeeSummaryResponse> getSummary(
            @Valid @RequestBody EmployeePeriodStatsRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeePerformanceService.getEmployeeSummary(request, accessUser.currentTenantId()));
    }

    /**
     * 人员业绩趋势数据
     * <p>
     * 查询指定员工的业绩趋势数据（聚合所有直播间），按天聚合
     * </p>
     *
     * @param request    请求参数（employeeId, liveRoomId, startDate, endDate）
     * @param accessUser 当前登录用户上下文
     * @return 日维度业绩趋势列表
     */
    @PostMapping("/trend")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ApiResponse<List<DailyPerformanceResponse>> getTrend(
            @Valid @RequestBody EmployeeTrendRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeePerformanceService.getEmployeeTrend(request, accessUser.currentTenantId()));
    }

    /**
     * 员工业绩按天维度分页查询
     * <p>
     * 使用场景：员工业绩详情页-按天维度分页列表
     * 以天为维度统计员工业绩，每天一条记录，包含当天参与的直播间列表
     * </p>
     *
     * @param request    查询请求
     * @param accessUser 当前登录用户上下文
     * @return 按天维度统计的分页数据
     */
    @PostMapping("/daily/page")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ApiResponse<PageData<EmployeeDailyPerformanceResponse>> pageDailyPerformance(
            @Valid @RequestBody EmployeeDailyPerformanceRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeePerformanceService.pageEmployeeDailyPerformance(request, accessUser.currentTenantId()));
    }

    /**
     * 员工业绩按班次维度分页查询
     * <p>
     * 使用场景：员工业绩详情页-按班次维度分页列表
     * 以班次（schedule_performance.id）为维度查询员工业绩，每条记录是一个直播班次
     * </p>
     *
     * @param request    查询请求
     * @param accessUser 当前登录用户上下文
     * @return 按班次维度统计的分页数据
     */
    @PostMapping("/schedule/page")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ApiResponse<PageData<EmployeeSchedulePerformanceResponse>> pageSchedulePerformance(
            @Valid @RequestBody EmployeeSchedulePerformanceRequest request, AccessUser accessUser) {
        return ApiResponse.success(employeePerformanceService.pageEmployeeSchedulePerformance(request, accessUser.currentTenantId()));
    }

    /**
     * 导出员工业绩数据（按天维度）（Excel）
     * <p>
     * 使用场景：员工业绩详情页-按天维度列表导出全部数据
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * 直播间列表按名称换行拼接为单列
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 employeeId（必填）、startDate、endDate、liveRoomName（可选）
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @GetMapping("/daily/export")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ResponseEntity<byte[]> exportDailyPerformance(@Valid EmployeeDailyPerformanceRequest request, AccessUser accessUser) {
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        PageData<EmployeeDailyPerformanceResponse> result = employeePerformanceService.pageEmployeeDailyPerformance(request, accessUser.currentTenantId());
        List<EmployeeDailyPerformanceExport> rows = ObjUtil
                .defaultIfNull(result.getList(), Collections.<EmployeeDailyPerformanceResponse>emptyList())
                .stream()
                .map(r -> EmployeeDailyPerformanceExport.builder()
                        .statsDate(r.getStatsDate() != null ? DateUtil.formatDate(new DateTime(r.getStatsDate())) : "")
                        .schedule(DataUtil.sessionString(r.getScheduleCount(), r.getLiveDurationMinutes()))
                        .liveRooms(r.getLiveRoomList() == null ? "" : r.getLiveRoomList().stream().map(EmployeeDailyPerformanceResponse.LiveRoomInfo::getLiveRoomName).collect(Collectors.joining("、")))
                        .viewCount(r.getViewCount())
                        .salesRevenue(r.getSalesRevenue())
                        .refund(r.getRefund())
                        .netSales(r.getNetSales())
                        .investment(r.getInvestment())
                        .roi(r.getRoi())
                        .build())
                .collect(Collectors.toList());
        return ExcelTemplate.downloadList(rows, "员工业绩数据（按天）.xlsx", "按天统计",
                Collections.emptySet(), EmployeeDailyPerformanceExport.class);
    }

    /**
     * 导出员工业绩数据（按班次维度）（Excel）
     * <p>
     * 使用场景：员工业绩详情页-按班次维度列表导出全部数据
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 employeeId（必填）、startDate、endDate、liveRoomName、sortField、sortOrder（可选）
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @GetMapping("/schedule/export")
    @RequiredLogin
    @Permissions({"my:performance:list", "sys:employee:performance:list"})
    public ResponseEntity<byte[]> exportSchedulePerformance(@Valid EmployeeSchedulePerformanceRequest request, AccessUser accessUser) {
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        PageData<EmployeeSchedulePerformanceResponse> result = employeePerformanceService.pageEmployeeSchedulePerformance(request, accessUser.currentTenantId());
        List<EmployeeSchedulePerformanceExport> rows = ObjUtil
                .defaultIfNull(result.getList(), Collections.<EmployeeSchedulePerformanceResponse>emptyList())
                .stream()
                .map(r -> EmployeeSchedulePerformanceExport.builder()
                        .date(r.getStartTime() != null ? DateUtil.format(r.getStartTime(), "yyyy-MM-dd") : "")
                        .timeSlot(DataUtil.liveTimeSlot(r.getStartTime(), r.getEndTime()))
                        .liveRoomName(r.getLiveRoomName())
                        .viewCount(r.getPerformanceData().getViewCount().getValue())
                        .salesRevenue(r.getPerformanceData().getSalesRevenue().getValue())
                        .refund(r.getPerformanceData().getRefund().getValue())
                        .netSales(r.getPerformanceData().getNetSales().getValue())
                        .investment(r.getPerformanceData().getInvestment().getValue())
                        .roi(r.getPerformanceData().getRoi().getValue())
                        .source(DataSource.descByCode(r.getSource()))
                        .build())
                .collect(Collectors.toList());
        return ExcelTemplate.downloadList(rows, "员工业绩数据（按班次）.xlsx", "按班次统计",
                Collections.emptySet(), EmployeeSchedulePerformanceExport.class);
    }
}
