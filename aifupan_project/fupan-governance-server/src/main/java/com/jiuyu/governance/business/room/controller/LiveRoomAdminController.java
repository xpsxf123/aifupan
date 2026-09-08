package com.jiuyu.governance.business.room.controller;

import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.Permissions;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.room.pojo.bo.RoomSchedulesRawBo;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomAddRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomScheduleAttributeSetRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSyncTenantAnchorsRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomScheduleAttributeResponse;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSearchQueryRequest;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomUpdateRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomSyncResultResponse;
import com.jiuyu.governance.business.performance.pojo.response.LiveRoomPerformanceResponse;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.bo.IdRequest;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业端 - 直播间API
 *
 * @author HeHui
 * @date 2026-03-25
 */
@RestController
@RequestMapping("/api/governance/live-room")
@GovernanceUser
@RequiredArgsConstructor
@Slf4j
public class LiveRoomAdminController {

    private final LiveRoomService liveRoomService;

    private final LiveRoomScheduleService roomScheduleService;

    /**
     * 新增直播间
     * <p>
     * 依赖第三方 API 获取主播公开信息，存在网络延迟，请勿频繁点击。
     * </p>
     *
     * @param request    新增请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:mgmt:add")
    @ResourceLock(prefix = "governance:live-room", key = "#request.anchorNumber", message = "正在添加直播间，请勿频繁点击")
    @PostMapping("/add")
    public ApiResponse<Void> addLiveRoom(@RequestBody @Validated LiveRoomAddRequest request, AccessUser accessUser) {
        return liveRoomService.addLiveRoom(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 修改直播间
     *
     * @param request    修改请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions({"room:mgmt:update", "room:mgmt:add"})
    @ResourceLock(prefix = "governance:live-room", key = "#request.id", message = "正在修改直播间，请勿频繁点击")
    @PostMapping("/update")
    public ApiResponse<Void> updateLiveRoom(@RequestBody @Validated LiveRoomUpdateRequest request, AccessUser accessUser) {
        return liveRoomService.updateLiveRoom(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 删除直播间
     *
     * @param request    包含 ID 的请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions("room:mgmt:delete")
    @ResourceLock(prefix = "governance:live-room", key = "#request.id", message = "正在删除直播间，请勿频繁点击")
    @PostMapping("/delete")
    public ApiResponse<Void> deleteLiveRoom(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return liveRoomService.deleteLiveRoom(request.getId(), accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 启用直播间
     *
     * @param request    包含 ID 的请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions({"room:mgmt:update", "room:mgmt:add"})
    @ResourceLock(prefix = "governance:live-room", key = "#request.id", message = "正在操作直播间，请勿频繁点击")
    @PostMapping("/enable")
    public ApiResponse<Void> enableLiveRoom(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return liveRoomService.enableLiveRoom(request.getId(), accessUser.currentTenantId(), accessUser.userId(), AccountStatus.NORMAL);
    }

    /**
     * 停用直播间
     *
     * @param request    包含 ID 的请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 响应结果
     */
    @Permissions({"room:mgmt:update", "room:mgmt:add"})
    @ResourceLock(prefix = "governance:live-room", key = "#request.id", message = "正在操作直播间，请勿频繁点击")
    @PostMapping("/disable")
    public ApiResponse<Void> disableLiveRoom(@RequestBody @Validated IdRequest request, AccessUser accessUser) {
        return liveRoomService.enableLiveRoom(request.getId(), accessUser.currentTenantId(), accessUser.userId(), AccountStatus.DISABLED);
    }

    /**
     * 同步租户主播到直播间
     * <p>
     * 远端来源：AnchorInfoService#getTenantAnchors。单租户范围内按 platform + secUid 对账，
     * 不存在则新增，存在则更新，并返回同步统计结果。
     * </p>
     *
     * @param request    同步请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 同步统计结果
     */
    @Permissions("room:mgmt:add")
    @ResourceLock(prefix = "governance:live-room:sync-tenant-anchors", key = "#accessUser.currentTenantId()",
        message = "正在同步直播间，请勿频繁点击")
    @PostMapping("/sync-tenant-anchors")
    public ApiResponse<LiveRoomSyncResultResponse> syncTenantAnchors(@RequestBody
        @Validated LiveRoomSyncTenantAnchorsRequest request, AccessUser accessUser) {
        return liveRoomService.syncTenantAnchors(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 直播间分页列表
     * 特别注意：当需要返回今天和明日排班数据时，请务必在请求参数中添加 loadSchedule = true
     * @param request    分页与条件请求参数
     * @param accessUser 当前登录用户上下文
     *
     * @return 直播间分页数据
     */
    @PostMapping("/page")
    public ApiResponse<PageData<LiveRoomResponse>> pageQueryLiveRoom(@RequestBody LiveRoomQueryRequest request, AccessUser accessUser) {
        // 执行直播间分页查询
        PageData<LiveRoomResponse> pageData = liveRoomService.pageQueryLiveRoom(request, accessUser.currentTenantId());

        // 当需要加载排班数据时，批量查询今明两天的排班信息并填充到结果中
        if (EmptyUtil.isNotEmpty(pageData.getList()) && Boolean.TRUE.equals(request.getLoadSchedule())) {
            List<Long> roomIds = pageData.getList().stream().map(LiveRoomResponse::getId).toList();
            LocalDate start = LocalDate.now();
            LocalDate tomorrow = start.plusDays(1);

            // 获取直播间今明两天的排班
            Map<Long, List<RoomSchedulesRawBo>> roomSchedulesMap = roomScheduleService.getLiveRoomSchedules(request.getTenantId(), roomIds, new Range<>(start, start.plusDays(2))).stream().collect(Collectors.groupingBy(RoomSchedulesRawBo::getRoomId));

            // 将排班数据按日期分组后设置到对应的直播间响应对象中
            pageData.getList().forEach(room -> {
                List<RoomSchedulesRawBo> roomSchedulesRawBos = roomSchedulesMap.get(room.getId());
                if (EmptyUtil.isEmpty(roomSchedulesRawBos)) {
                    return;
                }
                room.setThatDaySchedules(roomSchedulesRawBos.stream().filter(schedule -> schedule.getWorkDay().equals(start)).toList());
                room.setTomorrowSchedules(roomSchedulesRawBos.stream().filter(schedule -> schedule.getWorkDay().equals(tomorrow)).toList());
            });
        }
        return pageData.toResult();
    }

    /**
     * 获取指定直播间的详细信息
     *
     * @param id         直播间主键ID
     * @param accessUser 当前登录用户上下文
     *
     * @return 直播间详情数据
     */
    //@Permissions("room:mgmt:list")
    @GetMapping("/detail")
    public ApiResponse<LiveRoomResponse> getLiveRoomDetail(@RequestParam Long id, AccessUser accessUser) {
        return ApiResponse.success(liveRoomService.getLiveRoomDetail(id, accessUser.currentTenantId()));
    }


    /**
     * 获取直播间下拉选项
     *
     * @param request 搜索请求参数
     *
     * @return 直播间下拉选项
     */
    @GetMapping("/options")
    public ApiResponse<List<LabelOption>> options(LiveRoomSearchQueryRequest request) {
        return ApiResponse.success(liveRoomService.options(request));
    }


    /**
     * 设置直播间排班配置
     *
     * @param request    设置请求参数
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Permissions({"room:mgmt:update", "room:mgmt:add"})
    @PostMapping("/schedule-attribute")
    public ApiResponse<Void> setScheduleAttribute(@RequestBody @Validated LiveRoomScheduleAttributeSetRequest request, AccessUser accessUser) {
        return liveRoomService.setScheduleAttribute(request, accessUser.currentTenantId(), accessUser.userId());
    }

    /**
     * 获取直播间排班配置
     *
     * @param id       直播间主键ID
     * @param accessUser 当前登录用户
     *
     * @return {@link LiveRoomScheduleAttributeResponse }
     */
    @GetMapping("/schedule-attribute")
    public ApiResponse<LiveRoomScheduleAttributeResponse> getScheduleAttribute(@RequestParam Long id, AccessUser accessUser) {
        return ApiResponse.success(liveRoomService.getScheduleAttribute(id, accessUser.currentTenantId()));
    }
}
