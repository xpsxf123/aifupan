package com.jiuyu.governance.business.performance.controller;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.APIKey;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.performance.pojo.request.ClientPushVideoListRequest;
import com.jiuyu.governance.business.performance.pojo.request.ClientPushVideoRequest;
import com.jiuyu.governance.business.performance.pojo.request.VideoProductPageRequest;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.openfeign.replay.AnchorInfoService;
import com.jiuyu.governance.openfeign.replay.response.AnchorVideoInfoResponse;
import io.micrometer.common.util.StringUtils;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.business.performance.pojo.request.ClientSchedulePerformanceQueryRequest;
import com.jiuyu.governance.business.performance.pojo.response.client.ClientSchedulePerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.client.VideoProductPageResponse;
import com.jiuyu.governance.business.performance.service.LiveVideoService;
import com.jiuyu.governance.business.performance.service.SchedulePerformanceService;
import com.jiuyu.governance.business.performance.service.VideoProductService;
import com.jiuyu.governance.plugins.oauth.ClientUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端视频数据接口
 *
 * @author lj
 * @date 2026-03-19
 */
@Slf4j
@RestController
@RequestMapping("/api/governance/performance/video")
@RequiredArgsConstructor
public class ClientVideoController {

    private final LiveVideoService liveVideoService;
    private final VideoProductService videoProductService;
    private final SchedulePerformanceService schedulePerformanceService;
    private final AnchorInfoService anchorInfoService;

    private final LiveRoomService liveRoomService;

    /**
     * 客户端推送直播业绩数据
     *
     * @param request 请求数据
     * @return 响应结果
     */
    @ClientUser
    @ResourceLock(prefix = "governance:live-video-push", key = "#request.batchNumber + '_' + #accessUser.currentTenantId",
            message = "正在处理视频数据，请勿频繁点击", releaseLock = true, tryTimeout = 5)
    @PostMapping("/clientPushVideo")
    public ApiResponse<Void> clientPushVideo(@Valid @RequestBody ClientPushVideoRequest request, AccessUser accessUser) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "开始时间不能大于结束时间");
        }
        request.setTenantId(accessUser.currentTenantId());
        liveVideoService.clientPushVideo(request);
        return ApiResponse.success();
    }

    /**
     * 客户端批量推送直播业绩数据（复制到多个租户）
     * <p>
     * 将同一份视频业绩数据复制到多个目标租户中。使用 secUid + batchNumber 作为分布式锁维度，
     * 循环逐租户调用 {@link #clientPushVideo} 的核心逻辑，每个租户独立事务，互不影响。
     *
     * @param request 批量推送请求（含多个目标租户ID）
     * @return 响应结果
     */
    @APIKey
    @ResourceLock(prefix = "governance:live-video-push-list", key = "#request.secUid + '_' + #request.batchNumber",
            message = "正在处理视频数据，请勿频繁点击", releaseLock = true, tryTimeout = 5)
    @PostMapping("/clientPushVideoList")
    public ApiResponse<Void> clientPushVideoList(@Valid @RequestBody ClientPushVideoListRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "开始时间不能大于结束时间");
        }
        List<Long> tenantIds = liveRoomService.getRoomTenantMap(LivePlatformType.SHI_PING_HAO, List.of(request.getSecUid())).get(request.getSecUid());
        if (EmptyUtil.isEmpty(tenantIds)) {
            log.warn("[业绩] 批量推送视频失败, 未找到对应的租户, secUid={}, batchNumber={}", request.getSecUid(), request.getBatchNumber());
            return ApiResponse.success();
        }
        log.info("[业绩] 批量推送视频开始, secUid={}, batchNumber={}, tenantIds={}",
                request.getSecUid(), request.getBatchNumber(), tenantIds);

        for (Long tenantId : tenantIds) {
            ClientPushVideoRequest singleRequest = new ClientPushVideoRequest();
            BeanUtil.copyProperties(request, singleRequest);
            singleRequest.setTenantId(tenantId);
            liveVideoService.clientPushVideo(singleRequest);
        }

        log.info("[业绩] 批量推送视频完成, secUid={}, batchNumber={}, 租户数={}",
                request.getSecUid(), request.getBatchNumber(), tenantIds.size());
        return ApiResponse.success();
    }

    /**
     * 分页查询视频商品列表
     * <p>
     * 先查 live_video.batch_number 判断是否已拉取，已拉取才查 video_product。
     *
     * @param request    分页查询请求（batchNumber、分页参数等）
     * @param accessUser 当前登录用户
     * @return 分页商品数据（含 pullStatus）
     */
    @ClientUser
    @PostMapping("/productPage")
    public ApiResponse<VideoProductPageResponse> productPage(
            @Valid @RequestBody VideoProductPageRequest request,
            AccessUser accessUser) {
        // videoId 有值时，从爱复盘视频表获取真实 tenantId
        Long tenantId;
        if (StringUtils.isNotBlank(request.getVideoId())) {
            AnchorVideoInfoResponse videoInfo = anchorInfoService.getVideoByVideoId(request.getVideoId());
            if (videoInfo == null || videoInfo.getTenantId() == null) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "视频不存在或已删除");
            }
            tenantId = videoInfo.getTenantId();
        } else {
            tenantId = accessUser.currentTenantId();
        }
        String batchNumber = request.getBatchNumber();

        // 先查 live_video，判断 batch_number 是否有值 → 是否已拉取
        if (!liveVideoService.hasBatchNumber(tenantId, batchNumber)) {
            return ApiResponse.success(new VideoProductPageResponse(
                    List.of(), 0L, request.getLimit(), request.getPage(), 0L, false));
        }

        request.setTenantId(tenantId);
        return ApiResponse.success(videoProductService.pageByBatchNumber(request));
    }

    @APIKey
    @PostMapping("/productPageApi")
    public ApiResponse<VideoProductPageResponse> productPage(
            @Valid @RequestBody VideoProductPageRequest request) {
        return ApiResponse.success(videoProductService.pageByBatchNumber(request));
    }

    /**
     * 客户端查询排班业绩数据
     * <p>
     * 根据主播secUid和视频时间范围查询对应的排班业绩记录（最多返回1条）
     *
     * @param request    查询请求（secUid、startTime、endTime）
     * @param accessUser 当前登录用户
     * @return 排班业绩数据，无匹配时返回null
     */
    @ClientUser
    @PostMapping("/querySchedulePerformance")
    public ApiResponse<ClientSchedulePerformanceResponse> querySchedulePerformance(
            @Valid @RequestBody ClientSchedulePerformanceQueryRequest request, AccessUser accessUser) {
        ClientSchedulePerformanceResponse result = schedulePerformanceService.queryBySecUidAndTimeRange(
                accessUser.currentTenantId(), request.getSecUid(), request.getStartTime(), request.getEndTime());
        return ApiResponse.success(result);
    }
}
