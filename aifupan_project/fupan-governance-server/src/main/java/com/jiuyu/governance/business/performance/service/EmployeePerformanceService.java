package com.jiuyu.governance.business.performance.service;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.request.EmployeePerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeePeriodStatsRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeeTrendRequest;
import com.jiuyu.governance.business.performance.pojo.response.DailyPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeePerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeeSummaryResponse;
import com.jiuyu.governance.business.performance.pojo.response.PerformancePeriodStatsResponse;
import com.jiuyu.governance.business.performance.pojo.request.EmployeeDailyPerformanceRequest;
import com.jiuyu.governance.business.performance.pojo.request.EmployeeSchedulePerformanceRequest;
import com.jiuyu.governance.business.performance.pojo.response.EmployeeDailyPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.EmployeeSchedulePerformanceResponse;

import java.util.List;

/**
 * 人员业绩服务接口
 *
 * @author lj
 * @date 2026-03-30
 */
public interface EmployeePerformanceService {

    /**
     * 人员业绩分页查询
     *
     * @param request 查询请求
     * @return 分页数据
     */
    PageData<EmployeePerformanceResponse> pageQueryEmployeePerformance(EmployeePerformancePageRequest request, AccessUser accessUser);

    /**
     * 获取人员业绩时段统计
     *
     * @param request  请求参数（employeeId, liveRoomId）
     * @param tenantId 租户ID
     * @return 六时段业绩统计
     */
    PerformancePeriodStatsResponse getEmployeePeriodStats(EmployeePeriodStatsRequest request, Long tenantId);

    /**
     * 获取人员业绩汇总
     *
     * @param request  请求参数（employeeId, liveRoomId）
     * @param tenantId 租户ID
     * @return 业绩汇总
     */
    EmployeeSummaryResponse getEmployeeSummary(EmployeePeriodStatsRequest request, Long tenantId);

    /**
     * 获取人员业绩趋势数据
     *
     * @param request  请求参数（employeeId, liveRoomId, startDate, endDate）
     * @param tenantId 租户ID
     * @return 日维度业绩趋势列表
     */
    List<DailyPerformanceResponse> getEmployeeTrend(EmployeeTrendRequest request, Long tenantId);

    /**
     * 员工业绩按天维度分页查询
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     * @return 按天维度统计的分页数据
     */
    PageData<EmployeeDailyPerformanceResponse> pageEmployeeDailyPerformance(EmployeeDailyPerformanceRequest request, Long tenantId);

    /**
     * 员工业绩按班次维度分页查询
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     * @return 按班次维度统计的分页数据
     */
    PageData<EmployeeSchedulePerformanceResponse> pageEmployeeSchedulePerformance(EmployeeSchedulePerformanceRequest request, Long tenantId);
}
