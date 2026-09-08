package com.jiuyu.governance.business.performance.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.governance.business.performance.pojo.bo.DailyPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.EmployeeDailyPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.EmployeePerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.EmployeeSchedulePerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.PeriodPerformanceBO;
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
import com.jiuyu.governance.business.performance.pojo.response.PerformanceData;
import com.jiuyu.governance.business.performance.pojo.response.PerformancePeriodStatsResponse;
import com.jiuyu.governance.business.performance.pojo.entity.SessionOriginalValue;
import com.jiuyu.governance.business.performance.mapper.SessionOriginalValueMapper;
import com.jiuyu.governance.business.performance.service.EmployeePerformanceService;
import com.jiuyu.governance.business.performance.mapper.SchedulePerformanceMapper;
import com.jiuyu.governance.business.performance.utils.DataUtil;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.business.performance.utils.MetricsUtil;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人员业绩服务实现类
 *
 * @author lj
 * @date 2026-03-30
 */
@Service
@RequiredArgsConstructor
public class EmployeePerformanceServiceImpl implements EmployeePerformanceService {

    private final SchedulePerformanceMapper schedulePerformanceMapper;
    private final SessionOriginalValueMapper sessionOriginalValueMapper;
    private final SchedulePerformanceServiceImpl schedulePerformanceService;
    private final LiveRoomMapper liveRoomMapper;
    private final SubCompanyService subCompanyService;
    private final DeptService deptService;
    private final TeamService teamService;
    private final PositionService positionService;
    private final LiveRoomService liveRoomService;

    @Override
    public PageData<EmployeePerformanceResponse> pageQueryEmployeePerformance(EmployeePerformancePageRequest request, AccessUser accessUser) {
        if (request.empty()) {
            return PageData.empty();
        }

        // 1. 分页查询员工列表（按员工维度）
        Page<EmployeePerformanceBO> page = new Page<>(request.getPage(), request.getLimit());
        IPage<EmployeePerformanceBO> result = schedulePerformanceMapper.pageQueryEmployeePerformance(
                page, request, request.getTenantId());

        if (result.getRecords().isEmpty()) {
            return PageData.empty();
        }

        // 2. 收集所有直播间ID
        Set<Long> allLiveRoomIds = result.getRecords().stream()
                .filter(bo -> bo.getLiveRoomIds() != null && !bo.getLiveRoomIds().isEmpty())
                .flatMap(bo -> Arrays.stream(bo.getLiveRoomIds().split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        // 3. 批量获取直播间名称
        Map<Long, String> liveRoomNameMap = allLiveRoomIds.isEmpty()
                ? Collections.emptyMap()
                : liveRoomService.getLiveRoomNameMap(new ArrayList<>(allLiveRoomIds));

        // 4. 转换响应对象并填充直播间列表
        List<EmployeePerformanceResponse> responseList = result.getRecords().stream()
                .map(bo -> {
                    EmployeePerformanceResponse response = BeanUtil.copyProperties(bo, EmployeePerformanceResponse.class);

                    // 将逗号分隔的ID转换为直播间列表
                    if (bo.getLiveRoomIds() != null && !bo.getLiveRoomIds().isEmpty()) {
                        List<EmployeePerformanceResponse.LiveRoomInfo> liveRoomList = Arrays.stream(bo.getLiveRoomIds().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(Long::parseLong)
                                .map(id -> EmployeePerformanceResponse.LiveRoomInfo.builder()
                                        .id(id)
                                        .name(liveRoomNameMap.getOrDefault(id, ""))
                                        .build())
                                .collect(Collectors.toList());
                        response.setLiveRoomList(liveRoomList);
                    } else {
                        response.setLiveRoomList(Collections.emptyList());
                    }

                    return response;
                })
                .toList();

        // 5. 批量填充名称
        Complete.start(responseList)
                .build(EmployeePerformanceResponse::getCompanyId, EmployeePerformanceResponse::setCompanyName, subCompanyService::getNameMap)
                .then()
                .build(EmployeePerformanceResponse::getDeptId, EmployeePerformanceResponse::setDeptName, deptService::getDeptNameMap)
                .then()
                .build(EmployeePerformanceResponse::getTeamId, EmployeePerformanceResponse::setTeamName, teamService::getTeamNameMap)
                .then()
                .build(EmployeePerformanceResponse::getPositionId, EmployeePerformanceResponse::setPositionName, positionService::getPositionNameMap)
                .then()
                .over();

        // 6. 提取员工ID列表
        List<Long> employeeIds = responseList.stream()
                .map(EmployeePerformanceResponse::getEmployeeId)
                .distinct()
                .toList();

        // 7. 按员工ID查询六时段业绩数据
        DataUtil.PeriodDateRange range = DataUtil.calculatePeriodDateRange();

        Map<Long, PeriodPerformanceBO> todayMap = queryAndGroupByEmployee(request.getTenantId(), employeeIds, range.today(), range.today());
        Map<Long, PeriodPerformanceBO> yesterdayMap = queryAndGroupByEmployee(request.getTenantId(), employeeIds, range.yesterday(), range.yesterday());
        Map<Long, PeriodPerformanceBO> thisWeekMap = queryAndGroupByEmployee(request.getTenantId(), employeeIds, range.thisWeekStart(), range.today());
        Map<Long, PeriodPerformanceBO> lastWeekMap = queryAndGroupByEmployee(request.getTenantId(), employeeIds, range.lastWeekStart(), range.lastWeekEnd());
        Map<Long, PeriodPerformanceBO> thisMonthMap = queryAndGroupByEmployee(request.getTenantId(), employeeIds, range.thisMonthStart(), range.today());
        Map<Long, PeriodPerformanceBO> lastMonthMap = queryAndGroupByEmployee(request.getTenantId(), employeeIds, range.lastMonthStart(), range.lastMonthEnd());

        // 8. 组装业绩统计
        for (EmployeePerformanceResponse response : responseList) {
            Long employeeId = response.getEmployeeId();
            response.setPerformanceStatistics(DataUtil.buildPeriodStatsResponse(
                    todayMap.get(employeeId),
                    yesterdayMap.get(employeeId),
                    thisWeekMap.get(employeeId),
                    lastWeekMap.get(employeeId),
                    thisMonthMap.get(employeeId),
                    lastMonthMap.get(employeeId)));
        }

        return new PageData<>(responseList, result.getTotal(), request.getLimit(), request.getPage(), result.getPages());
    }

    /**
     * 按员工ID查询业绩数据并分组
     */
    private Map<Long, PeriodPerformanceBO> queryAndGroupByEmployee(
            Long tenantId, List<Long> employeeIds, LocalDate startDate, LocalDate endDate) {
        List<PeriodPerformanceBO> list = schedulePerformanceMapper.queryPeriodStatsByEmployee(
                tenantId, employeeIds, startDate, endDate);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.toMap(
                        PeriodPerformanceBO::getEmployeeId,
                        bo -> bo,
                        (a, b) -> a
                ));
    }

    /**
     * 按员工ID和直播间ID批量查询业绩数据并分组
     */
    private Map<String, PeriodPerformanceBO> queryAndGroupByEmployeeAndRoom(
            Long tenantId, List<Long> employeeIds, List<Long> liveRoomIds,
            LocalDate startDate, LocalDate endDate) {
        List<PeriodPerformanceBO> list = schedulePerformanceMapper.queryPeriodStatsByEmployeeAndRoom(
                tenantId, employeeIds, liveRoomIds, startDate, endDate);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.toMap(
                        bo -> bo.getEmployeeId() + "_" + bo.getLiveRoomId(),
                        bo -> bo,
                        (a, b) -> a
                ));
    }

    @Override
    public PerformancePeriodStatsResponse getEmployeePeriodStats(EmployeePeriodStatsRequest request, Long tenantId) {
        Long employeeId = request.getEmployeeId();

        // 计算六时段日期范围
        DataUtil.PeriodDateRange range = DataUtil.calculatePeriodDateRange();

        // 按员工ID查询六时段业绩数据（聚合所有直播间）
        List<Long> employeeIds = List.of(employeeId);

        Map<Long, PeriodPerformanceBO> todayMap = queryAndGroupByEmployee(tenantId, employeeIds, range.today(), range.today());
        Map<Long, PeriodPerformanceBO> yesterdayMap = queryAndGroupByEmployee(tenantId, employeeIds, range.yesterday(), range.yesterday());
        Map<Long, PeriodPerformanceBO> thisWeekMap = queryAndGroupByEmployee(tenantId, employeeIds, range.thisWeekStart(), range.today());
        Map<Long, PeriodPerformanceBO> lastWeekMap = queryAndGroupByEmployee(tenantId, employeeIds, range.lastWeekStart(), range.lastWeekEnd());
        Map<Long, PeriodPerformanceBO> thisMonthMap = queryAndGroupByEmployee(tenantId, employeeIds, range.thisMonthStart(), range.today());
        Map<Long, PeriodPerformanceBO> lastMonthMap = queryAndGroupByEmployee(tenantId, employeeIds, range.lastMonthStart(), range.lastMonthEnd());

        return DataUtil.buildPeriodStatsResponse(
                todayMap.get(employeeId),
                yesterdayMap.get(employeeId),
                thisWeekMap.get(employeeId),
                lastWeekMap.get(employeeId),
                thisMonthMap.get(employeeId),
                lastMonthMap.get(employeeId));
    }

    @Override
    public EmployeeSummaryResponse getEmployeeSummary(EmployeePeriodStatsRequest request, Long tenantId) {
        // 按员工聚合所有直播间的汇总数据
        PeriodPerformanceBO bo = schedulePerformanceMapper.queryEmployeeSummaryByEmployee(
                tenantId, request.getEmployeeId());

        if (bo == null) {
            return EmployeeSummaryResponse.builder()
                    .totalViewCount(0)
                    .totalSalesRevenue(BigDecimal.ZERO)
                    .build();
        }
        double wholeConversionRate = 0.0;

        if (ObjUtil.isNotNull(bo.getPayComboCnt()) && ObjUtil.isNotNull(bo.getViewCount()) && bo.getViewCount() != 0 && bo.getPayComboCnt() != 0){
            BigDecimal divide = MetricsUtil.divide(BigDecimal.valueOf(bo.getPayComboCnt()), BigDecimal.valueOf(bo.getViewCount()));
            wholeConversionRate = divide != null ? divide.doubleValue() : wholeConversionRate;
        }

        return EmployeeSummaryResponse.builder()
                .totalViewCount(bo.getViewCount() != null ? bo.getViewCount() : 0)
                .totalSalesRevenue(bo.getSalesRevenue() != null ? bo.getSalesRevenue() : BigDecimal.ZERO)
                .wholeConversionRate(wholeConversionRate)
                .build();
    }

    @Override
    public List<DailyPerformanceResponse> getEmployeeTrend(EmployeeTrendRequest request, Long tenantId) {
        // 按员工维度聚合所有直播间的业绩趋势
        List<DailyPerformanceBO> dbResults = schedulePerformanceMapper.queryEmployeeTrendByEmployee(
                tenantId, request.getEmployeeId(),
                request.getStartDate(), request.getEndDate());

        // 转为Map便于快速查找
        Map<LocalDate, DailyPerformanceBO> dataMap = dbResults.stream()
                .collect(Collectors.toMap(DailyPerformanceBO::getStatsDate, bo -> bo));

        // 填补时间范围内所有天，无数据的天显示0
        List<DailyPerformanceResponse> responses = new ArrayList<>();
        LocalDate current = request.getStartDate();
        while (!current.isAfter(request.getEndDate())) {
            DailyPerformanceBO bo = dataMap.get(current);
            responses.add(DailyPerformanceResponse.builder()
                    .date(current)
                    .viewCount(bo != null ? bo.getViewCount() : 0)
                    .salesRevenue(bo != null ? bo.getSalesRevenue() : BigDecimal.ZERO)
                    .refund(bo != null ? bo.getRefund() : BigDecimal.ZERO)
                    .netSales(bo != null ? bo.getNetSales() : BigDecimal.ZERO)
                    .investment(bo != null ? bo.getInvestment() : BigDecimal.ZERO)
                    .build());
            current = current.plusDays(1);
        }
        return responses;
    }

    @Override
    public PageData<EmployeeDailyPerformanceResponse> pageEmployeeDailyPerformance(EmployeeDailyPerformanceRequest request, Long tenantId) {
        // 1. 分页查询按天维度统计的业绩数据
        Page<EmployeeDailyPerformanceBO> page = new Page<>(request.getPage(), request.getLimit());
        IPage<EmployeeDailyPerformanceBO> result = schedulePerformanceMapper.pageEmployeeDailyPerformance(
                page,
                tenantId,
                request.getEmployeeId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getLiveRoomName(),
                request.getSortField(),
                request.getSortOrder()
        );

        if (result.getRecords().isEmpty()) {
            return PageData.empty();
        }

        // 2. 收集所有直播间ID
        Set<Long> allLiveRoomIds = result.getRecords().stream()
                .filter(bo -> bo.getLiveRoomIds() != null && !bo.getLiveRoomIds().isEmpty())
                .flatMap(bo -> Arrays.stream(bo.getLiveRoomIds().split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        // 3. 批量获取直播间名称
        Map<Long, String> liveRoomNameMap = allLiveRoomIds.isEmpty()
                ? Collections.emptyMap()
                : liveRoomService.getLiveRoomNameMap(new ArrayList<>(allLiveRoomIds));

        // 4. 转换响应对象并填充直播间列表
        List<EmployeeDailyPerformanceResponse> responseList = result.getRecords().stream()
                .map(bo -> {
                    // 将逗号分隔的ID转换为直播间列表
                    List<EmployeeDailyPerformanceResponse.LiveRoomInfo> liveRoomList =
                            bo.getLiveRoomIds() != null && !bo.getLiveRoomIds().isEmpty()
                                    ? Arrays.stream(bo.getLiveRoomIds().split(","))
                                            .map(String::trim)
                                            .filter(s -> !s.isEmpty())
                                            .map(Long::parseLong)
                                            .map(id -> EmployeeDailyPerformanceResponse.LiveRoomInfo.builder()
                                                    .liveRoomId(id)
                                                    .liveRoomName(liveRoomNameMap.getOrDefault(id, ""))
                                                    .build())
                                            .collect(Collectors.toList())
                                    : Collections.emptyList();

                    return EmployeeDailyPerformanceResponse.builder()
                            .statsDate(bo.getStatsDate())
                            .liveRoomList(liveRoomList)
                            .scheduleCount(bo.getScheduleCount())
                            .liveDurationMinutes(bo.getLiveDurationMinutes())
                            .viewCount(bo.getViewCount())
                            .salesRevenue(bo.getSalesRevenue())
                            .refund(bo.getRefund())
                            .netSales(bo.getNetSales())
                            .investment(bo.getInvestment())
                            .roi(bo.getRoi())
                            .build();
                })
                .collect(Collectors.toList());

        return new PageData<>(responseList, result.getTotal(), request.getLimit(), request.getPage(), result.getPages());
    }

    @Override
    public PageData<EmployeeSchedulePerformanceResponse> pageEmployeeSchedulePerformance(EmployeeSchedulePerformanceRequest request, Long tenantId) {
        // 分页查询
        Page<EmployeeSchedulePerformanceBO> page = new Page<>(request.getPage(), request.getLimit());
        IPage<EmployeeSchedulePerformanceBO> result = schedulePerformanceMapper.pageEmployeeSchedulePerformance(
                page,
                tenantId,
                request.getEmployeeId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getLiveRoomName(),
                request.getSortField(),
                request.getSortOrder()
        );

        if (result.getRecords().isEmpty()) {
            return PageData.empty();
        }

        // 收集直播间ID
        List<Long> liveRoomIds = result.getRecords().stream()
                .map(EmployeeSchedulePerformanceBO::getLiveRoomId)
                .distinct()
                .toList();

        // 批量获取直播间名称
        Map<Long, String> liveRoomNameMap = liveRoomService.getLiveRoomNameMap(liveRoomIds);

        // 收集业绩ID
        List<Long> performanceIds = result.getRecords().stream()
                .map(EmployeeSchedulePerformanceBO::getId)
                .collect(Collectors.toList());

        // 批量查询原始值
        Map<Long, SessionOriginalValue> ovMap = sessionOriginalValueMapper.selectList(
                new LambdaQueryWrapper<SessionOriginalValue>()
                        .in(SessionOriginalValue::getSourceId, performanceIds)
                        .eq(SessionOriginalValue::getSourceType, SessionOriginalValue.SOURCE_TYPE_SCHEDULE_PERFORMANCE)
                        .eq(SessionOriginalValue::getSource, 1))
                .stream()
                .collect(Collectors.toMap(SessionOriginalValue::getSourceId, ov -> ov, (a, b) -> a));

        // 组装响应
        List<EmployeeSchedulePerformanceResponse> responseList = result.getRecords().stream()
                .map(bo -> {
                    PerformanceData pd = schedulePerformanceService.buildPerformanceData(bo, null, ovMap.get(bo.getId()));
                    return EmployeeSchedulePerformanceResponse.builder()
                            .id(bo.getId())
                            .liveRoomId(bo.getLiveRoomId())
                            .liveRoomName(liveRoomNameMap.get(bo.getLiveRoomId()))
                            .startTime(bo.getStartTime())
                            .endTime(bo.getEndTime())
                            .source(bo.getSource())
                            .performanceData(pd)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageData<>(responseList, result.getTotal(), request.getLimit(), request.getPage(), result.getPages());
    }
}
