package com.jiuyu.governance.business.performance.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.oauth.client.annotation.RequiredLogin;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.performance.pojo.request.*;
import com.jiuyu.governance.business.performance.pojo.response.*;
import com.jiuyu.governance.business.performance.service.PerformanceSummaryService;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.business.performance.pojo.response.export.DailyPerformanceExport;
import com.jiuyu.governance.business.performance.pojo.response.export.PerformanceSummaryExport;
import com.jiuyu.governance.plugins.excel.ExcelTemplate;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 业绩汇总控制器
 * <p>
 * 提供业绩数据的汇总统计功能，支持多维度查询：
 * - 分公司维度：统计分公司的所有业绩
 * - 部门维度：统计部门的所有业绩
 * - 小组维度：统计小组的所有业绩
 * - 直播间维度：统计直播间的所有业绩
 * </p>
 * <p>
 * 组织关系：
 * 1、租户 &gt; 分公司 &gt; 部门 &gt; 小组 &gt; 直播间
 * 2、租户 &gt; 部门 &gt; 小组 &gt; 直播间
 * 3、租户 &gt; 部门 &gt; 直播间
 * 4、租户 &gt; 直播间
 * </p>
 * <p>
 * 数据计算说明：
 * - ROI = 销售额 / 投放，若投放为0则ROI返回null
 * - 场次数量的统计口径：按实际开播场次，一场直播对应多条业绩记录算一条
 * - 直播时长：(结束时间 - 开始时间) / 60，单位：分钟
 * </p>
 * <p>
 * 时段定义：
 * - 今天：当天 00:00:00 至当前时间
 * - 昨天：昨天 00:00:00 至 23:59:59
 * - 本周：本周一 00:00:00 至当前时间（周一作为一周开始）
 * - 上周：上周一 00:00:00 至上周日 23:59:59
 * - 本月：本月1日 00:00:00 至当前时间
 * - 上月：上月1日 00:00:00 至上月最后一天 23:59:59
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/performance/summary")
@RequiredArgsConstructor
public class PerformanceSummaryController {

    private final PerformanceSummaryService performanceSummaryService;

    private final DataPermissionsHandler dataPermissionsHandler;

    /**
     * 解析sourceType到数据权限类型
     */
    private String resolveDataType(String sourceType) {
        if (sourceType == null) return null;
        return switch (sourceType) {
            case "subCompany" -> OauthConstant.COMPANY;
            case "dept" -> OauthConstant.DEPT;
            case "team" -> OauthConstant.TEAM;
            case "liveRoom" -> OauthConstant.LIVE_ROOM;
            default -> null;
        };
    }

    /**
     * 检查用户是否有指定数据ID的权限
     */
    private boolean hasDataPermission(AccessUser accessUser, String dataType, Long dataId) {
        if (accessUser == null || dataType == null || dataId == null) return false;
        if (OauthConstant.isTenantAdmin(accessUser)) return true;
        Map<String, List<Long>> userDataIds = dataPermissionsHandler.getUserDataIds(accessUser, List.of(dataType));
        if (EmptyUtil.isEmpty(userDataIds) || !userDataIds.containsKey(dataType)) return false;
        List<Long> dataIds = userDataIds.get(dataType);
        return dataIds.contains(dataId) || dataIds.contains(0L);
    }

    /**
     * 各分公司业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各分公司分页列表，集团业绩汇总罗盘-各公司
     * 主表为分公司，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 查询请求，包含name（模糊查询）、startDate、endDate、page、limit
     * @return 分页数据，包含分公司ID、名称、场观、销售额、退款、净销售额、投放、ROI
     */
    @Permissions("performance:summary:branch-performance:list")
    @PostMapping("/sub-company/page")
    @RequiredLogin
    public ApiResponse<PageData<PerformanceSummaryResponse>> pageQuerySubCompanyPerformance(@Valid @RequestBody SubCompanyPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(performanceSummaryService.pageQuerySubCompanyPerformance(request, accessUser));
    }

    /**
     * 各部门业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各部门分页列表，分公司业绩汇总详情-部门数据
     * 主表为部门，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 查询请求，包含name（模糊查询）、companyId（可选）、startDate、endDate、page、limit
     * @return 分页数据，包含部门ID、名称、场观、销售额、退款、净销售额、投放、ROI
     */
    @Permissions("performance:summary:department-performance:list")
    @PostMapping("/dept/page")
    @RequiredLogin
    public ApiResponse<PageData<PerformanceSummaryResponse>> pageQueryDeptPerformance(@Valid @RequestBody DeptPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(performanceSummaryService.pageQueryDeptPerformance(request, accessUser));
    }

    /**
     * 各小组业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各小组分页列表，分公司业绩汇总详情-小组数据，部门业绩汇总详情-小组数据
     * 主表为小组，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 查询请求，包含name（模糊查询）、companyId（可选）、deptId（可选）、startDate、endDate、page、limit
     * @return 分页数据，包含小组ID、名称、场观、销售额、退款、净销售额、投放、ROI
     */
    @Permissions("performance:summary:team-performance:list")
    @PostMapping("/team/page")
    @RequiredLogin
    public ApiResponse<PageData<PerformanceSummaryResponse>> pageQueryTeamPerformance(@Valid @RequestBody TeamPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(performanceSummaryService.pageQueryTeamPerformance(request, accessUser));
    }

    /**
     * 各直播间业绩数据分页查询
     * <p>
     * 使用场景：业绩汇总-各直播间分页列表，分公司/部门/小组业绩汇总详情-直播间数据
     * 主表为直播间，统计对应的业绩数据，业绩来源表为session_performance
     * </p>
     *
     * @param request 查询请求，包含name（模糊查询）、companyId/deptId/teamId（可选）、startDate、endDate、page、limit
     * @return 分页数据，包含直播间ID、名称（主播名称）、场观、销售额、退款、净销售额、投放、ROI
     */
    @Permissions("performance:summary:live-room-performance:list")
    @PostMapping("/live-room/page")
    @RequiredLogin
    public ApiResponse<PageData<PerformanceSummaryResponse>> pageQueryLiveRoomPerformance(@Valid @RequestBody LiveRoomPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(performanceSummaryService.pageQueryLiveRoomPerformance(request, accessUser));
    }

    /**
     * 获取组织数量统计
     * <p>
     * 使用场景：各级业绩汇总罗盘/详情页-名称和数量统计
     * 不适用场景：直播间详情页的头部显示
     * 根据sourceType返回不同层级的下属组织数量
     * </p>
     *
     * @param request 查询请求，包含sourceId（来源ID）、sourceType（tenant/subCompany/dept/team）
     * @return 组织数量统计，包含sourceName、companyCount、deptCount、teamCount、liveRoomCount
     */
    @Permissions("performance:summary:group-compass:page")
    @PostMapping("/org/count")
    @RequiredLogin
    public ApiResponse<OrgCountResponse> getOrgCount(@Valid @RequestBody OrgCountRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        String dataType = resolveDataType(request.getSourceType());
        if (dataType != null && !hasDataPermission(accessUser, dataType, request.getSourceId())) {
            return ApiResponse.success(new OrgCountResponse());
        }
        return ApiResponse.success(performanceSummaryService.getOrgCount(request));
    }

    /**
     * 获取业绩时段统计
     * <p>
     * 使用场景：业绩汇总详情页-业绩天、周、月的汇总统计
     * 统计维度为：租户、分公司、部门、小组、直播间中的一个
     * 返回六个时段（今天/昨天/本周/上周/本月/上月）的场次、场观、销售额、退款、净销售额、投放
     * </p>
     *
     * @param request 查询请求，包含sourceId（来源ID）、sourceType（tenant/subCompany/dept/team/liveRoom）
     * @return 时段统计数据，每个指标包含today/yesterday/thisWeek/lastWeek/thisMonth/lastMonth
     */
    @Permissions("performance:summary:group-compass:page")
    @PostMapping("/period-stats")
    @RequiredLogin
    public ApiResponse<PerformancePeriodStatsResponse> getPeriodStats(@Valid @RequestBody PerformancePeriodStatsRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        String dataType = resolveDataType(request.getSourceType());
        if (dataType != null && !hasDataPermission(accessUser, dataType, request.getSourceId())) {
            return ApiResponse.success(new PerformancePeriodStatsResponse());
        }
        return ApiResponse.success(performanceSummaryService.getPeriodStats(request));
    }

    /**
     * 获取数据趋势（柱形图）
     * <p>
     * 使用场景：业绩汇总详情页-数据趋势柱形图
     * 按日期维度返回每日汇总数据，日期范围内无数据的日期也会返回（值为0）
     * </p>
     *
     * @param request 查询请求，包含sourceId、sourceType、startDate、endDate
     * @return 每日业绩数据列表（按日期升序），包含date、viewCount、salesRevenue、refund、netSales、investment
     */
    @Permissions("performance:summary:group-compass:page")
    @PostMapping("/trend")
    @RequiredLogin
    public ApiResponse<List<DailyPerformanceResponse>> getTrendData(@Valid @RequestBody PerformanceTrendRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        String dataType = resolveDataType(request.getSourceType());
        if (dataType != null && !hasDataPermission(accessUser, dataType, request.getSourceId())) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(performanceSummaryService.getTrendData(request));
    }

    /**
     * 获取数据详情分页列表
     * <p>
     * 使用场景：业绩汇总详情页-数据详情分页列表
     * 以日期(yyyy-MM-dd)为维度的汇总数据，仅返回有数据的日期
     * </p>
     *
     * @param request 查询请求，包含sourceId、sourceType、startDate、endDate、page、limit
     * @return 分页数据（按日期降序），包含date、viewCount、salesRevenue、refund、netSales、investment
     */
    @Permissions("performance:summary:group-compass:page")
    @PostMapping("/daily/page")
    @RequiredLogin
    public ApiResponse<PageData<DailyPerformanceResponse>> pageQueryDailyPerformance(@Valid @RequestBody PerformanceDailyPageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        String dataType = resolveDataType(request.getSourceType());
        if (dataType != null && !hasDataPermission(accessUser, dataType, request.getSourceId())) {
            return ApiResponse.success(PageData.empty());
        }
        return ApiResponse.success(performanceSummaryService.pageQueryDailyPerformance(request));
    }

    /**
     * 导出数据详情列表（Excel）
     * <p>
     * 使用场景：业绩汇总详情页-数据详情列表导出
     * 与分页接口参数相同，不传 page/limit，导出日期范围内全部数据
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 sourceId、sourceType、startDate、endDate
     * @param accessUser 当前登录用户上下文
     *
     * @return Excel 文件字节流
     */
    @Permissions("performance:summary:group-compass:page")
    @GetMapping("/daily/export")
    @RequiredLogin
    public ResponseEntity<byte[]> exportDailyPerformance(@Valid PerformanceDailyPageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        String dataType = resolveDataType(request.getSourceType());
        if (dataType != null && !hasDataPermission(accessUser, dataType, request.getSourceId())) {
            return ExcelTemplate.downloadList(Collections.emptyList(), "每日业绩数据.xlsx", "每日业绩", Collections.emptySet(), DailyPerformanceExport.class);
        }
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        List<DailyPerformanceResponse> result = performanceSummaryService.loadQueryDailyPerformance(request);
        List<DailyPerformanceExport> rows = BeanUtil.copyToList(ObjUtil.defaultIfNull(result, new ArrayList<DailyPerformanceResponse>()), DailyPerformanceExport.class);
        return ExcelTemplate.downloadList(
                rows,
                "每日业绩数据.xlsx",
                "每日业绩",
                Collections.emptySet(),
                DailyPerformanceExport.class
        );
    }

    /**
     * 获取销售额汇总
     * <p>
     * 使用场景：分公司占比（前端计算）、top部门、top小组、top直播间
     * 以dimensionType指定的维度汇总销售额，支持通过companyId/deptId/teamId进行层级筛选
     * </p>
     *
     * @param request 查询请求，包含dimensionType（subCompany/dept/team/liveRoom）、companyId/deptId/teamId（可选）、startDate、endDate
     * @return 销售额汇总列表（按销售额降序），包含id、name、salesRevenue
     */
    @Permissions("performance:summary:group-compass:page")
    @PostMapping("/sales-revenue/summary")
    @RequiredLogin
    public ApiResponse<List<SalesRevenueSummaryResponse>> getSalesRevenueSummary(@Valid @RequestBody SalesRevenueSummaryRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        if (request.getCompanyId() != null && !hasDataPermission(accessUser, OauthConstant.COMPANY, request.getCompanyId())) {
            return ApiResponse.success(List.of());
        }
        if (request.getDeptId() != null && !hasDataPermission(accessUser, OauthConstant.DEPT, request.getDeptId())) {
            return ApiResponse.success(List.of());
        }
        if (request.getTeamId() != null && !hasDataPermission(accessUser, OauthConstant.TEAM, request.getTeamId())) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(performanceSummaryService.getSalesRevenueSummary(request));
    }

    /**
     * 导出各分公司业绩数据（Excel）
     * <p>
     * 使用场景：业绩汇总-各分公司列表导出
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 name（可选）、startDate、endDate
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @Permissions("performance:summary:branch-performance:list")
    @GetMapping("/sub-company/export")
    @RequiredLogin
    public ResponseEntity<byte[]> exportSubCompanyPerformance(@Valid SubCompanyPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        List<PerformanceSummaryResponse> result = performanceSummaryService.listQuerySubCompanyPerformance(request, accessUser);
        List<PerformanceSummaryExport> rows = BeanUtil.copyToList(
                ObjUtil.defaultIfNull(result, new ArrayList<PerformanceSummaryResponse>()), PerformanceSummaryExport.class);
        return ExcelTemplate.downloadList(rows, "各分公司业绩数据.xlsx", "各分公司业绩", Collections.emptySet(), PerformanceSummaryExport.class);
    }

    /**
     * 导出各部门业绩数据（Excel）
     * <p>
     * 使用场景：业绩汇总-各部门列表导出
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 name（可选）、companyId（可选）、startDate、endDate
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @Permissions("performance:summary:department-performance:list")
    @GetMapping("/dept/export")
    @RequiredLogin
    public ResponseEntity<byte[]> exportDeptPerformance(@Valid DeptPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        List<PerformanceSummaryResponse> result = performanceSummaryService.listQueryDeptPerformance(request, accessUser);
        List<PerformanceSummaryExport> rows = BeanUtil.copyToList(
                ObjUtil.defaultIfNull(result, new ArrayList<PerformanceSummaryResponse>()), PerformanceSummaryExport.class);
        return ExcelTemplate.downloadList(rows, "各部门业绩数据.xlsx", "各部门业绩", Collections.emptySet(), PerformanceSummaryExport.class);
    }

    /**
     * 导出各小组业绩数据（Excel）
     * <p>
     * 使用场景：业绩汇总-各小组列表导出
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 name（可选）、companyId（可选）、deptId（可选）、startDate、endDate
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @Permissions("performance:summary:team-performance:list")
    @GetMapping("/team/export")
    @RequiredLogin
    public ResponseEntity<byte[]> exportTeamPerformance(@Valid TeamPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        List<PerformanceSummaryResponse> result = performanceSummaryService.listQueryTeamPerformance(request, accessUser);
        List<PerformanceSummaryExport> rows = BeanUtil.copyToList(
                ObjUtil.defaultIfNull(result, new ArrayList<PerformanceSummaryResponse>()), PerformanceSummaryExport.class);
        return ExcelTemplate.downloadList(rows, "各小组业绩数据.xlsx", "各小组业绩", Collections.emptySet(), PerformanceSummaryExport.class);
    }

    /**
     * 导出各直播间业绩数据（Excel）
     * <p>
     * 使用场景：业绩汇总-各直播间列表导出
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 name（可选）、companyId（可选）、deptId（可选）、teamId（可选）、startDate、endDate
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @Permissions("performance:summary:live-room-performance:list")
    @GetMapping("/live-room/export")
    @RequiredLogin
    public ResponseEntity<byte[]> exportLiveRoomPerformance(@Valid LiveRoomPerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        List<PerformanceSummaryResponse> result = performanceSummaryService.listQueryLiveRoomPerformance(request, accessUser);
        List<PerformanceSummaryExport> rows = BeanUtil.copyToList(
                ObjUtil.defaultIfNull(result, new ArrayList<PerformanceSummaryResponse>()), PerformanceSummaryExport.class);
        return ExcelTemplate.downloadList(rows, "各直播间业绩数据.xlsx", "各直播间业绩", Collections.emptySet(), PerformanceSummaryExport.class);
    }
}
