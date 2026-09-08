package com.jiuyu.governance.business.performance.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import com.jiuyu.governance.business.org.mapper.DeptMapper;
import com.jiuyu.governance.business.org.mapper.SubCompanyMapper;
import com.jiuyu.governance.business.org.mapper.TeamMapper;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.performance.mapper.LiveRoomPerformanceMapper;
import com.jiuyu.governance.business.performance.mapper.OrgPerformanceMapper;
import com.jiuyu.governance.business.performance.mapper.SchedulePerformanceMapper;
import com.jiuyu.governance.business.performance.mapper.SessionPerformanceMapper;
import com.jiuyu.governance.business.performance.pojo.bo.MultiPeriodSessionStatsBO;
import com.jiuyu.governance.business.performance.pojo.bo.DailyPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.PerformanceAggregationBO;
import com.jiuyu.governance.business.performance.pojo.bo.LiveRoomPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.OrgPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.PeriodPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.constants.SourceType;
import com.jiuyu.governance.business.performance.pojo.entity.SessionPerformance;
import com.jiuyu.governance.business.performance.pojo.request.*;
import com.jiuyu.governance.business.performance.pojo.response.*;
import com.jiuyu.governance.business.performance.service.PerformanceSummaryService;
import com.jiuyu.governance.business.performance.utils.DataUtil;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业绩汇总服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PerformanceSummaryServiceImpl implements PerformanceSummaryService {

    private final SubCompanyMapper subCompanyMapper;
    private final DeptMapper deptMapper;
    private final TeamMapper teamMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final LiveRoomPerformanceMapper liveRoomPerformanceMapper;
    private final OrgPerformanceMapper orgPerformanceMapper;
    private final SessionPerformanceMapper sessionPerformanceMapper;
    private final SchedulePerformanceMapper schedulePerformanceMapper;

    @Override
    public PageData<PerformanceSummaryResponse> pageQuerySubCompanyPerformance(SubCompanyPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getCompanyIds())) {
            return PageData.empty();
        }
        PageData<OrgPerformanceBO> pageData = CustomPage.execute(request, (page, req) ->
            orgPerformanceMapper.pageQuerySubCompanyPerformance(page, req));

        return pageData.conversion(bo -> PerformanceSummaryResponse.builder()
            .id(bo.getId())
            .name(bo.getName())
            .viewCount(bo.getViewCount())
            .salesRevenue(bo.getSalesRevenue())
            .refund(bo.getRefund())
            .netSales(bo.getNetSales())
            .investment(bo.getInvestment())
            .roi(calcRoi(bo.getSalesRevenue(), bo.getInvestment()))
            .build());
    }


    /**
     * 各分公司业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各分公司列表，集团业绩汇总罗盘-各公司
     * 主表为分公司，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 列表查询请求
     *
     * @return 列表数据，包含分公司名称、分公司ID、场观、销售额、退款、净销售额、投放、ROI
     */
    @Override
    public List<PerformanceSummaryResponse> listQuerySubCompanyPerformance(SubCompanyPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getCompanyIds())) {
            return List.of();
        }
        List<SubCompany> companyList = ChainWrappers.lambdaQueryChain(subCompanyMapper)
            .eq(SubCompany::getTenantId, request.getTenantId())
            .eq(SubCompany::getIsDeleted, false)
            .in(EmptyUtil.isNotEmpty(request.getCompanyIds()), SubCompany::getId, request.getCompanyIds())
            .like(EmptyUtil.isNotEmpty(request.getName()), SubCompany::getName, request.getName())
            .list().stream().sorted(Comparator.comparingInt(SubCompany::getSort)).toList();
        if (EmptyUtil.isEmpty(companyList)) {
            return List.of();
        }
        List<Long> ids = companyList.stream().map(SubCompany::getId).collect(Collectors.toList());
        Map<Long, PerformanceSummaryResponse> map = queryPerformanceByIds(request.getTenantId(), ids, "company_id", request.getStartDate(), request.getEndDate());

        return companyList.stream().map(c -> {
            return map.getOrDefault(c.getId(), buildEmptyResponse());
        }).toList();
    }

    @Override
    public PageData<PerformanceSummaryResponse> pageQueryDeptPerformance(DeptPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getDeptIds())) {
            return PageData.empty();
        }
        PageData<OrgPerformanceBO> pageData = CustomPage.execute(request, (page, req) ->
            orgPerformanceMapper.pageQueryDeptPerformance(page, req));

        return pageData.conversion(bo -> PerformanceSummaryResponse.builder()
            .id(bo.getId())
            .name(bo.getName())
            .viewCount(bo.getViewCount())
            .salesRevenue(bo.getSalesRevenue())
            .refund(bo.getRefund())
            .netSales(bo.getNetSales())
            .investment(bo.getInvestment())
            .roi(calcRoi(bo.getSalesRevenue(), bo.getInvestment()))
            .build());
    }

    /**
     * 各部门业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各部门列表，分公司业绩汇总详情-部门数据
     * 主表为部门，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 列表查询请求
     *
     * @return 列表数据，包含部门名称、部门ID、场观、销售额、退款、净销售额、投放、ROI
     */
    @Override
    public List<PerformanceSummaryResponse> listQueryDeptPerformance(DeptPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getDeptIds())) {
            return List.of();
        }
        List<Dept> deptList = ChainWrappers.lambdaQueryChain(deptMapper)
            .eq(Dept::getTenantId, request.getTenantId())
            .eq(Dept::getIsDeleted, false)
            .eq(request.getCompanyId() != null, Dept::getCompanyId, request.getCompanyId())
            .in(EmptyUtil.isNotEmpty(request.getDeptIds()), Dept::getId, request.getDeptIds())
            .like(EmptyUtil.isNotEmpty(request.getName()), Dept::getName, request.getName())
            .list().stream().sorted(Comparator.comparingInt(Dept::getSort)).toList();
        if (EmptyUtil.isEmpty(deptList)) {
            return List.of();
        }
        List<Long> ids = deptList.stream().map(Dept::getId).collect(Collectors.toList());
        Map<Long, PerformanceSummaryResponse> map = queryPerformanceByIds(request.getTenantId(), ids, "dept_id", request.getStartDate(), request.getEndDate());
        return deptList.stream().map(d -> {
            return map.getOrDefault(d.getId(), buildEmptyResponse());
        }).toList();
    }

    @Override
    public PageData<PerformanceSummaryResponse> pageQueryTeamPerformance(TeamPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getTeamIds())) {
            return PageData.empty();
        }
        PageData<OrgPerformanceBO> pageData = CustomPage.execute(request, (page, req) ->
            orgPerformanceMapper.pageQueryTeamPerformance(page, req));

        return pageData.conversion(bo -> PerformanceSummaryResponse.builder()
            .id(bo.getId())
            .name(bo.getName())
            .viewCount(bo.getViewCount())
            .salesRevenue(bo.getSalesRevenue())
            .refund(bo.getRefund())
            .netSales(bo.getNetSales())
            .investment(bo.getInvestment())
            .roi(calcRoi(bo.getSalesRevenue(), bo.getInvestment()))
            .build());
    }

    /**
     * 各小组业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各小组列表，分公司业绩汇总详情-小组数据，部门业绩汇总详情-小组数据
     * 主表为小组，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 列表查询请求
     *
     * @return 列表数据，包含小组名称、小组ID、场观、销售额、退款、净销售额、投放、ROI
     */
    @Override
    public List<PerformanceSummaryResponse> listQueryTeamPerformance(TeamPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getTeamIds())) {
            return List.of();
        }
        List<Team> teamList = ChainWrappers.lambdaQueryChain(teamMapper)
            .eq(Team::getTenantId, request.getTenantId())
            .eq(Team::getIsDeleted, false)
            .eq(request.getCompanyId() != null, Team::getCompanyId, request.getCompanyId())
            .eq(request.getDeptId() != null, Team::getDeptId, request.getDeptId())
            .in(EmptyUtil.isNotEmpty(request.getTeamIds()), Team::getId, request.getTeamIds())
            .like(EmptyUtil.isNotEmpty(request.getName()), Team::getName, request.getName())
            .list().stream().sorted(Comparator.comparingInt(Team::getSort)).toList();
        if (EmptyUtil.isEmpty(teamList)) {
            return List.of();
        }
        List<Long> ids = teamList.stream().map(Team::getId).collect(Collectors.toList());
        Map<Long, PerformanceSummaryResponse> map = queryPerformanceByIds(request.getTenantId(), ids, "team_id", request.getStartDate(), request.getEndDate());
        return teamList.stream().map(t -> {
            return map.getOrDefault(t.getId(), buildEmptyResponse());
        }).toList();
    }

    @Override
    public PageData<PerformanceSummaryResponse> pageQueryLiveRoomPerformance(LiveRoomPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getLiveRoomIds())) {
            return PageData.empty();
        }

        PageData<LiveRoomPerformanceBO> pageData = CustomPage.execute(request, (page, req) ->
            liveRoomPerformanceMapper.pageQueryLiveRoomPerformance(page, req));

        if (pageData.getList().isEmpty()) return PageData.empty();

        return pageData.conversion(bo -> PerformanceSummaryResponse.builder()
            .id(bo.getId())
            .name(bo.getName())
            .anchorAvatar(bo.getAnchorAvatar())
            .viewCount(bo.getViewCount())
            .salesRevenue(bo.getSalesRevenue())
            .refund(bo.getRefund())
            .netSales(bo.getNetSales())
            .investment(bo.getInvestment())
            .roi(calcRoi(bo.getSalesRevenue(), bo.getInvestment()))
            .build());
    }

    /**
     * 各直播间业绩数据列表查询
     * <p>
     * 使用场景：业绩汇总-各直播间列表，分公司业绩汇总详情-直播间数据，
     * 部门业绩汇总详情-直播间数据，小组业绩汇总详情-直播间数据
     * 主表为直播间，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 列表查询请求
     *
     * @return 列表数据，包含直播间名称、直播间ID、场观、销售额、退款、净销售额、投放、ROI
     */
    @Override
    public List<PerformanceSummaryResponse> listQueryLiveRoomPerformance(LiveRoomPerformancePageRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getLiveRoomIds())) {
            return List.of();
        }
        List<LiveRoom> liveRoomList = ChainWrappers.lambdaQueryChain(liveRoomMapper)
            .eq(LiveRoom::getTenantId, request.getTenantId())
            .eq(LiveRoom::getIsDeleted, false)
            .eq(request.getCompanyId() != null, LiveRoom::getCompanyId, request.getCompanyId())
            .eq(request.getDeptId() != null, LiveRoom::getDeptId, request.getDeptId())
            .eq(request.getTeamId() != null, LiveRoom::getTeamId, request.getTeamId())
            .in(EmptyUtil.isNotEmpty(request.getLiveRoomIds()), LiveRoom::getId, request.getLiveRoomIds())
            .like(EmptyUtil.isNotEmpty(request.getName()), LiveRoom::getAnchorName, request.getName())
            .list().stream().sorted(Comparator.comparingLong(LiveRoom::getId)).toList();
        if (EmptyUtil.isEmpty(liveRoomList)) {
            return List.of();
        }
        List<Long> ids = liveRoomList.stream().map(LiveRoom::getId).collect(Collectors.toList());
        Map<Long, PerformanceSummaryResponse> map = queryPerformanceByIds(request.getTenantId(), ids, "live_room_id", request.getStartDate(), request.getEndDate());
        return liveRoomList.stream().map(t -> {
            return map.getOrDefault(t.getId(), buildEmptyResponse());
        }).toList();
    }

    private PerformanceSummaryResponse buildEmptyResponse() {
        return PerformanceSummaryResponse.builder()
            .viewCount(0).salesRevenue(BigDecimal.ZERO).refund(BigDecimal.ZERO)
            .netSales(BigDecimal.ZERO).investment(BigDecimal.ZERO).roi(null).build();
    }

    @Override
    public OrgCountResponse getOrgCount(OrgCountRequest request) {
        Long tenantId = request.getTenantId();
        SourceType sourceType = SourceType.fromValue(request.getSourceType());
        Long sourceId = request.getSourceId();
        OrgCountResponse.OrgCountResponseBuilder builder = OrgCountResponse.builder();

        if (sourceType == null) return builder.build();

        switch (sourceType) {
            case TENANT -> builder.sourceId(tenantId)
                .sourceName("所有公司")
                .companyCount(countCompanies(tenantId))
                .deptCount(countDepts(tenantId, null))
                .teamCount(countTeams(tenantId, null, null))
                .liveRoomCount(countLiveRooms(tenantId, null, null, null));
            case SUB_COMPANY -> {
                SubCompany company = subCompanyMapper.selectById(sourceId);
                builder.sourceId(sourceId)
                    .sourceName(company != null ? company.getName() : null)
                    .companyName(company != null ? company.getName() : null)
                    .deptCount(countDepts(tenantId, sourceId))
                    .teamCount(countTeams(tenantId, sourceId, null))
                    .liveRoomCount(countLiveRooms(tenantId, sourceId, null, null));
            }
            case DEPT -> {
                Dept dept = deptMapper.selectById(sourceId);
                String deptCompanyName = null;
                if (dept != null) {
                    SubCompany company = subCompanyMapper.selectById(dept.getCompanyId());
                    deptCompanyName = company != null ? company.getName() : null;
                }
                builder.sourceId(sourceId)
                    .sourceName(dept != null ? dept.getName() : null)
                    .companyName(deptCompanyName)
                    .deptName(dept != null ? dept.getName() : null)
                    .teamCount(countTeams(tenantId, null, sourceId))
                    .liveRoomCount(countLiveRooms(tenantId, null, sourceId, null));
            }
            case TEAM -> {
                Team team = teamMapper.selectById(sourceId);
                String teamCompanyName = null;
                String teamDeptName = null;
                if (team != null) {
                    SubCompany company = subCompanyMapper.selectById(team.getCompanyId());
                    Dept dept = deptMapper.selectById(team.getDeptId());
                    teamCompanyName = company != null ? company.getName() : null;
                    teamDeptName = dept != null ? dept.getName() : null;
                }
                builder.sourceId(sourceId)
                    .sourceName(team != null ? team.getName() : null)
                    .companyName(teamCompanyName)
                    .deptName(teamDeptName)
                    .teamName(team != null ? team.getName() : null)
                    .liveRoomCount(countLiveRooms(tenantId, null, null, sourceId));
            }
            default -> { /* LIVE_ROOM无下级组织 */ }
        }
        return builder.build();
    }

    @Override
    public PerformancePeriodStatsResponse getPeriodStats(PerformancePeriodStatsRequest request) {
        Long tenantId = request.getTenantId();
        SourceType sourceType = SourceType.fromValue(request.getSourceType());
        Long sourceId = request.getSourceId();

        // 解析组织过滤字段
        String idField = resolveIdField(sourceType);

        // 计算6个时段的日期范围
        DataUtil.PeriodDateRange range = DataUtil.calculatePeriodDateRange();

        // 一次查询：从 schedule_performance 获取全部6个时段的场次数量和直播时长
        MultiPeriodSessionStatsBO sessionStatsBO = schedulePerformanceMapper.queryPeriodSessionStats(
                tenantId, idField, sourceId,
                range.today(), range.yesterday(),
                range.thisWeekStart(),
                range.lastWeekStart(), range.lastWeekEnd(),
                range.thisMonthStart(),
                range.lastMonthStart(), range.lastMonthEnd());

        // 6次查询：从 session_performance 获取观看人次、销售额等指标
        PeriodPerformanceBO todayBo = sessionPerformanceMapper.queryPeriodStats(tenantId, idField, sourceId, range.today(), range.today());
        PeriodPerformanceBO yesterdayBo = sessionPerformanceMapper.queryPeriodStats(tenantId, idField, sourceId, range.yesterday(), range.yesterday());
        PeriodPerformanceBO thisWeekBo = sessionPerformanceMapper.queryPeriodStats(tenantId, idField, sourceId, range.thisWeekStart(), range.today());
        PeriodPerformanceBO lastWeekBo = sessionPerformanceMapper.queryPeriodStats(tenantId, idField, sourceId, range.lastWeekStart(), range.lastWeekEnd());
        PeriodPerformanceBO thisMonthBo = sessionPerformanceMapper.queryPeriodStats(tenantId, idField, sourceId, range.thisMonthStart(), range.today());
        PeriodPerformanceBO lastMonthBo = sessionPerformanceMapper.queryPeriodStats(tenantId, idField, sourceId, range.lastMonthStart(), range.lastMonthEnd());

        // 用 schedule 数据覆盖场次相关字段
        todayBo.setSessionCount(sessionStatsBO.getTodaySessionCount());
        todayBo.setDuration(sessionStatsBO.getTodayDuration());
        yesterdayBo.setSessionCount(sessionStatsBO.getYesterdaySessionCount());
        yesterdayBo.setDuration(sessionStatsBO.getYesterdayDuration());
        thisWeekBo.setSessionCount(sessionStatsBO.getThisWeekSessionCount());
        thisWeekBo.setDuration(sessionStatsBO.getThisWeekDuration());
        lastWeekBo.setSessionCount(sessionStatsBO.getLastWeekSessionCount());
        lastWeekBo.setDuration(sessionStatsBO.getLastWeekDuration());
        thisMonthBo.setSessionCount(sessionStatsBO.getThisMonthSessionCount());
        thisMonthBo.setDuration(sessionStatsBO.getThisMonthDuration());
        lastMonthBo.setSessionCount(sessionStatsBO.getLastMonthSessionCount());
        lastMonthBo.setDuration(sessionStatsBO.getLastMonthDuration());

        return DataUtil.buildPeriodStatsResponse(todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo);
    }

    @Override
    public List<DailyPerformanceResponse> getTrendData(PerformanceTrendRequest request) {
        Long tenantId = request.getTenantId();
        SourceType sourceType = SourceType.fromValue(request.getSourceType());
        Long sourceId = request.getSourceId();
        String idField = resolveIdField(sourceType);

        // SQL层面按天聚合查询
        List<DailyPerformanceBO> dbResults = sessionPerformanceMapper.queryDailyPerformance(
            tenantId, idField, sourceId, request.getStartDate(), request.getEndDate());

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
    public PageData<DailyPerformanceResponse> pageQueryDailyPerformance(PerformanceDailyPageRequest request) {
        Long tenantId = request.getTenantId();
        Long sourceId = request.getSourceId();
        String idField = resolveIdField(SourceType.fromValue(request.getSourceType()));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        long pageSize = request.getLimit();
        long pageNum = request.getPage();

        // 1. 生成起止时间范围内的所有天列表（降序）
        List<DateTime> dateList = DateUtil.rangeToList(DateUtil.beginOfDay(DateUtil.date(startDate)), DateUtil.beginOfDay(DateUtil.date(endDate)), DateField.DAY_OF_YEAR);
        // dataList排序
        dateList.sort(Comparator.reverseOrder());

        List<DateTime> list = ListUtil.page((int) pageNum - 1, (int) pageSize, dateList);

        if (list == null || list.isEmpty()) {
            return new PageData<>(Collections.emptyList(), 0, pageSize, pageNum, 0);
        }


        // 2. 对日期列表分页，计算当前页的起止日期
        LocalDate minDate = Collections.min(dateList).toLocalDateTime().toLocalDate();
        LocalDate maxDate = Collections.max(dateList).toLocalDateTime().toLocalDate();
        // 3. 使用分页后的起止时间查询业绩
        Map<LocalDate, DailyPerformanceBO> dbResults = ObjUtil.defaultIfNull(sessionPerformanceMapper.queryDailyPerformance(tenantId, idField, sourceId, minDate, maxDate), new ArrayList<DailyPerformanceBO>())
            .stream()
            .collect(Collectors.toMap(DailyPerformanceBO::getStatsDate, Function.identity(), (a, b) -> a));

        // 4. 转为Response
        List<DailyPerformanceResponse> resList = list.stream()
            .map(bo -> {
                LocalDate currentDate = bo.toLocalDateTime().toLocalDate();
                DailyPerformanceBO temp = ObjUtil.defaultIfNull(dbResults.get(currentDate), new DailyPerformanceBO());
                return DailyPerformanceResponse.builder()
                    .date(currentDate)
                    .viewCount(ObjUtil.defaultIfNull(temp.getViewCount(), 0))
                    .salesRevenue(ObjUtil.defaultIfNull(temp.getSalesRevenue(), BigDecimal.ZERO))
                    .refund(ObjUtil.defaultIfNull(temp.getRefund(), BigDecimal.ZERO))
                    .netSales(ObjUtil.defaultIfNull(temp.getNetSales(), BigDecimal.ZERO))
                    .investment(ObjUtil.defaultIfNull(temp.getInvestment(), BigDecimal.ZERO))
                    .build();
            })
            .collect(Collectors.toList());

        return new PageData<>(resList, dateList.size(), pageSize, pageNum, dateList.size() / pageSize + (dateList.size() % pageSize != 0 ? 1 : 0));
    }


    /**
     * 获取数据详情列表
     * <p>
     * 使用场景：业绩汇总详情页-数据详情列表
     * 以时间(yyyy-MM-dd)为维度的汇总数据
     * </p>
     *
     * @param request 查询请求
     *
     * @return 列表数据，按日期降序排列
     */
    @Override
    public List<DailyPerformanceResponse> loadQueryDailyPerformance(PerformanceDailyPageRequest request) {
        Long tenantId = request.getTenantId();
        Long sourceId = request.getSourceId();
        String idField = resolveIdField(SourceType.fromValue(request.getSourceType()));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // 1. 生成起止时间范围内的所有天列表（降序）
        List<DateTime> dateList = DateUtil.rangeToList(DateUtil.beginOfDay(DateUtil.date(startDate)), DateUtil.beginOfDay(DateUtil.date(endDate)), DateField.DAY_OF_YEAR);
        // dataList排序
        dateList.sort(Comparator.reverseOrder());

        // 2. 对日期列表分页，计算当前页的起止日期
        LocalDate minDate = Collections.min(dateList).toLocalDateTime().toLocalDate();
        LocalDate maxDate = Collections.max(dateList).toLocalDateTime().toLocalDate();
        // 3. 使用分页后的起止时间查询业绩
        Map<LocalDate, DailyPerformanceBO> dbResults = ObjUtil.defaultIfNull(sessionPerformanceMapper.queryDailyPerformance(tenantId, idField, sourceId, minDate, maxDate), new ArrayList<DailyPerformanceBO>())
            .stream()
            .collect(Collectors.toMap(DailyPerformanceBO::getStatsDate, Function.identity(), (a, b) -> a));

        // 4. 转为Response
        return dateList.stream()
            .map(bo -> {
                LocalDate currentDate = bo.toLocalDateTime().toLocalDate();
                DailyPerformanceBO temp = ObjUtil.defaultIfNull(dbResults.get(currentDate), new DailyPerformanceBO());
                return DailyPerformanceResponse.builder()
                    .date(currentDate)
                    .viewCount(ObjUtil.defaultIfNull(temp.getViewCount(), 0))
                    .salesRevenue(ObjUtil.defaultIfNull(temp.getSalesRevenue(), BigDecimal.ZERO))
                    .refund(ObjUtil.defaultIfNull(temp.getRefund(), BigDecimal.ZERO))
                    .netSales(ObjUtil.defaultIfNull(temp.getNetSales(), BigDecimal.ZERO))
                    .investment(ObjUtil.defaultIfNull(temp.getInvestment(), BigDecimal.ZERO))
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SalesRevenueSummaryResponse> getSalesRevenueSummary(SalesRevenueSummaryRequest request) {
        List<SalesRevenueSummaryResponse> responses = new ArrayList<>();
        String type = request.getDimensionType();

        if ("subCompany".equals(type)) {
            List<SubCompany> list = queryCompanies(request.getTenantId());
            Map<Long, BigDecimal> map = querySalesByCompanyIds(request.getTenantId(), list.stream().map(SubCompany::getId).collect(Collectors.toList()), request.getStartDate(), request.getEndDate());
            responses = list.stream()
                .map(c ->
                    SalesRevenueSummaryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .salesRevenue(map.getOrDefault(c.getId(), BigDecimal.ZERO))
                        .build()
                )
                .sorted(Comparator.comparing(SalesRevenueSummaryResponse::getSalesRevenue, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        } else if ("dept".equals(type)) {
            List<Dept> list = queryDepts(request.getTenantId(), request.getCompanyId());
            Map<Long, BigDecimal> map = querySalesByDeptIds(request.getTenantId(), list.stream().map(Dept::getId).collect(Collectors.toList()), request.getStartDate(), request.getEndDate());
            responses = list.stream().map(d -> SalesRevenueSummaryResponse.builder().id(d.getId()).name(d.getName()).salesRevenue(map.getOrDefault(d.getId(), BigDecimal.ZERO)).build())
                .sorted(Comparator.comparing(SalesRevenueSummaryResponse::getSalesRevenue, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        } else if ("team".equals(type)) {
            List<Team> list = queryTeams(request.getTenantId(), request.getCompanyId(), request.getDeptId());
            Map<Long, BigDecimal> map = querySalesByTeamIds(request.getTenantId(), list.stream().map(Team::getId).collect(Collectors.toList()), request.getStartDate(), request.getEndDate());
            responses = list.stream().map(t -> SalesRevenueSummaryResponse.builder().id(t.getId()).name(t.getName()).salesRevenue(map.getOrDefault(t.getId(), BigDecimal.ZERO)).build())
                .sorted(Comparator.comparing(SalesRevenueSummaryResponse::getSalesRevenue, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        } else if ("liveRoom".equals(type)) {
            List<LiveRoom> list = queryLiveRooms(request.getTenantId(), request.getCompanyId(), request.getDeptId(), request.getTeamId());
            Map<Long, BigDecimal> map = querySalesByLiveRoomIds(request.getTenantId(), list.stream().map(LiveRoom::getId).collect(Collectors.toList()), request.getStartDate(), request.getEndDate());
            responses = list.stream().map(r -> SalesRevenueSummaryResponse.builder().id(r.getId()).name(r.getAnchorName()).salesRevenue(map.getOrDefault(r.getId(), BigDecimal.ZERO)).build())
                .sorted(Comparator.comparing(SalesRevenueSummaryResponse::getSalesRevenue, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        }
        return responses;
    }

    // ==================== 私有方法 ====================

    /**
     * 根据来源类型解析对应的数据库字段名
     *
     * @return 数据库字段名，TENANT返回null（不按组织过滤）
     */
    private String resolveIdField(SourceType sourceType) {
        if (sourceType == null) return null;
        return switch (sourceType) {
            case SUB_COMPANY -> "company_id";
            case DEPT -> "dept_id";
            case TEAM -> "team_id";
            case LIVE_ROOM -> "live_room_id";
            default -> null;
        };
    }

    /**
     * 按组织维度聚合查询业绩数据（SQL层面聚合）
     */
    private Map<Long, PerformanceSummaryResponse> queryPerformanceByIds(Long tenantId, List<Long> ids, String idField, LocalDate start, LocalDate end) {
        if (EmptyUtil.isEmpty(ids)) return Collections.emptyMap();
        List<PerformanceAggregationBO> list = sessionPerformanceMapper.queryPerformanceByIds(tenantId, ids, idField, start, end);
        return list.stream().collect(Collectors.toMap(
            PerformanceAggregationBO::getId,
            bo -> PerformanceSummaryResponse.builder()
                .viewCount(bo.getViewCount())
                .salesRevenue(bo.getSalesRevenue())
                .refund(bo.getRefund())
                .netSales(bo.getNetSales())
                .investment(bo.getInvestment())
                .roi(calcRoi(bo.getSalesRevenue(), bo.getInvestment()))
                .build()
        ));
    }

    private BigDecimal calcRoi(BigDecimal sales, BigDecimal investment) {
        if (sales == null || investment == null || investment.compareTo(BigDecimal.ZERO) == 0) return null;
        return sales.divide(investment, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumDecimal(List<SessionPerformance> list, Function<SessionPerformance, BigDecimal> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer countCompanies(Long tenantId) {
        return Math.toIntExact(subCompanyMapper.selectCount(new LambdaQueryWrapper<SubCompany>()
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getIsDeleted, false)));
    }

    private Integer countDepts(Long tenantId, Long companyId) {
        if (companyId == null && tenantId == null) return 0;
        return Math.toIntExact(deptMapper.selectCount(new LambdaQueryWrapper<Dept>()
            .eq(Dept::getIsDeleted, false)
            .eq(ObjUtil.isNotNull(tenantId), Dept::getTenantId, tenantId)
            .eq(ObjUtil.isNotNull(companyId), Dept::getCompanyId, companyId)));
    }

    private Integer countTeams(Long tenantId, Long companyId, Long deptId) {
        if (companyId == null && tenantId == null) return 0;
        return Math.toIntExact(teamMapper.selectCount(new LambdaQueryWrapper<Team>()
                .eq(Team::getIsDeleted, false)
                .eq(ObjUtil.isNotNull(tenantId), Team::getTenantId, tenantId)
                .eq(ObjUtil.isNotNull(companyId), Team::getCompanyId, companyId)
                .eq(ObjUtil.isNotNull(deptId), Team::getDeptId, deptId)
            )
        );
    }

    private Integer countLiveRooms(Long tenantId, Long companyId, Long deptId, Long teamId) {
        if (companyId == null && tenantId == null) return 0;
        return Math.toIntExact(liveRoomMapper.selectCount(new LambdaQueryWrapper<LiveRoom>()
            .eq(LiveRoom::getIsDeleted, false)
            .eq(ObjUtil.isNotNull(tenantId), LiveRoom::getTenantId, tenantId)
            .eq(ObjUtil.isNotNull(companyId), LiveRoom::getCompanyId, companyId)
            .eq(ObjUtil.isNotNull(deptId), LiveRoom::getDeptId, deptId)
            .eq(ObjUtil.isNotNull(teamId), LiveRoom::getTeamId, teamId)
        ));
    }

    private List<SessionPerformance> queryPerformances(Long tenantId, String sourceTypeStr, Long sourceId, LocalDate start, LocalDate end) {
        SourceType sourceType = SourceType.fromValue(sourceTypeStr);
        String idField = resolveIdField(sourceType);
        LambdaQueryWrapper<SessionPerformance> w = new LambdaQueryWrapper<SessionPerformance>()
            .eq(SessionPerformance::getTenantId, tenantId)
            .ge(SessionPerformance::getStatsDate, start)
            .le(SessionPerformance::getStatsDate, end);
        if (idField != null && sourceId != null) {
            switch (sourceType) {
                case SUB_COMPANY -> w.eq(SessionPerformance::getCompanyId, sourceId);
                case DEPT -> w.eq(SessionPerformance::getDeptId, sourceId);
                case TEAM -> w.eq(SessionPerformance::getTeamId, sourceId);
                case LIVE_ROOM -> w.eq(SessionPerformance::getLiveRoomId, sourceId);
                default -> {
                }
            }
        }
        return sessionPerformanceMapper.selectList(w);
    }

    private List<SubCompany> queryCompanies(Long tenantId) {
        if (tenantId == null) return Collections.emptyList();
        return subCompanyMapper.selectList(new LambdaQueryWrapper<SubCompany>()
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getIsDeleted, false)
            .orderByAsc(SubCompany::getSort));
    }

    private List<Dept> queryDepts(Long tenantId, Long companyId) {
        if (companyId == null && tenantId == null) return Collections.emptyList();
        return deptMapper.selectList(new LambdaQueryWrapper<Dept>()
            .eq(Dept::getIsDeleted, false)
            .eq(ObjUtil.isNotNull(tenantId), Dept::getTenantId, tenantId)
            .eq(ObjUtil.isNotNull(companyId), Dept::getCompanyId, companyId)
            .orderByAsc(Dept::getSort)
        );
    }

    private List<Team> queryTeams(Long tenantId, Long companyId, Long deptId) {
        if (companyId == null && tenantId == null && deptId == null) return Collections.emptyList();
        return teamMapper.selectList(new LambdaQueryWrapper<Team>()
            .eq(Team::getIsDeleted, false)
            .eq(ObjUtil.isNotNull(tenantId), Team::getTenantId, tenantId)
            .eq(ObjUtil.isNotNull(companyId), Team::getCompanyId, companyId)
            .eq(ObjUtil.isNotNull(deptId), Team::getDeptId, deptId)
            .orderByAsc(Team::getSort)
        );
    }

    private List<LiveRoom> queryLiveRooms(Long tenantId, Long companyId, Long deptId, Long teamId) {
        LambdaQueryWrapper<LiveRoom> w = new LambdaQueryWrapper<LiveRoom>().eq(LiveRoom::getTenantId, tenantId).eq(LiveRoom::getIsDeleted, false);
        if (companyId != null) w.eq(LiveRoom::getCompanyId, companyId);
        if (deptId != null) w.eq(LiveRoom::getDeptId, deptId);
        if (teamId != null) w.eq(LiveRoom::getTeamId, teamId);
        return liveRoomMapper.selectList(w);
    }

    private Map<Long, BigDecimal> querySalesByCompanyIds(Long tenantId, List<Long> ids, LocalDate start, LocalDate end) {
        if (EmptyUtil.isEmpty(ids)) return Collections.emptyMap();
        return sessionPerformanceMapper.selectList(new LambdaQueryWrapper<SessionPerformance>()
                .eq(SessionPerformance::getTenantId, tenantId)
                .in(SessionPerformance::getCompanyId, ids)
                .ge(SessionPerformance::getStatsDate, start)
                .le(SessionPerformance::getStatsDate, end))
            .stream()
            .collect(Collectors.groupingBy(SessionPerformance::getCompanyId))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> sumDecimal(e.getValue(), SessionPerformance::getSalesRevenue)));
    }

    private Map<Long, BigDecimal> querySalesByDeptIds(Long tenantId, List<Long> ids, LocalDate start, LocalDate end) {
        if (EmptyUtil.isEmpty(ids)) return Collections.emptyMap();
        return sessionPerformanceMapper.selectList(new LambdaQueryWrapper<SessionPerformance>()
                .eq(SessionPerformance::getTenantId, tenantId).in(SessionPerformance::getDeptId, ids)
                .ge(SessionPerformance::getStatsDate, start).le(SessionPerformance::getStatsDate, end))
            .stream().collect(Collectors.groupingBy(SessionPerformance::getDeptId)).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> sumDecimal(e.getValue(), SessionPerformance::getSalesRevenue)));
    }

    private Map<Long, BigDecimal> querySalesByTeamIds(Long tenantId, List<Long> ids, LocalDate start, LocalDate end) {
        if (EmptyUtil.isEmpty(ids)) return Collections.emptyMap();
        return sessionPerformanceMapper.selectList(new LambdaQueryWrapper<SessionPerformance>()
                .eq(SessionPerformance::getTenantId, tenantId).in(SessionPerformance::getTeamId, ids)
                .ge(SessionPerformance::getStatsDate, start).le(SessionPerformance::getStatsDate, end))
            .stream().collect(Collectors.groupingBy(SessionPerformance::getTeamId)).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> sumDecimal(e.getValue(), SessionPerformance::getSalesRevenue)));
    }

    private Map<Long, BigDecimal> querySalesByLiveRoomIds(Long tenantId, List<Long> ids, LocalDate start, LocalDate end) {
        if (EmptyUtil.isEmpty(ids)) return Collections.emptyMap();
        return sessionPerformanceMapper.selectList(new LambdaQueryWrapper<SessionPerformance>()
                .eq(SessionPerformance::getTenantId, tenantId).in(SessionPerformance::getLiveRoomId, ids)
                .ge(SessionPerformance::getStatsDate, start).le(SessionPerformance::getStatsDate, end))
            .stream().collect(Collectors.groupingBy(SessionPerformance::getLiveRoomId)).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> sumDecimal(e.getValue(), SessionPerformance::getSalesRevenue)));
    }
}
