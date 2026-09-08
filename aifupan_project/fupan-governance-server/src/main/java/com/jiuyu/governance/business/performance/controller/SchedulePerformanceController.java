package com.jiuyu.governance.business.performance.controller;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.oauth.client.annotation.RequiredLogin;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.constants.DataSource;
import com.jiuyu.governance.business.performance.pojo.request.DailyPerformanceStatsRequest;
import com.jiuyu.governance.business.performance.pojo.request.ScheduleListRequest;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformanceSaveRequest;
import com.jiuyu.governance.business.performance.pojo.response.*;
import com.jiuyu.governance.business.performance.pojo.response.export.DailyPerformanceStatsExport;
import com.jiuyu.governance.business.performance.pojo.response.export.SchedulePerformanceExport;
import com.jiuyu.governance.business.performance.service.SchedulePerformanceService;
import com.jiuyu.governance.business.performance.utils.DataUtil;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomQueryRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.plugins.excel.ExcelTemplate;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import cn.hutool.core.util.ObjUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 直播间业绩列表和排班业绩控制器
 * <p>
 * 提供排班维度的业绩统计功能，支持时间范围和主播名称筛选
 * </p>
 *
 * @author lj
 * @date 2026-03-27
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/performance/live-room")
@RequiredArgsConstructor
public class SchedulePerformanceController {

    private final SchedulePerformanceService schedulePerformanceService;
    private final LiveRoomService liveRoomService;

    /**
     * 直播间的业绩分页列表
     * <p>
     * 按直播间维度统计业绩数据
     * 返回直播间的信息和对应今天、昨天、本周、上周、本月、上月的业绩数据
     * </p>
     *
     * @param request    分页与条件请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 直播间分页数据
     */
    @Permissions("room:performance:list")
    @PostMapping("/pageLiveRoomPerformance")
    public ApiResponse<PageData<LiveRoomPerformanceResponse>> pageLiveRoomPerformance(@RequestBody LiveRoomQueryRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        PageData<LiveRoomResponse> result = liveRoomService.pageQueryLiveRoomWithTodayPerformance(request, accessUser.currentTenantId());
        return ApiResponse.success(schedulePerformanceService.LiveRoomPerformanceData(result, accessUser.currentTenantId()));
    }

    /**
     * 详细的排班业绩分页查询
     * <p>
     * 按排班维度统计业绩数据，支持时间范围筛选和主播名称筛选
     * 返回开始时间、结束时间、人员列表、场观、销售额、退款、净销售额、投放、ROI、数据来源
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据
     */
    @Permissions("room:performance:list")
    @PostMapping("/pageQuerySchedule")
    @RequiredLogin
    public ApiResponse<PageData<SchedulePerformanceResponse>> pageQuerySchedulePerformance(
            @Valid @RequestBody SchedulePerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(schedulePerformanceService.pageQuerySchedulePerformance(request));
    }

    /**
     * 按天维度统计排班业绩
     * <p>
     * 以天为维度统计排班业绩数据，支持时间范围筛选、主播名称筛选、直播间ID筛选
     * 返回日期、场次数量、直播时长、人员统计、场观、销售额、退款、净销售额、投放、ROI
     * 支持多字段排序：statsDate, scheduleCount, liveDurationMinutes, viewCount, salesRevenue, refund, netSales, investment, roi
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据
     */
    @Permissions("room:performance:list")
    @PostMapping("/dailyStats")
    @RequiredLogin
    public ApiResponse<PageData<DailyPerformanceStatsResponse>> pageQueryDailyStats(
            @Valid @RequestBody DailyPerformanceStatsRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        return ApiResponse.success(schedulePerformanceService.pageQueryDailyStats(request));
    }

    /**
     * 单个班次业绩详情查询
     * <p>
     * 根据班次业绩ID查询详情，返回业绩数据、人员列表、凭证图片URL、字段修改状态
     * </p>
     *
     * @param id 班次业绩ID
     * @return 班次业绩详情
     */
    @Permissions("room:performance:list")
    @GetMapping("/schedulePerformance/{id}")
    @RequiredLogin
    public ApiResponse<SchedulePerformanceDetailResponse> getDetailById(@PathVariable Long id, AccessUser accessUser) {
        return ApiResponse.success(schedulePerformanceService.getDetailById(id, accessUser.currentTenantId()));
    }

    /**
     * 单个班次业绩保存/修改
     * <p>
     * 新增或修改班次业绩数据：
     * 1. id为空则新增，有值则修改
     * 2. 如果scheduleId有值，则从班次获取时间和直播间ID
     * 3. 如果scheduleId为空，使用前端传入的时间，并检查时间重叠
     * 4. 修改时如果原数据来源是系统，需要记录原始值
     * </p>
     *
     * @param request    保存请求
     * @param accessUser 当前用户
     * @return 业绩ID
     */
    @Permissions({"room:performance:add", "room:performance:update"})
    @PostMapping("/schedulePerformanceSave")
    @RequiredLogin
    public ApiResponse<Long> save(@Valid @RequestBody SchedulePerformanceSaveRequest request, AccessUser accessUser) {
        Long id = schedulePerformanceService.saveSchedulePerformance(request, accessUser.currentTenantId(), accessUser.userId());
        // TODO: 保存后筛选 is_schedule_staff=1 的人员，回写到排班系统。此功能由他人实现。
        return ApiResponse.success(id);
    }

    /**
     * 排班列表查询
     * <p>
     * 查询指定日期的排班列表，并标记每个排班是否已有业绩。
     * 如果传入排班业绩ID，则以该业绩的直播间过滤，并标记当前编辑的排班。
     * </p>
     *
     * @param request    查询请求，date 必填，schedulePerformanceId 选填
     * @param accessUser 当前登录用户
     * @return 排班列表
     */
    @Permissions("room:performance:list")
    @PostMapping("/scheduleList")
    @RequiredLogin
    public ApiResponse<List<ScheduleListItemResponse>> scheduleList(
            @Valid @RequestBody ScheduleListRequest request, AccessUser accessUser) {
        List<ScheduleListItemResponse> list = schedulePerformanceService.queryScheduleList(
                request, accessUser.currentTenantId());
        return ApiResponse.success(list);
    }

    /**
     * 删除班次业绩
     * <p>
     * 逻辑删除班次业绩主表及其关联的明细、人员、图片数据
     * </p>
     *
     * @param id         班次业绩ID
     * @param accessUser 当前登录用户上下文
     * @return 成功响应
     */
    @Permissions("room:performance:delete")
    @PostMapping("/deleteSchedulePerformance")
    @RequiredLogin
    public ApiResponse<Void> deleteSchedulePerformance(@RequestBody IdRequest id, AccessUser accessUser) {
        schedulePerformanceService.deleteSchedulePerformance(id.getId(), accessUser.currentTenantId());
        return ApiResponse.success();
    }

    /**
     * 导出排班业绩数据（班次维度）（Excel）
     * <p>
     * 使用场景：排班业绩列表-导出全部数据
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * 人员信息按"岗位 姓名"格式逗号拼接为单列
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 startTime、endTime、anchorName、liveRoomId（均可选）
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @Permissions("room:performance:list")
    @GetMapping("/export/schedule")
    @RequiredLogin
    public ResponseEntity<byte[]> exportSchedulePerformance(@Valid SchedulePerformancePageRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        PageData<SchedulePerformanceResponse> result = schedulePerformanceService.pageQuerySchedulePerformance(request);
        List<SchedulePerformanceExport> rows = ObjUtil
                .defaultIfNull(result.getList(), Collections.<SchedulePerformanceResponse>emptyList())
                .stream()
                .map(r -> SchedulePerformanceExport.builder()
                        .date(r.getStartTime() != null ? DateUtil.format(r.getStartTime(), "yyyy-MM-dd") : "")
                        .timeSlot(DataUtil.liveTimeSlot(r.getStartTime(), r.getEndTime()))
                        .staffNames(r.getStaffList() == null ? "" : r.getStaffList().stream().map(SchedulePerformanceResponse.StaffInfo::showName).filter(ObjUtil::isNotEmpty).collect(Collectors.joining("\n")))
                        .viewCount(r.getPerformanceData().getViewCount().getValue())
                        .salesRevenue(r.getPerformanceData().getSalesRevenue().getValue())
                        .refund(r.getPerformanceData().getRefund().getValue())
                        .netSales(r.getPerformanceData().getNetSales().getValue())
                        .investment(r.getPerformanceData().getInvestment().getValue())
                        .roi(r.getPerformanceData().getRoi().getValue())
                        .source(DataSource.descByCode(r.getSource()))
                        .build())
                .collect(Collectors.toList());
        return ExcelTemplate.downloadList(rows, "排班业绩数据.xlsx", "排班业绩",
                Collections.emptySet(), SchedulePerformanceExport.class);
    }

    /**
     * 导出排班业绩数据（按天维度）（Excel）
     * <p>
     * 使用场景：按天统计排班业绩列表-导出全部数据
     * 与分页接口参数相同，不传 page/limit，导出全部数据
     * 人员明细不导出，与分页页面一致（打开详情才查看人员）
     * Windows.open调用接口时，可以把token放到参数中，用于登录
     * </p>
     *
     * @param request    查询请求，包含 startTime、endTime、anchorName、liveRoomId、sortField、sortOrder（均可选）
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件字节流
     */
    @Permissions("room:performance:list")
    @GetMapping("/export/daily-stats")
    @RequiredLogin
    public ResponseEntity<byte[]> exportDailyStats(@Valid DailyPerformanceStatsRequest request, AccessUser accessUser) {
        request.setTenantId(accessUser.currentTenantId());
        request.setPage(1);
        request.setLimit(Integer.MAX_VALUE);
        PageData<DailyPerformanceStatsResponse> result = schedulePerformanceService.pageQueryDailyStats(request);
        List<DailyPerformanceStatsExport> rows = ObjUtil
                .defaultIfNull(result.getList(), Collections.<DailyPerformanceStatsResponse>emptyList())
                .stream()
                .map(r -> DailyPerformanceStatsExport.builder()
                        .statsDate(r.getStatsDate() != null ? DateUtil.formatDate(new DateTime(r.getStatsDate())) : "")
                        .schedule(DataUtil.sessionString(r.getScheduleCount(), r.getLiveDurationMinutes()))
                        .staffNames(r.getStaffList() == null ? "" : r.getStaffList().stream().map(StaffDailyStats::showName).filter(ObjUtil::isNotEmpty).collect(Collectors.joining("\n")))
                        .viewCount(r.getViewCount())
                        .salesRevenue(r.getSalesRevenue())
                        .refund(r.getRefund())
                        .netSales(r.getNetSales())
                        .investment(r.getInvestment())
                        .roi(r.getRoi())
                        .build())
                .collect(Collectors.toList());
        return ExcelTemplate.downloadList(rows, "按天排班业绩数据.xlsx", "按天统计",
                Collections.emptySet(), DailyPerformanceStatsExport.class);
    }
}
