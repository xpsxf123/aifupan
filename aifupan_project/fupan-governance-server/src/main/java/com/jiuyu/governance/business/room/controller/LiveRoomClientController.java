package com.jiuyu.governance.business.room.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.request.ClientLiveRoomAddRequest;
import com.jiuyu.governance.business.room.pojo.request.schedule.AnchorQueryScheduleRequest;
import com.jiuyu.governance.business.room.pojo.response.ClientFuturePlanScheduleResponse;
import com.jiuyu.governance.business.room.pojo.response.ClientLiveRoomSchedulesResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleAlignResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.plugins.oauth.ClientUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 客户端 - 直播间相关API
 *
 * @author HeHui
 * @date 2026-03-24 15:01
 */
@RestController
@RequestMapping("/api/governance/client/live-room")
@ClientUser
@Slf4j
public class LiveRoomClientController {

    private final LiveRoomScheduleService roomScheduleService;

    private final LiveRoomService liveRoomService;

    public LiveRoomClientController(LiveRoomScheduleService roomScheduleService, LiveRoomService liveRoomService) {
        this.roomScheduleService = roomScheduleService;
        this.liveRoomService = liveRoomService;
    }


    /**
     * 获取未来排班计划
     *
     * @return ApiResponse包含排班计划列表
     */
    @GetMapping("/future-plan")
    public ApiResponse<List<ClientFuturePlanScheduleResponse>> futurePlan(AccessUser accessUser) {
        return ApiResponse.success(roomScheduleService.getLiveRoomFuturePlanSchedules(LocalDate.now().plusDays(35), accessUser.currentTenantId()));
    }

    /**
     * 获取直播间排班计划
     *
     * @param day 排班计划日期 yyyy-MM-dd or yyyy/MM/dd
     *
     * @return ApiResponse包含排班计划列表
     */
    @GetMapping("/schedules")
    public ApiResponse<List<ClientLiveRoomSchedulesResponse>> getLiveRoomSchedules(LocalDate day, AccessUser accessUser) {
        return ApiResponse.success(roomScheduleService.getLiveRoomSchedules(day, accessUser.currentTenantId()));
    }

    /**
     * 获取主播排班计划
     *
     * @param request 查询参数
     *
     * @return ApiResponse包含排班计划列表
     */
    @PostMapping("/plan")
    public ApiResponse<List<LiveRoomSchedulePageResponse>> query(@RequestBody @Validated AnchorQueryScheduleRequest request, AccessUser accessUser) {
        LivePlatformType platformType = LivePlatformType.getByValue(request.getLivePlatformType());
        if (platformType == null) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "直播平台类型错误");
        }
        return ApiResponse.success(roomScheduleService.queryAnchorRoomSchedule(platformType, request.getSecUid(), request.getStartTime(), request.getEndTime(), accessUser.currentTenantId()));
    }

    /**
     * 复盘列表 - 批量查询直播间排班
     *
     * @param requests 批量查询参数
     *
     * @return ApiResponse包含排班计划列表
     */
    @PostMapping("/batch-plan")
    public ApiResponse<List<LiveRoomScheduleAlignResponse>> batchQuery(@RequestBody @Validated List<AnchorQueryScheduleRequest> requests, AccessUser accessUser) {
        if (requests.size() > 100) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "批量查询最多支持100条");
        }
        if (requests.stream().filter(r -> r.getStartTime() != null && r.getEndTime() != null).anyMatch(r -> r.getStartTime().isAfter(r.getEndTime()))) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "开始时间不能大于结束时间");
        }
//        if (requests.stream().filter(r -> r.getStartTime() != null && r.getEndTime() != null).anyMatch(r -> ChronoUnit.DAYS.between(r.getStartTime().toLocalDate(), r.getEndTime().toLocalDate()) > 2)) {
//            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "批量查询时间范围不能超过3天");
//        }
        return ApiResponse.success(roomScheduleService.batchQueryAnchorRoomSchedule(requests, accessUser.currentTenantId()));
    }


    /**
     * 添加主播
     *
     * @param request 添加主播参数
     *
     * @return ApiResponse
     */
    @PostMapping("/anchor")
    public ApiResponse<Void> addAnchor(@RequestBody @Validated ClientLiveRoomAddRequest request, AccessUser accessUser) {
        return liveRoomService.addAnchor(request, accessUser.currentTenantId());
    }
}
