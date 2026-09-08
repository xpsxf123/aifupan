package com.jiuyu.governance.business.room.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.room.pojo.request.schedule.*;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleResponse;
import com.jiuyu.governance.business.room.handler.LiveRoomScheduleRangeMergeHandler;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleManageService;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 企业端 - 直播间排班API
 *
 * @author HeHui
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/api/governance/room-schedule")
@GovernanceUser
@RequiredArgsConstructor
@Slf4j
public class LiveRoomScheduleAdminController {

    private final LiveRoomScheduleManageService scheduleManageService;

    /**
     * 新增直播间排班
     *
     * @param request    新增请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:add")
    @ResourceLock(prefix = "governance:live-room-schedule", key = "#request.liveRoomId", message = "正在添加排班,请勿频繁点击")
    @PostMapping("/add")
    public ApiResponse<Void> addSchedule(@RequestBody @Validated LiveRoomScheduleAddRequest request, AccessUser accessUser) {
        // 验证排班日期不能早于当前日期
        LocalDate workDay = request.getWorkDay();
        LocalDate now = LocalDate.now();
        if (workDay.isBefore(now)) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "排班日期["+ LocalDateTimeUtil.formatNormal(request.getWorkDay()) +"]比当天[" + LocalDateTimeUtil.formatNormal(now) + "]还早,请重新选择排班日期");
        }

        // 获取最早的班次开始时间并验证至少有一个班次
        Optional<LocalTime> timeOptional = request.getSessions().stream().map(ScheduleSessionRequest::getStartWork).min(LocalTime::compareTo);
        if (timeOptional.isEmpty()) {
            // 不可能会出现,但是为了代码洁癖(无黄色警告)还是选择判断
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "请添加至少一个班次");
        }

        // 验证当天排班时间必须在当前时间一小时后(或当前已经是23点了)
        LocalTime time = timeOptional.get();
        LocalTime nowTime = LocalTime.now();
        if (now.equals(workDay) && (nowTime.getHour() == 23 || time.isBefore(nowTime.plusHours(1)))) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "排班时间["+ LocalDateTimeUtil.formatNormal(request.getWorkDay()) + " " + time.getHour() + ":" + (time.getMinute() < 10 ? "0" + time.getMinute() : time.getMinute()) + "]马上就要开始啦,仅支持添加当前时间一个小时后的排班");
        }

        // 调用服务层执行排班新增操作
        return scheduleManageService.addSchedule(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 批量导入直播间排班（覆盖式）
     *
     * @param request    导入请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:add")
    @ResourceLock(prefix = "governance:live-room-schedule", key = "#request.liveRoomId", message = "正在导入排班，请勿频繁点击")
    @PostMapping("/import")
    public ApiResponse<Void> importSchedule(@RequestBody @Validated LiveRoomScheduleImportRequest request, AccessUser accessUser) {
        return scheduleManageService.importSchedule(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 查询指定时间范围排班数据
     *
     * @param request    查询请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @PostMapping("/list-range")
    public ApiResponse<List<LiveRoomScheduleResponse>> listRange(@RequestBody @Validated LiveRoomScheduleQueryRequest request, AccessUser accessUser) {
        return ApiResponse.success(scheduleManageService.listRange(request, accessUser.currentTenantId()));
    }


    /**
     * 查询指定排班详情
     *
     * @param roomScheduleId 排班ID
     *
     * @return 响应结果
     */
    @GetMapping("/detail")
    public ApiResponse<LiveRoomScheduleResponse> detail(@RequestParam Long roomScheduleId, AccessUser accessUser) {
        LiveRoomScheduleResponse responses = scheduleManageService.getAdminDetail(roomScheduleId, accessUser.currentTenantId());
        if (EmptyUtil.isEmpty(responses)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "排班不存在");
        }
        return ApiResponse.success(responses);
    }

    /**
     * 查询指定时间范围排班数据 - 拆分每天
     *
     * @param request    查询请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @PostMapping("/list-range-spit-day")
    public ApiResponse<List<LiveRoomScheduleResponse>> listRangeSpitDay(@RequestBody @Validated LiveRoomScheduleQueryRequest request, AccessUser accessUser) {
        List<LiveRoomScheduleResponse> list = scheduleManageService.listRange(request, accessUser.currentTenantId());
        return ApiResponse.success(LiveRoomScheduleRangeMergeHandler.splitByDay(list));
    }

    /**
     * 分页查询排班数据
     *  必填直播间ID
     * @param request    分页查询请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:list")
    @PostMapping("/list")
    public ApiResponse<PageData<LiveRoomSchedulePageResponse>> page(@RequestBody @Validated LiveRoomSchedulePageRequest request, AccessUser accessUser) {
        if (EmptyUtil.isEmpty(request.getLiveRoomId())) {
            return ApiResponse.success(PageData.empty());
        }
        return ApiResponse.success(scheduleManageService.page(request, accessUser.currentTenantId()));
    }

    /**
     * 添加排班人员
     *
     * @param request    添加人员请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:update")
    @ResourceLock(prefix = "governance:live-room-schedule", key = "#request.liveRoomId", message = "正在添加排班，请勿频繁点击")
    @PostMapping("/add-employee")
    public ApiResponse<Void> addEmployee(@RequestBody @Validated ScheduleAddEmployeeRequest request, AccessUser accessUser) {
        return scheduleManageService.addEmployee(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 移除排班人员
     *
     * @param request    移除人员请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:update")
    @ResourceLock(prefix = "governance:live-room-schedule", key = "#request.liveRoomId", message = "正在添加排班，请勿频繁点击")
    @PostMapping("/remove-employee")
    public ApiResponse<Void> removeEmployee(@RequestBody @Validated ScheduleRemoveEmployeeRequest request, AccessUser accessUser) {
        return scheduleManageService.removeEmployee(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 批量更换排班人员
     *
     * @param request    更换人员请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:update")
    @PostMapping("/replace-employee")
    public ApiResponse<Void> replaceEmployees(@RequestBody @Validated ScheduleReplaceEmployeeRequest request, AccessUser accessUser) {
        return scheduleManageService.replaceEmployees(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 修改排班时间
     *
     * @param request    修改请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:update")
    @ResourceLock(prefix = "governance:live-room-schedule", key = "#request.liveRoomId", message = "正在修改排班，请勿频繁点击")
    @PostMapping("/update")
    public ApiResponse<Void> updateSchedule(@RequestBody @Validated ScheduleUpdateRequest request, AccessUser accessUser) {
        return scheduleManageService.updateSchedule(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 删除排班
     *
     * @param request    删除请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:schedule:delete")
    @ResourceLock(prefix = "governance:live-room-schedule", key = "#request.liveRoomId", message = "正在删除排班，请勿频繁点击")
    @PostMapping("/delete")
    public ApiResponse<Void> deleteSchedule(@RequestBody @Validated DeleteScheduleRequest request, AccessUser accessUser) {
        return scheduleManageService.deleteSchedule(request.getId(), accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 下载直播间排班导入模板（矩阵式 .xlsx）
     *
     * @param liveRoomId 直播间ID
     * @param accessUser 当前登录用户上下文
     * @return Excel 文件流
     */
    @Permissions("room:schedule:list")
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> exportTemplate(@RequestParam Long liveRoomId, AccessUser accessUser) {
        byte[] bytes = scheduleManageService.exportTemplate(liveRoomId, accessUser.currentTenantId());
        HttpHeaders headers = new HttpHeaders();
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(URLEncoder.encode("直播间排班导入模板.xlsx", StandardCharsets.UTF_8))
            .build();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
