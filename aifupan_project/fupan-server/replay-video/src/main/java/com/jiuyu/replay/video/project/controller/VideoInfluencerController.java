package com.jiuyu.replay.video.project.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSort;
import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerInfoBo;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerSearchBo;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerSubscribeBo;
import com.jiuyu.replay.video.project.bo.subscription.InfluencerSubscriptionEditBo;
import com.jiuyu.replay.video.project.bo.subscription.InfluencerSubscriptionQueryBo;
import com.jiuyu.replay.video.project.entity.VideoUserInfluencerSubscriptionEntity;
import com.jiuyu.replay.video.project.producer.VideoInfluencerProducer;
import com.jiuyu.replay.video.project.vo.admin.AdminInfluencerSubscriptionVo;
import com.jiuyu.replay.video.project.vo.influencer.VideoInfluencerInfoDetailVo;
import com.jiuyu.replay.video.project.vo.influencer.VideoInfluencerSearchHistoryVo;
import com.jiuyu.replay.video.project.vo.subscription.InfluencerSubscriptionGroupVo;
import com.jiuyu.replay.video.project.vo.subscription.VideoInfluencerSubscriptionVo;
import com.jiuyu.replay.video.project.vo.video.VideoInfluencerInfoDetailVideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 达人管理控制器
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 达人相关接口控制器，提供达人搜索、订阅等功能
 */
@Slf4j
@RestController
@RequestMapping("/replay/video/influencer")
@RequiredArgsConstructor
@ApiSort(value = 1)
@Tag(name = "V2.5.3短视频/达人管理模块", description = "达人搜索、订阅相关接口")
@Validated
public class VideoInfluencerController {

    private final VideoInfluencerProducer videoInfluencerProducer;
    private final UserFeign userFeign;

    @PostMapping("/saveSearch")
    @Operation(summary = "客户端-保存搜索达人数据", description = "根据关键词搜索达人信息保存")
    public R<Long> saveSearchInfluencers(@Validated @RequestBody VideoInfluencerSearchBo searchBO) {
        return R.ok(videoInfluencerProducer.saveSearchInfluencers(searchBO));
    }

    @GetMapping("/search")
    @Operation(summary = "WEB端-搜索达人数据", description = "根据快照id搜索达人信息")
    public R<PageUtils<VideoInfluencerSearchHistoryVo>> searchInfluencers(@RequestParam Long snapshotId, @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page, @RequestParam(defaultValue = "10") @Max(value = 100, message = "每页最大条数不能超过100条") Integer limit) {
        return R.ok(videoInfluencerProducer.searchInfluencers(snapshotId, page, limit));
    }

    @GetMapping("/history")
    @Operation(summary = "WEB端-搜索达人历史记录", description = "搜索达人历史记录")
    public R<PageUtils<VideoInfluencerSearchHistoryVo>> queryHistoryRecord(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page, @RequestParam(defaultValue = "10") @Max(value = 100, message = "每页最大条数不能超过100条") Integer limit) {
        return R.ok(videoInfluencerProducer.queryHistoryRecord(page, limit));
    }

    @GetMapping("/getInfluencerLastSyncTime")
    @Operation(summary = "客户端-获取达人最后同步时间", description = "获取达人最后同步时间")
    public R<LocalDateTime> getInfluencerLastSyncTime(@RequestParam @NotNull(message = "平台类型不能为空") @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法") Byte platformType, @RequestParam @NotBlank(message = "平台ID不能为空") @Length(max = 100, message = "平台ID不合法") String platformUserId) {
        return R.ok(videoInfluencerProducer.getInfluencerLastSyncTime(platformType, platformUserId));
    }

    @GetMapping("/hasInfluencerSyncVideos")
    @Operation(summary = "客户端-判断达人是否需要同步视频", description = "获取true或者false")
    public R<Boolean> hasInfluencerSyncVideos(@RequestParam @NotNull(message = "平台类型不能为空") @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法") Byte platformType, @RequestParam @NotBlank(message = "平台ID不能为空") @Length(max = 100, message = "平台ID不合法") String platformUserId) {
        return R.ok(videoInfluencerProducer.hasInfluencerSyncVideos(platformType, platformUserId));
    }

    @PutMapping("/syncInfluencerInfo")
    @Operation(summary = "客户端-同步达人数据（包含短视频信息）", description = "同步达人数据（包含短视频信息）")
    public R<Boolean> syncInfluencerInfo(@RequestBody @Validated(VideoInfluencerInfoBo.Add.class) VideoInfluencerInfoBo videoInfluencerInfoBo) {
        return R.ok(videoInfluencerProducer.syncInfluencerInfo(videoInfluencerInfoBo));
    }

    @GetMapping("/detail")
    @Operation(summary = "WEB端-获取达人详情", description = "获取达人详情")
    public R<VideoInfluencerInfoDetailVo> getInfluencerDetail(
            @RequestParam @NotNull(message = "平台类型不能为空") @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法") Byte platformType,
            @RequestParam @NotBlank(message = "平台ID不能为空") @Length(max = 100, message = "平台ID不合法") String platformUserId
    ) {
        return R.ok(videoInfluencerProducer.getInfluencerDetail(platformType, platformUserId));
    }

    @GetMapping("/detailVideos")
    @Operation(summary = "WEB端-获取达人详情视频", description = "获取达人详情视频")
    public R<PageUtils<VideoInfluencerInfoDetailVideoVo>> getDetailVideos(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(defaultValue = "10") @Max(value = 100, message = "每页最大条数不能超过100条") Integer limit,
            @RequestParam @NotNull(message = "平台类型不能为空") @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法") Byte platformType,
            @RequestParam @NotBlank(message = "平台ID不能为空") @Length(max = 100, message = "平台ID不合法") String platformUserId,
            @RequestParam(defaultValue = "1") @EnumValue(byteValues = {1, 2, 3, 4, 5, 6}, message = "排序类型标识不合法")
            @Parameter(name = "sortCode", description = "排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序 6：评赞比排序", in = ParameterIn.QUERY)
            Byte sortCode,
            @RequestParam(defaultValue = "0") @EnumValue(byteValues = {0, 1}, message = "排序顺序不合法")
            @Parameter(name = "sortSequence", description = "排序顺序 0：降序 1：升序", in = ParameterIn.QUERY)
            Byte sortSequence,
            @Parameter(name = "videoPublishTimeType", description = "视频发布时间类型 1：今日发布 2：三日发布", in = ParameterIn.QUERY)
            @RequestParam(required = false) @EnumValue(byteValues = {1, 2}, message = "视频发布时间类型不合法") Byte videoPublishTimeType) {
        return R.ok(videoInfluencerProducer.getInfluencerDetailVideos(page, limit, platformType, platformUserId, sortCode, sortSequence, videoPublishTimeType));
    }


    // ==================== 达人订阅管理接口 ====================

    @PostMapping("/subscribe")
    @Operation(summary = "WEB端-添加达人订阅", description = "添加达人订阅")
    public R<VideoUserInfluencerSubscriptionEntity> subscribe(@RequestBody @Validated(VideoInfluencerSubscribeBo.Add.class) VideoInfluencerSubscribeBo videoInfluencerSubscribeBo) {
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        return R.ok(videoInfluencerProducer.subscribe(videoInfluencerSubscribeBo, userCacheVo, tenantId));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "WEB端-查询达人订阅列表", description = "查询达人订阅列表，按分组展示，支持按昵称、行业筛选")
    public R<List<InfluencerSubscriptionGroupVo>> getInfluencerSubscriptions(
            @RequestParam(required = false) @Length(max = 50, message = "达人昵称长度不能超过50个字符") String nickname,
            @RequestParam(required = false) Long industryId) {
        InfluencerSubscriptionQueryBo queryBo = InfluencerSubscriptionQueryBo.builder()
                .nickname(nickname)
                .industryId(industryId)
                .build();
        return R.ok(videoInfluencerProducer.getInfluencerSubscriptions(queryBo));
    }

    @PutMapping("/subscription/edit")
    @Operation(summary = "WEB端-编辑达人订阅", description = "编辑达人订阅信息，包括行业、分组、自动同步设置等")
    public R<Boolean> editInfluencerSubscription(@RequestBody @Validated InfluencerSubscriptionEditBo editBo) {
        return R.ok(videoInfluencerProducer.editInfluencerSubscription(editBo));
    }

    @DeleteMapping("/subscription/{subscriptionId}")
    @Operation(summary = "WEB端-删除达人订阅", description = "删除指定的达人订阅，确认删除后将停止同步该达人的新内容")
    public R<Boolean> deleteInfluencerSubscription(@PathVariable @NotNull(message = "订阅ID不能为空") @Positive(message = "订阅ID必须为正数") Long subscriptionId) {
        return R.ok(videoInfluencerProducer.deleteInfluencerSubscription(subscriptionId));
    }

    @GetMapping("/subscription/list")
    @Operation(summary = "客户端-获取当前用户订阅达人列表", description = "获取当前用户订阅达人列表")
    public R<List<VideoInfluencerSubscriptionVo>> getVideoInfluencerSubscriptionList(Long influencerId) {
        return R.ok(videoInfluencerProducer.getVideoInfluencerSubscriptionList(influencerId));
    }

    @GetMapping("/admin/subscriptions/{userId}")
    @Operation(summary = "后台管理-根据用户ID查询达人订阅信息", description = "查询用户的达人订阅信息，包括达人昵称、抖音ID、短视频数量、订阅时间")
    public R<List<AdminInfluencerSubscriptionVo>> getInfluencerSubscriptionsByUserId(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        return R.ok(videoInfluencerProducer.getInfluencerSubscriptionsByUserId(userId));
    }
}
