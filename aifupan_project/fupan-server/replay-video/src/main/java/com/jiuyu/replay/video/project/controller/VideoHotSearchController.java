package com.jiuyu.replay.video.project.controller;

import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.video.project.bo.hotsearch.*;
import com.jiuyu.replay.video.project.entity.VideoUserHotSubscriptionEntity;
import com.jiuyu.replay.video.project.producer.VideoHotSearchProducer;
import com.jiuyu.replay.video.project.vo.admin.AdminHotSubscriptionVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchHistoryVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchVideoInfoVo;
import com.jiuyu.replay.video.project.vo.subscription.VideoUserHotSubscriptionAllVo;
import com.jiuyu.replay.video.project.vo.subscription.VideoUserHotSubscriptionListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 爆款搜索控制器
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 爆款搜索相关接口控制器，提供搜索、订阅、分组管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/replay/video/hotSearch")
@RequiredArgsConstructor
@Tag(name = "V2.5.3短视频/爆款管理模块", description = "爆款相关接口")
public class VideoHotSearchController {

    private final VideoHotSearchProducer videoHotSearchProducer;
    private final UserFeign userFeign;

    @PostMapping("/saveSearch")
    @Operation(summary = "客户端-保存搜索爆款数据", description = "根据关键词搜索爆款视频信息保存")
    public R<Long> saveSearchHotVideos(@Validated @RequestBody VideoHotSearchBo searchBO) {
        return R.ok(videoHotSearchProducer.saveSearchHotVideos(searchBO));
    }

    @GetMapping("/search")
    @Operation(summary = "WEB端-根据搜索id搜索爆款信息", description = "根据快照id搜索爆款视频信息")
    public R<PageUtils<VideoHotSearchResultVo>> searchHotVideos(@RequestParam Long snapshotId,
                                                                @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
                                                                @RequestParam(defaultValue = "10") @Max(value = 100, message = "每页最大条数不能超过100条") Integer limit,
                                                                @RequestParam(defaultValue = "1") @EnumValue(byteValues = {1, 2, 3, 4, 5}, message = "排序类型标识不合法")
                                                                    @Parameter(name = "sortCode", description = "排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序", in = ParameterIn.QUERY)
                                                                    Byte sortCode,
                                                                @RequestParam(defaultValue = "0") @EnumValue(byteValues = {0, 1}, message = "排序顺序不合法")
                                                                    @Parameter(name = "sortSequence", description = "排序顺序 0：降序 1：升序", in = ParameterIn.QUERY)
                                                                    Byte sortSequence) {
        return R.ok(videoHotSearchProducer.searchHotVideos(snapshotId, page, limit, sortCode, sortSequence));
    }

    @GetMapping("/history")
    @Operation(summary = "WEB端-分页查询搜索历史", description = "分页查询用户的爆款搜索历史记录")
    public R<PageUtils<VideoHotSearchHistoryVo>> getSearchHistory(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
                                                                  @RequestParam(defaultValue = "10") @Max(value = 100, message = "每页最大条数不能超过100条") Integer limit) {
        return R.ok(videoHotSearchProducer.getSearchHistory(page, limit));
    }

    @PostMapping("/list")
    @Operation(summary = "WEB端-根据条件查询搜索列表", description = "根据快照ID和筛选条件查询爆款视频列表")
    public R<PageUtils<VideoHotSearchResultVo>> getSearchList(@RequestBody @Validated VideoHotSearchListQueryBo queryBo) {
        return R.ok(videoHotSearchProducer.getSearchList(queryBo));
    }

    @PostMapping("/list/subscription")
    @Operation(summary = "WEB端-根据订阅条件查询视频列表", description = "根据订阅条件查询视频列表")
    public R<PageUtils<VideoHotSearchResultVo>> getVideoListBySubscriptionCondition(@RequestBody @Validated VideoHotSearchListSubscriptionQueryBo queryBo) {
        return R.ok(videoHotSearchProducer.getVideoListBySubscriptionCondition(queryBo));
    }

    @PostMapping("/list/example")
    @Operation(summary = "WEB端-根据爆款示例条件查询视频列表", description = "根据爆款示例条件查询视频列表")
    public R<PageUtils<VideoHotSearchResultVo>> getVideoListByExampleCondition(@RequestBody @Validated VideoHotSearchListExampleQueryBo queryBo) {
        return R.ok(videoHotSearchProducer.getVideoListByExampleCondition(queryBo));
    }

    @PostMapping("/subscription")
    @Operation(summary = "WEB端-添加爆款订阅", description = "添加新的爆款订阅")
    public R<VideoUserHotSubscriptionEntity> addSubscription(@Validated @RequestBody VideoHotSubscriptionAddBo addBo) {
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        return R.ok(videoHotSearchProducer.addSubscribe(addBo, userCacheVo, tenantId));
    }

    @PutMapping("/subscription")
    @Operation(summary = "WEB端-编辑爆款订阅", description = "编辑爆款订阅信息")
    public R<Boolean> editSubscription(@Validated @RequestBody VideoHotSubscriptionEditBo editBo) {
        return R.ok(videoHotSearchProducer.editSubscription(editBo));
    }

    @DeleteMapping("/subscription/{subscriptionId}")
    @Operation(summary = "WEB端-删除爆款订阅", description = "删除指定的爆款订阅")
    public R<Boolean> deleteSubscription(@PathVariable Long subscriptionId) {
        return R.ok(videoHotSearchProducer.deleteSubscription(subscriptionId));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "WEB端-查询订阅爆款列表", description = "查询用户的爆款订阅列表，按分组展示")
    public R<List<VideoUserHotSubscriptionListVo>> getSubscriptionList() {
        return R.ok(videoHotSearchProducer.getSubscriptionList());
    }

    @GetMapping("/subscription/list")
    @Operation(summary = "客户端-获取当前用户订阅列表", description = "获取当前用户订阅列表")
    public R<List<VideoUserHotSubscriptionAllVo>> getSubscriptionListForClient() {
        return R.ok(videoHotSearchProducer.getSubscriptionListForClient());
    }

    @PostMapping("/syncVideoHotSearchData")
    @Operation(summary = "客户端-同步爆款订阅数据", description = "根据订阅同步爆款数据")
    public R<Boolean> syncVideoHotSearchData(@RequestBody @Validated(VideoHotSearchSyncDataBo.Add.class) VideoHotSearchSyncDataBo videoHotSearchSyncDataBo) {
        return R.ok(videoHotSearchProducer.syncVideoHotSearchData(videoHotSearchSyncDataBo));
    }

    @GetMapping("/videoList")
    @Operation(summary = "客户端-获取视频列表根据平台类型和关键字", description = "获取视频列表根据平台类型和关键字")
    public R<List<VideoHotSearchVideoInfoVo>> getVideoListByPlatformTypeAndPlatformVideoId(
            @RequestParam @NotNull(message = "平台类型不能为空") @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法") Byte platformType,
            @RequestParam @NotBlank(message = "关键字不能为空") @Length(max = 100, message = "关键字长度不能超过100个字符") String keyword) {
        return R.ok(videoHotSearchProducer.getVideoListByPlatformTypeAndKeyword(platformType, keyword));
    }

    @GetMapping("/admin/subscriptions/{userId}")
    @Operation(summary = "后台管理-根据用户ID查询爆款订阅信息", description = "查询用户的爆款订阅信息，包括关键词、所属行业、短视频数量、订阅时间")
    public R<List<AdminHotSubscriptionVo>> getHotSubscriptionsByUserId(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        return R.ok(videoHotSearchProducer.getHotSubscriptionsByUserId(userId));
    }

}
