package com.jiuyu.replay.video.project.producer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.words.TradeFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.video.common.utils.LambdaUtil;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerInfoBo;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerSaveSearchInfoBo;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerSearchBo;
import com.jiuyu.replay.video.project.bo.influencer.VideoInfluencerSubscribeBo;
import com.jiuyu.replay.video.project.bo.subscription.InfluencerSubscriptionEditBo;
import com.jiuyu.replay.video.project.bo.subscription.InfluencerSubscriptionQueryBo;
import com.jiuyu.replay.video.project.entity.*;
import com.jiuyu.replay.video.project.service.*;
import com.jiuyu.replay.video.project.vo.admin.AdminInfluencerSubscriptionVo;
import com.jiuyu.replay.video.project.vo.influencer.VideoInfluencerInfoDetailVo;
import com.jiuyu.replay.video.project.vo.influencer.VideoInfluencerSearchHistoryVo;
import com.jiuyu.replay.video.project.vo.subscription.InfluencerSubscriptionGroupVo;
import com.jiuyu.replay.video.project.vo.subscription.InfluencerSubscriptionVo;
import com.jiuyu.replay.video.project.vo.subscription.VideoInfluencerSubscriptionVo;
import com.jiuyu.replay.video.project.vo.video.VideoCommentLikeRatioVo;
import com.jiuyu.replay.video.project.vo.video.VideoInfluencerInfoDetailVideoVo;
import com.jiuyu.replay.video.project.vo.video.VideoInfoIncrementVo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 达人模块业务聚合层
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 达人模块业务逻辑聚合层，提供达人搜索、订阅、分组管理等复合业务功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInfluencerProducer {

    private final VideoInfluencerSearchSnapshotService videoInfluencerSearchSnapshotService;
    private final VideoInfluencerDailyDataService videoInfluencerDailyDataService;
    private final VideoUserSubscriptionGroupService videoUserSubscriptionGroupService;
    private final VideoUserInfluencerSubscriptionService videoUserInfluencerSubscriptionService;
    private final VideoInfluencerSearchRelationService videoInfluencerSearchRelationService;
    private final VideoInfluencerInfoService videoInfluencerInfoService;
    private final VideoInfoService videoInfoService;
    private final UserFeign userFeign;
    private final VideoInfoDailyDataService videoInfoDailyDataService;
    private final TradeFeign tradeFeign;
    private final VideoUserVideoService videoUserVideoService;
    private final SystemKvService systemKvService;
    private final UserPropertyFeign userPropertyFeign;

    /**
     * 保存搜索达人信息
     * TODO: 成功后需要扣减套餐资产
     *
     * @param searchBO 搜索参数
     * @return 搜索快照id
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveSearchInfluencers(VideoInfluencerSearchBo searchBO) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 创建搜索快照
        LocalDateTime createdDate = LocalDateTime.now();
        VideoInfluencerSearchSnapshotEntity snapshot = VideoInfluencerSearchSnapshotEntity.builder()
                .id(SnowflakeManager.nextValue())
                .userId(userCacheVo.getId())
                .tenantId(tenantId)
                .searchKeyword(searchBO.getSearchKeyword().trim())
                .platformType(searchBO.getPlatformType())
                .totalInfluencers(CollectionUtil.isNotEmpty(searchBO.getInfluencerList()) ? searchBO.getInfluencerList().size() : 0) // 搜索到的达人数量
                .totalVideos(CollectionUtil.isNotEmpty(searchBO.getInfluencerList()) ? searchBO.getInfluencerList().stream().mapToInt(VideoInfluencerSaveSearchInfoBo::getVideoCount).sum() : 0) // 搜索到的视频数量
                .searchTime(createdDate)
                .createdDate(createdDate)
                .updateDate(createdDate)
                .build();
        if (CollectionUtil.isEmpty(searchBO.getInfluencerList())) {
            videoInfluencerSearchSnapshotService.save(snapshot);
            return snapshot.getId();
        }

        // 保存达人信息&和搜索快照关联信息
        List<VideoInfluencerInfoEntity> infoEntities = new ArrayList<>();
        List<VideoInfluencerSearchRelationEntity> relationEntities = new ArrayList<>();
        searchBO.getInfluencerList().forEach(LambdaUtil.consumerWithIndex((item, index) -> {

            VideoInfluencerInfoEntity influencerInfoEntity = new VideoInfluencerInfoEntity();
            BeanUtils.copyProperties(item, influencerInfoEntity);
            // 搜索达人保存数据-设置同步时间为空
            influencerInfoEntity.setLastSyncTime(null);
            infoEntities.add(influencerInfoEntity);

            // 关联关系信息（暂时不设置influencerId和snapshotId）
            VideoInfluencerSearchRelationEntity relationEntity = VideoInfluencerSearchRelationEntity.builder()
                    .id(SnowflakeManager.nextValue())
                    .sortOrder(index + 1)
                    .videoCount(item.getVideoCount())
                    .followersCount(item.getFollowersCount())
                    .isProcessed((byte) 0)
                    .createdDate(createdDate)
                    .updateDate(createdDate)
                    .isDeleted((byte) 0)
                    .build();
            relationEntities.add(relationEntity);
        }));

        // 使用推荐的方案1：先保存达人信息，再创建关联关系
        saveInfluencersAndRelations(infoEntities, relationEntities, snapshot);
        // 扣减套餐资产
        boolean property = userPropertyFeign.useShortVideoProperty(userCacheVo.getId(), OrderEnums.commodityTypeCode.SEARCH_INFLUENCER_NUM.getCode(), -1L);
        if (!property) {
            throw new BusinessException("套餐资源不足！");
        }
        return snapshot.getId();
    }

    /**
     * 搜索达人结果数据
     *
     * @param snapshotId 快照id
     * @param page       当前页
     * @param limit      每页条数
     * @return 返回搜索结果
     */
    public PageUtils<VideoInfluencerSearchHistoryVo> searchInfluencers(Long snapshotId, Integer page, Integer limit) {
        return queryHistoryRecord(snapshotId, page, limit, null);
    }

    /**
     * 达人搜索记录
     *
     * @param page  当前页
     * @param limit 每页条数
     * @return
     */
    public PageUtils<VideoInfluencerSearchHistoryVo> queryHistoryRecord(Integer page, Integer limit) {
        return queryHistoryRecord(null, page, limit, LocalDateTime.now().minusDays(7));
    }

    /**
     * 获取达人信息最后同步时间
     *
     * @param platformType   平台类型
     * @param platformUserId 平台ID
     * @return
     */
    public LocalDateTime getInfluencerLastSyncTime(@NotNull(message = "平台类型不能为空") Byte platformType, @NotBlank(message = "平台ID不能为空") @Length(max = 100, message = "平台ID不合法") String platformUserId) {
        VideoInfluencerInfoEntity influencerInfoEntity = videoInfluencerInfoService.getOne(new LambdaQueryWrapper<>(VideoInfluencerInfoEntity.class).select(VideoInfluencerInfoEntity::getLastSyncTime).eq(VideoInfluencerInfoEntity::getPlatformType, platformType).eq(VideoInfluencerInfoEntity::getPlatformUserId, platformUserId));
        if (Objects.isNull(influencerInfoEntity)) {
            return null;
        }
        return influencerInfoEntity.getLastSyncTime();
    }

    /**
     * 判断达人是否需要同步视频
     *
     * @param platformType   平台类型
     * @param platformUserId 平台ID
     * @return
     */
    public Boolean hasInfluencerSyncVideos(@NotNull(message = "平台类型不能为空") Byte platformType, @NotBlank(message = "平台ID不能为空") @Length(max = 100, message = "平台ID不合法") String platformUserId) {
        SystemKvEntity syncShortVideoInterval = systemKvService.getByKey("sync_short_video_min_interval");
        if (Objects.isNull(syncShortVideoInterval)) {
            return true;
        }
        LocalDateTime influencerLastSyncTime = this.getInfluencerLastSyncTime(platformType, platformUserId);
        if (Objects.isNull(influencerLastSyncTime)) {
            return true;
        }
        if (Objects.equals(syncShortVideoInterval.getKvValue(), "-1")) {
            return false;
        }
        return LocalDateTime.now().minusSeconds(Long.parseLong(syncShortVideoInterval.getKvValue())).isAfter(influencerLastSyncTime);
    }

    /**
     * 达人搜索
     *
     * @param snapshotId 快照搜索id
     * @param page       当前页
     * @param limit      每页条数
     * @param queryTime  查询时间
     * @return
     */
    public PageUtils<VideoInfluencerSearchHistoryVo> queryHistoryRecord(Long snapshotId, Integer page, Integer limit, LocalDateTime queryTime) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        IPage<VideoInfluencerSearchRelationEntity> videoInfluencerSearchRelationEntityIPage = videoInfluencerSearchRelationService.queryHistoryRecord(new Page<>(page, limit), tenantId, userId, snapshotId, queryTime);
        List<VideoInfluencerSearchRelationEntity> records = videoInfluencerSearchRelationEntityIPage.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageUtils<>(videoInfluencerSearchRelationEntityIPage, Collections.emptyList());
        }

        List<Long> influencerIds = records.stream().map(VideoInfluencerSearchRelationEntity::getInfluencerId).distinct().toList();
        List<VideoInfluencerInfoEntity> influencerInfoEntityList = videoInfluencerInfoService.listByIds(influencerIds);
        if (CollectionUtil.isEmpty(influencerInfoEntityList)) {
            return new PageUtils<>(videoInfluencerSearchRelationEntityIPage, Collections.emptyList());
        }
        Map<Long, VideoInfluencerInfoEntity> influencerInfoEntityMap = influencerInfoEntityList.stream().collect(Collectors.toMap(VideoInfluencerInfoEntity::getId, Function.identity(), (existing, replacement) -> existing));
        List<VideoInfluencerSearchHistoryVo> results = new ArrayList<>();
        records.forEach(item -> {
            VideoInfluencerInfoEntity influencerInfoEntity = influencerInfoEntityMap.get(item.getInfluencerId());
            results.add(VideoInfluencerSearchHistoryVo.builder()
                    .platformType(influencerInfoEntity.getPlatformType())
                    .platformUserId(influencerInfoEntity.getPlatformUserId())
                    .nickname(influencerInfoEntity.getNickname())
                    .avatar(influencerInfoEntity.getAvatar())
                    .platformAccount(influencerInfoEntity.getPlatformAccount())
                    .followersCount(influencerInfoEntity.getFollowersCount())
                    .videoCount(item.getVideoCount())
                    .build());
        });
        return new PageUtils<>(videoInfluencerSearchRelationEntityIPage, results);
    }

    /**
     * 订阅达人（分布式锁逻辑：同一租户下 相同达人加锁订阅）
     *
     * @param videoInfluencerSubscribeBo 订阅数据
     * @param userCacheVo                用户信息
     * @param tenantId                   租户id
     * @return
     */
    @CustomRedissonLock(key = "'replay:lock:influencer:subscribe:' + #args[2] + ':' + #args[0].platformType + ':' + #args[0].platformUserId")
    @Transactional(rollbackFor = Exception.class)
    public VideoUserInfluencerSubscriptionEntity subscribe(VideoInfluencerSubscribeBo videoInfluencerSubscribeBo, UserCacheVo userCacheVo, Long tenantId) {
        // 获取用户信息和租户信息
        Long userId = userCacheVo.getId();

        VideoInfluencerInfoEntity influencerInfoEntity = videoInfluencerInfoService.singleSaveOrUpdate(BeanConvertUtils.convert(videoInfluencerSubscribeBo, VideoInfluencerInfoEntity.class));
        VideoUserInfluencerSubscriptionEntity subscription = videoUserInfluencerSubscriptionService.getOne(new LambdaQueryWrapper<>(VideoUserInfluencerSubscriptionEntity.class)
                .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
                .eq(VideoUserInfluencerSubscriptionEntity::getInfluencerId, influencerInfoEntity.getId()));

        // 已经存在订阅
        if (Objects.nonNull(subscription)) {
            throw new BusinessException("已订阅该达人！不能重复订阅");
        }
        // 验证行业ID是否存在
        validateIndustryExists(videoInfluencerSubscribeBo.getIndustryId());

        // 如果分组ID不为空验证分组ID是否存在 并且如果是用户是子账号需判断分组是否是自己的
        if (videoInfluencerSubscribeBo.getGroupId() != null) {
            validateGroupExists(videoInfluencerSubscribeBo.getGroupId(), userId, tenantId, userCacheVo.getUserType());
        }

        LocalDateTime now = LocalDateTime.now();
        VideoUserInfluencerSubscriptionEntity subscriptionEntity = VideoUserInfluencerSubscriptionEntity.builder()
                .id(SnowflakeManager.nextValue())
                .userId(userId)
                .tenantId(tenantId)
                .groupId(videoInfluencerSubscribeBo.getGroupId())
                .industryId(videoInfluencerSubscribeBo.getIndustryId())
                .influencerId(influencerInfoEntity.getId())
                .isEnabled((byte) 0)
                .monitorFrequency(8)
                .lastSyncTime(now)
                .createdDate(now)
                .updateDate(now).build();
        boolean save = videoUserInfluencerSubscriptionService.save(subscriptionEntity);
        if (save) {
            // 调用消耗资源接口
            boolean property = userPropertyFeign.useShortVideoProperty(userCacheVo.getId(), OrderEnums.commodityTypeCode.SUBSCRIBE_INFLUENCER_NUM.getCode(), -1L);
            if (!property) {
                throw new BusinessException("套餐资源不足！");
            }
        }
        return subscriptionEntity;
    }

    /**
     * 保存达人信息和关联关系
     *
     * @param infoEntities     达人信息列表
     * @param relationEntities 关联关系列表
     * @param snapshot         搜索快照
     */
    private void saveInfluencersAndRelations(
            List<VideoInfluencerInfoEntity> infoEntities,
            List<VideoInfluencerSearchRelationEntity> relationEntities,
            VideoInfluencerSearchSnapshotEntity snapshot) {

        // 验证数据一致性
        if (infoEntities.size() != relationEntities.size()) {
            throw new IllegalArgumentException("达人信息和关联关系数量不匹配");
        }

        // 1. 先批量保存达人信息
        videoInfluencerInfoService.batchSaveOrUpdate(infoEntities);

        // 2. 保存搜索快照
        videoInfluencerSearchSnapshotService.save(snapshot);

        // 3. 通过索引匹配，设置关联关系的真实influencerId
        for (int i = 0; i < infoEntities.size(); i++) {
            VideoInfluencerInfoEntity influencer = infoEntities.get(i);
            VideoInfluencerSearchRelationEntity relation = relationEntities.get(i);

            // 设置真实的达人ID和快照ID
            relation.setInfluencerId(influencer.getId());
            relation.setSnapshotId(snapshot.getId());

        }

        // 4. 批量保存关联关系
        videoInfluencerSearchRelationService.saveBatch(relationEntities);

    }

    /**
     * 同步单个达人数据（包含短视频信息）
     *
     * @param videoInfluencerInfoBo 达人数据（包含基础信息和视频列表）
     * @return 同步结果消息
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean syncInfluencerInfo(VideoInfluencerInfoBo videoInfluencerInfoBo) {

        try {
            // 1. 同步达人基础信息
            VideoInfluencerInfoEntity savedInfluencer = syncSingleInfluencerInfo(videoInfluencerInfoBo);

            // 2. 同步视频信息（如果有）
            if (CollectionUtil.isNotEmpty(videoInfluencerInfoBo.getVideoInfoEntityList())) {
                syncVideoInfoBatch(savedInfluencer, videoInfluencerInfoBo.getVideoInfoEntityList());
            }

            return true;

        } catch (Exception e) {
            log.error("同步单个达人数据失败，平台类型: {}, 平台用户ID: {}",
                    videoInfluencerInfoBo.getPlatformType(), videoInfluencerInfoBo.getPlatformUserId(), e);
            throw new RuntimeException("同步达人数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取达人详情
     *
     * @param platformType   平台类型
     * @param platformUserId 平台用户Id
     * @return
     */
    public VideoInfluencerInfoDetailVo getInfluencerDetail(Byte platformType, String platformUserId) {
        VideoInfluencerInfoEntity influencerInfoEntity = videoInfluencerInfoService.getOne(
                new LambdaQueryWrapper<>(VideoInfluencerInfoEntity.class)
                        .eq(VideoInfluencerInfoEntity::getPlatformType, platformType)
                        .eq(VideoInfluencerInfoEntity::getPlatformUserId, platformUserId));
        Assert.notNull(influencerInfoEntity, "达人不存在");
        VideoInfluencerInfoDetailVo influencerInfoDetailVo = VideoInfluencerInfoDetailVo.builder()
                .id(influencerInfoEntity.getId())
                .platformType(influencerInfoEntity.getPlatformType())
                .platformUserId(influencerInfoEntity.getPlatformUserId())
                .platformAccount(influencerInfoEntity.getPlatformAccount())
                .avatar(influencerInfoEntity.getAvatar())
                .nickname(influencerInfoEntity.getNickname())
                .description(influencerInfoEntity.getDescription())
                .followersCount(influencerInfoEntity.getFollowersCount())
                .videoCount(influencerInfoEntity.getVideoCount())
                .likeCount(influencerInfoEntity.getLikeCount())
                .lastSyncTime(influencerInfoEntity.getLastSyncTime())
                .build();
        // 查询近期新增粉丝数
        LocalDate now = LocalDate.now();
        VideoInfluencerDailyDataEntity influencerDailyData = videoInfluencerDailyDataService
                .getOne(new LambdaQueryWrapper<>(VideoInfluencerDailyDataEntity.class).select(VideoInfluencerDailyDataEntity::getFollowersIncrement, VideoInfluencerDailyDataEntity::getVideoCount)
                        .eq(VideoInfluencerDailyDataEntity::getInfluencerId, influencerInfoDetailVo.getId())
                        .eq(VideoInfluencerDailyDataEntity::getDataDate, now));
        // 2.查询短视频基础信息表 获取3日发布数据总量
        long threeDayVideoCount = videoInfoService.count(new LambdaQueryWrapper<VideoInfoEntity>()
                .eq(VideoInfoEntity::getAuthorId, influencerInfoDetailVo.getId())
                .ge(VideoInfoEntity::getPublishTime, now.minusDays(2L).atStartOfDay())
                .le(VideoInfoEntity::getPublishTime, now.atTime(LocalTime.MAX)));
        influencerInfoDetailVo.setYesterdayFansCount(Objects.nonNull(influencerDailyData) ? influencerDailyData.getFollowersIncrement() : 0);
        influencerInfoDetailVo.setThreeDaysVideoCount((int) threeDayVideoCount);
        return influencerInfoDetailVo;
    }

    /**
     * 获取达人详情视频列表
     *
     * @param page                 当前页
     * @param limit                每页条数
     * @param platformType         平台类型
     * @param platformUserId       平台用户ID
     * @param sortCode             排序code 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序 6：评赞比排序
     * @param sortSequence         排序顺序 0：降序 1：升序
     * @param videoPublishTimeType 视频发布时间类型
     * @return
     */
    public PageUtils<VideoInfluencerInfoDetailVideoVo> getInfluencerDetailVideos(Integer page, Integer limit, Byte platformType, String platformUserId, Byte sortCode, Byte sortSequence, Byte videoPublishTimeType) {
        // 获取当前用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        VideoInfluencerInfoEntity influencerInfoEntity = videoInfluencerInfoService.getOne(new LambdaQueryWrapper<>(VideoInfluencerInfoEntity.class)
                .eq(VideoInfluencerInfoEntity::getPlatformType, platformType)
                .eq(VideoInfluencerInfoEntity::getPlatformUserId, platformUserId));
        if (Objects.isNull(influencerInfoEntity)) {
            return new PageUtils<>(page, limit);
        }
        LambdaQueryWrapper<VideoInfoEntity> wrapper = new LambdaQueryWrapper<>(VideoInfoEntity.class)
                .eq(VideoInfoEntity::getAuthorId, influencerInfoEntity.getId().toString());
        if (Objects.nonNull(videoPublishTimeType)) {
            // 设置视频发布时间筛选
            LocalDate now = LocalDate.now();
            LocalDateTime startTime = videoPublishTimeType == 1 ? now.atStartOfDay() : now.minusDays(2).atStartOfDay();
            LocalDateTime endTime = now.atTime(LocalTime.MAX);
            wrapper.ge(VideoInfoEntity::getPublishTime, startTime).le(VideoInfoEntity::getPublishTime, endTime);
        }
        switch (sortCode) {

            case 2:
                wrapper.last("ORDER BY like_count " + (sortSequence == 0 ? "DESC" : "ASC"));
                break;
            case 3:
                wrapper.last("ORDER BY comment_count " + (sortSequence == 0 ? "DESC" : "ASC"));
                break;
            case 4:
                wrapper.last("ORDER BY share_count " + (sortSequence == 0 ? "DESC" : "ASC"));
                break;
            case 5:
                wrapper.last("ORDER BY collect_count " + (sortSequence == 0 ? "DESC" : "ASC"));
                break;
            case 6:
                // 评赞比排序：评论数/点赞数
                wrapper.last("ORDER BY CASE WHEN like_count > 0 THEN comment_count / like_count ELSE 0 END " + (sortSequence == 0 ? "DESC" : "ASC"));
                break;
            default:
                wrapper.last("ORDER BY publish_time DESC,created_date DESC");
                break;
        }
        Page<VideoInfoEntity> videoInfoEntityPage = videoInfoService.page(new Page<>(page, limit), wrapper);
        List<VideoInfoEntity> records = videoInfoEntityPage.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageUtils<>(videoInfoEntityPage, Collections.emptyList());
        }
        List<Long> videoIds = records.stream().map(VideoInfoEntity::getId).toList();

        // 查询当前用户的视频提取状态
        Map<Long, VideoUserVideoEntity> userVideoExtractMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(videoIds)) {
            List<VideoUserVideoEntity> userVideoEntities = videoUserVideoService.list(
                    Wrappers.<VideoUserVideoEntity>lambdaQuery()
                            .eq(VideoUserVideoEntity::getTenantId, tenantId)
                            .in(VideoUserVideoEntity::getVideoId, videoIds)
            );
            if (CollectionUtil.isNotEmpty(userVideoEntities)) {
                userVideoExtractMap = userVideoEntities.stream()
                        .collect(Collectors.toMap(VideoUserVideoEntity::getVideoId, Function.identity(), (existing, replacement) -> existing));
            }
        }
        LocalDate now = LocalDate.now();
        // 查询每个视频最近一次同步的数据用于计算增量（使用自定义SQL优化性能）
        List<VideoInfoDailyDataEntity> dailyDataEntities = videoInfoDailyDataService.getLatestDailyDataByVideoIds(videoIds, now);
        // 获取视频今日数据
        List<VideoInfoDailyDataEntity> todayDataEntities = videoInfoDailyDataService.list(Wrappers.<VideoInfoDailyDataEntity>lambdaQuery().in(VideoInfoDailyDataEntity::getVideoId, videoIds).eq(VideoInfoDailyDataEntity::getDataDate, now));

        Map<Long, VideoInfoDailyDataEntity> lastVideoMap;
        Map<Long, VideoInfoDailyDataEntity> todayVideoMap;
        if (CollectionUtil.isNotEmpty(dailyDataEntities)) {
            lastVideoMap = dailyDataEntities.stream().collect(Collectors.toMap(VideoInfoDailyDataEntity::getVideoId, Function.identity(), (existing, replacement) -> existing));
        } else {
            lastVideoMap = null;
        }
        if (CollectionUtil.isNotEmpty(todayDataEntities)) {
            todayVideoMap = todayDataEntities.stream().collect(Collectors.toMap(VideoInfoDailyDataEntity::getVideoId, Function.identity(), (existing, replacement) -> existing));
        } else {
            todayVideoMap = null;
        }
        List<VideoInfluencerInfoDetailVideoVo> videoInfluencerInfoDetailVideoVos = new ArrayList<>();
        final Map<Long, VideoUserVideoEntity> finalUserVideoExtractMap = userVideoExtractMap;
        records.forEach(videoInfoEntity -> {
            // 获取当前用户对该视频的提取状态，如果没有则默认为null（未提取）
            VideoUserVideoEntity videoUserVideoEntity = finalUserVideoExtractMap.get(videoInfoEntity.getId());

            VideoInfluencerInfoDetailVideoVo influencerInfoDetailVideoVo = VideoInfluencerInfoDetailVideoVo.builder()
                    .id(videoInfoEntity.getId())
                    .platformType(videoInfoEntity.getPlatformType())
                    .platformVideoId(videoInfoEntity.getPlatformVideoId())
                    .coverUrl(videoInfoEntity.getCoverUrl())
                    .videoUrl(videoInfoEntity.getVideoUrl())
                    .videoHash(videoInfoEntity.getVideoHash())
                    .title(videoInfoEntity.getTitle())
                    .publishTime(videoInfoEntity.getPublishTime())
                    .duration(videoInfoEntity.getDuration())
                    .extractStatus(videoUserVideoEntity != null ? videoUserVideoEntity.getExtractStatus() : 0)
                    .extractId(videoUserVideoEntity != null ? videoUserVideoEntity.getId() : 0)
                    .build();
            List<VideoInfoIncrementVo> likeCountIncrements = new ArrayList<>(2);
            List<VideoInfoIncrementVo> commentCountIncrements = new ArrayList<>(2);
            List<VideoInfoIncrementVo> shareCountIncrements = new ArrayList<>(2);
            List<VideoInfoIncrementVo> collectCountIncrements = new ArrayList<>(2);
            List<VideoCommentLikeRatioVo> commentLikeRatios = new ArrayList<>(2);

            // 处理今日数据
            if (Objects.nonNull(todayVideoMap) && todayVideoMap.containsKey(videoInfoEntity.getId())) {
                VideoInfoDailyDataEntity todayVideoInfo = todayVideoMap.get(videoInfoEntity.getId());
                likeCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(todayVideoInfo.getLikeCount()).build());
                commentCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(todayVideoInfo.getCommentCount()).build());
                shareCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(todayVideoInfo.getShareCount()).build());
                collectCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(todayVideoInfo.getCollectCount()).build());
                String todayRatio = calculateCommentLikeRatio(todayVideoInfo.getCommentCount(), todayVideoInfo.getLikeCount());
                commentLikeRatios.add(VideoCommentLikeRatioVo.builder().dataDate(now).ratio(todayRatio).build());
            } else {
                likeCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(null).build());
                commentCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(null).build());
                shareCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(null).build());
                collectCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(now).number(null).build());
                commentLikeRatios.add(VideoCommentLikeRatioVo.builder().dataDate(now).ratio(null).build());
            }

            // 处理上次数据
            if (Objects.nonNull(lastVideoMap) && lastVideoMap.containsKey(videoInfoEntity.getId())) {
                VideoInfoDailyDataEntity lastVideoInfo = lastVideoMap.get(videoInfoEntity.getId());
                likeCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(lastVideoInfo.getDataDate()).number(lastVideoInfo.getLikeCount()).build());
                commentCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(lastVideoInfo.getDataDate()).number(lastVideoInfo.getCommentCount()).build());
                shareCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(lastVideoInfo.getDataDate()).number(lastVideoInfo.getShareCount()).build());
                collectCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(lastVideoInfo.getDataDate()).number(lastVideoInfo.getCollectCount()).build());
                String lastRatio = calculateCommentLikeRatio(lastVideoInfo.getCommentCount(), lastVideoInfo.getLikeCount());
                commentLikeRatios.add(VideoCommentLikeRatioVo.builder().dataDate(lastVideoInfo.getDataDate()).ratio(lastRatio).build());
            } else {
                likeCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(null).number(null).build());
                commentCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(null).number(null).build());
                shareCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(null).number(null).build());
                collectCountIncrements.add(VideoInfoIncrementVo.builder().dataDate(null).number(null).build());
                commentLikeRatios.add(VideoCommentLikeRatioVo.builder().dataDate(null).ratio(null).build());
            }

            influencerInfoDetailVideoVo.setLikeCountIncrements(likeCountIncrements);
            influencerInfoDetailVideoVo.setCommentCountIncrements(commentCountIncrements);
            influencerInfoDetailVideoVo.setShareCountIncrements(shareCountIncrements);
            influencerInfoDetailVideoVo.setCollectCountIncrements(collectCountIncrements);
            influencerInfoDetailVideoVo.setCommentLikeRatios(commentLikeRatios);

            videoInfluencerInfoDetailVideoVos.add(influencerInfoDetailVideoVo);
        });
        return new PageUtils<>(videoInfoEntityPage, videoInfluencerInfoDetailVideoVos);
    }

    /**
     * 计算评赞比
     *
     * @param commentCount 评论数
     * @param likeCount    点赞数
     * @return 评赞比（保留两位小数）
     */
    private String calculateCommentLikeRatio(Long commentCount, Long likeCount) {
        if (likeCount != null && likeCount > 0 && commentCount != null) {
            double ratio = (double) commentCount / likeCount;
            return String.format("%.2f", ratio);
        }
        return "0.00";
    }

    /**
     * 查询达人订阅列表
     *
     * @param queryBo 查询信息
     * @return
     */
    public List<InfluencerSubscriptionGroupVo> getInfluencerSubscriptions(InfluencerSubscriptionQueryBo queryBo) {

        // 用户id
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        // 租户id
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        if (Objects.isNull(tenantId) || Objects.equals(tenantId, 0L)) {
            return List.of();
        }

        LambdaQueryWrapper<VideoUserInfluencerSubscriptionEntity> queryWrapper =
                new LambdaQueryWrapper<>(VideoUserInfluencerSubscriptionEntity.class)
                        .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
                        .orderByDesc(VideoUserInfluencerSubscriptionEntity::getCreatedDate);
        if (Objects.nonNull(queryBo.getIndustryId())) {
            queryWrapper.eq(VideoUserInfluencerSubscriptionEntity::getIndustryId, queryBo.getIndustryId());
        }
        List<VideoUserInfluencerSubscriptionEntity> videoUserInfluencerSubscriptionEntities = videoUserInfluencerSubscriptionService.list(queryWrapper);
        if (CollectionUtil.isEmpty(videoUserInfluencerSubscriptionEntities)) {
            return List.of();
        }
        return processInfluencerSubscriptionsByIndustry(videoUserInfluencerSubscriptionEntities, queryBo);
    }

    /**
     * 修改达人订阅
     *
     * @param editBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean editInfluencerSubscription(InfluencerSubscriptionEditBo editBo) {
        VideoUserInfluencerSubscriptionEntity subscriptionEntity = videoUserInfluencerSubscriptionService.getById(editBo.getSubscriptionId());
        Assert.notNull(subscriptionEntity, "订阅不存在");
        // 用户id
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        // 租户id
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        if (Objects.isNull(tenantId) || Objects.equals(tenantId, 0L)) {
            return false;
        }
        Assert.isTrue(Objects.equals(subscriptionEntity.getTenantId(), tenantId), "订阅不存在");
        if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(subscriptionEntity.getUserId(), userId), "订阅不存在");
        }

        // 验证行业ID是否存在（如果行业ID不为null）
        if (editBo.getIndustryId() != null) {
            validateIndustryExists(editBo.getIndustryId());
        }

        // 如果分组ID不为空验证分组ID是否存在 并且如果是用户是子账号需判断分组是否是自己的
        if (editBo.getGroupId() != null) {
            validateGroupExists(editBo.getGroupId(), userId, tenantId, userCacheVo.getUserType());
        }

        if (Objects.nonNull(editBo.getAutoSyncEnabled()) && editBo.getAutoSyncEnabled() == 1) {
            Assert.notNull(editBo.getLikeCountThreshold(), "点赞阈值条件不能为空");
            Assert.notNull(editBo.getUpdateTimeCondition(), "更新时间条件不能为空");
            subscriptionEntity.setLikeCountThreshold(editBo.getLikeCountThreshold());
            subscriptionEntity.setUpdateTimeCondition(editBo.getUpdateTimeCondition());
        } else {
            subscriptionEntity.setLikeCountThreshold(null);
            subscriptionEntity.setUpdateTimeCondition(null);
        }

        subscriptionEntity.setIndustryId(editBo.getIndustryId());
        subscriptionEntity.setGroupId(editBo.getGroupId());
        subscriptionEntity.setIsEnabled(editBo.getAutoSyncEnabled());
        subscriptionEntity.setUpdateDate(LocalDateTime.now());

        return videoUserInfluencerSubscriptionService.updateById(subscriptionEntity);
    }

    /**
     * 删除达人订阅
     *
     * @param subscriptionId 订阅ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInfluencerSubscription(Long subscriptionId) {

        // 查询订阅记录
        VideoUserInfluencerSubscriptionEntity subscriptionEntity = videoUserInfluencerSubscriptionService.getById(subscriptionId);
        Assert.notNull(subscriptionEntity, "订阅记录不存在");

        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户信息无效");

        // 验证租户权限
        Assert.isTrue(Objects.equals(subscriptionEntity.getTenantId(), tenantId), "无权限删除该订阅");

        // 如果是子账号，验证是否为自己的订阅
        if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(subscriptionEntity.getUserId(), userId), "子账号只能删除自己的订阅");
        }

        // 如果订阅时间在24小时内不能进行删除
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(24);
        if (subscriptionEntity.getCreatedDate().isAfter(localDateTime)) {
            throw new BusinessException("新订阅达人24小时内不能删除！");
        }

        // 执行删除操作
        boolean deleteResult = videoUserInfluencerSubscriptionService.removeById(subscriptionId);

        if (deleteResult) {
            // 调用增加资源接口
            boolean property = userPropertyFeign.useShortVideoProperty(subscriptionEntity.getUserId(), OrderEnums.commodityTypeCode.SUBSCRIBE_INFLUENCER_NUM.getCode(), 1L);
            if (!property) {
                throw new BusinessException("回退资源失败！");
            }
        }

        return deleteResult;
    }

    /**
     * 同步达人基础信息
     *
     * @param videoInfluencerInfoBo 达人数据
     * @return 保存后的达人信息实体
     */
    private VideoInfluencerInfoEntity syncSingleInfluencerInfo(VideoInfluencerInfoBo videoInfluencerInfoBo) {
        VideoInfluencerInfoEntity influencerEntity = new VideoInfluencerInfoEntity();
        BeanUtils.copyProperties(videoInfluencerInfoBo, influencerEntity);
        // 设置同步时间
        influencerEntity.setLastSyncTime(LocalDateTime.now());
        // 使用原子性的保存或更新方法
        return videoInfluencerInfoService.singleSaveOrUpdate(influencerEntity);
    }

    /**
     * 批量同步视频信息
     *
     * @param influencerEntity 达人信息
     * @param videoList        视频信息列表
     */
    private void syncVideoInfoBatch(VideoInfluencerInfoEntity influencerEntity, List<VideoInfoEntity> videoList) {
        if (CollectionUtil.isNotEmpty(videoList)) {
            videoList.forEach(item -> {
                item.setAuthorId(influencerEntity.getId().toString());
                item.setAuthorName(influencerEntity.getNickname());
            });
            // 使用批量原子性的保存或更新方法
            videoInfoService.batchSaveOrUpdate(videoList, true);
        }
    }

    /**
     * 按行业分组处理达人订阅数据
     *
     * @param subscriptionEntities 达人订阅实体列表
     * @param queryBo              查询参数
     * @return 按行业分组的达人订阅数据
     */
    private List<InfluencerSubscriptionGroupVo> processInfluencerSubscriptionsByIndustry(
            List<VideoUserInfluencerSubscriptionEntity> subscriptionEntities,
            InfluencerSubscriptionQueryBo queryBo) {

        if (CollectionUtil.isEmpty(subscriptionEntities)) {
            return List.of();
        }

        // 1. 获取所有达人ID
        List<Long> influencerIds = subscriptionEntities.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getInfluencerId)
                .distinct()
                .toList();

        // 2. 批量查询达人信息
        List<VideoInfluencerInfoEntity> infoEntities = videoInfluencerInfoService.listByIds(influencerIds);
        if (CollectionUtil.isEmpty(infoEntities)) {
            return List.of();
        }
        Map<Long, VideoInfluencerInfoEntity> influencerMap = infoEntities
                .stream()
                .collect(Collectors.toMap(VideoInfluencerInfoEntity::getId, Function.identity(),
                        (existing, replacement) -> existing));

        // 3. 如果有昵称过滤条件，先进行过滤
        List<VideoUserInfluencerSubscriptionEntity> filteredSubscriptions = subscriptionEntities;
        if (StrUtil.isNotBlank(queryBo.getNickname())) {
            filteredSubscriptions = subscriptionEntities.stream()
                    .filter(subscription -> {
                        VideoInfluencerInfoEntity influencer = influencerMap.get(subscription.getInfluencerId());
                        return influencer != null &&
                                StrUtil.isNotBlank(influencer.getNickname()) &&
                                influencer.getNickname().contains(queryBo.getNickname());
                    })
                    .toList();
        }

        if (CollectionUtil.isEmpty(filteredSubscriptions)) {
            return List.of();
        }

        // 4. 按分组ID分组，null表示默认分组，用0L作为key
        Map<Long, List<VideoUserInfluencerSubscriptionEntity>> groupMap = filteredSubscriptions.stream()
                .collect(Collectors.groupingBy(subscription ->
                        subscription.getGroupId() != null ? subscription.getGroupId() : 0L));

        // 5. 获取所有分组ID
        List<Long> groupIds = filteredSubscriptions.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 6. 批量查询分组信息
        Map<Long, VideoUserSubscriptionGroupEntity> groupInfoMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(groupIds)) {
            groupInfoMap = videoUserSubscriptionGroupService.listByIds(groupIds)
                    .stream()
                    .collect(Collectors.toMap(VideoUserSubscriptionGroupEntity::getId, Function.identity(),
                            (existing, replacement) -> existing));
        }

        // 7. 获取所有行业ID
        List<Long> industryIds = filteredSubscriptions.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getIndustryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 8. 批量查询行业信息
        Map<Long, TradeVo> industryMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(industryIds)) {
            List<TradeVo> tradeVos = tradeFeign.listTradeByIds(industryIds);
            if (CollectionUtil.isNotEmpty(tradeVos)) {
                industryMap = tradeVos.stream()
                        .collect(Collectors.toMap(TradeVo::getId, Function.identity(),
                                (existing, replacement) -> existing));
            }
        }

        // 9. 获取所有用户
        List<Long> userIds = filteredSubscriptions.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 10. 批量查询行业信息
        Map<Long, UserDto> userMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(userIds)) {
            List<UserDto> userDtos = userFeign.listByIds(userIds);
            if (CollectionUtil.isNotEmpty(userDtos)) {
                userMap = userDtos.stream()
                        .collect(Collectors.toMap(UserDto::getId, Function.identity(),
                                (existing, replacement) -> existing));
            }
        }

        // 11. 批量获取视频统计数据（优化N+1查询）
        List<Long> allInfluencerIds = filteredSubscriptions.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getInfluencerId)
                .distinct()
                .toList();
        Map<Long, VideoStatistics> statisticsMap = batchGetVideoStatistics(allInfluencerIds);

        // 12. 构建返回结果
        List<InfluencerSubscriptionGroupVo> result = new ArrayList<>();

        for (Map.Entry<Long, List<VideoUserInfluencerSubscriptionEntity>> entry : groupMap.entrySet()) {
            Long groupId = entry.getKey();
            List<VideoUserInfluencerSubscriptionEntity> groupSubscriptions = entry.getValue();
            // 获取分组名称和创建时间，0L表示原始数据中groupId为null的默认分组
            String groupName;
            LocalDateTime createTime;
            if (groupId == 0L) {
                groupName = "默认分组"; // null表示默认分组
                createTime = LocalDateTime.MAX; // 设置为最大时间，确保排在第一位
            } else {
                VideoUserSubscriptionGroupEntity group = groupInfoMap.get(groupId);
                groupName = group != null ? group.getGroupName() : "未知分组";
                createTime = group != null ? group.getCreatedDate() : LocalDateTime.MIN;
            }

            // 构建达人列表
            Map<Long, TradeVo> finalIndustryMap = industryMap;
            Map<Long, UserDto> finalUserMap = userMap;
            List<InfluencerSubscriptionVo> influencers = groupSubscriptions.stream()
                    .map(subscription -> buildInfluencerSubscriptionVo(
                            subscription,
                            influencerMap.get(subscription.getInfluencerId()),
                            finalIndustryMap,
                            finalUserMap,
                            statisticsMap.get(subscription.getInfluencerId())
                    ))
                    .filter(Objects::nonNull)
                    .toList();

            if (CollectionUtil.isNotEmpty(influencers)) {
                InfluencerSubscriptionGroupVo groupVo = InfluencerSubscriptionGroupVo.builder()
                        .groupId(groupId == 0L ? null : groupId) // 0L转回null，表示默认分组
                        .groupName(groupName)
                        .createTime(createTime) // 使用创建时间进行排序
                        .influencerCount(influencers.size())
                        .influencers(influencers)
                        .build();
                result.add(groupVo);
            }
        }

        // 13. 排序：默认分组排在第一位，其他分组按创建时间倒序
        result.sort(Comparator.comparing(InfluencerSubscriptionGroupVo::getCreateTime).reversed());
        return result;
    }

    /**
     * 构建达人订阅VO对象
     *
     * @param subscription 订阅实体
     * @param influencer   达人信息实体
     * @param industryMap  行业信息Map
     * @param userDtoMap   用户信息Map
     * @param statistics   视频统计数据
     * @return 达人订阅VO
     */
    private InfluencerSubscriptionVo buildInfluencerSubscriptionVo(
            VideoUserInfluencerSubscriptionEntity subscription,
            VideoInfluencerInfoEntity influencer,
            Map<Long, TradeVo> industryMap,
            Map<Long, UserDto> userDtoMap,
            VideoStatistics statistics) {

        if (influencer == null) {
            log.warn("达人信息不存在，订阅ID: {}, 达人ID: {}", subscription.getId(), subscription.getInfluencerId());
            return null;
        }

        // 从industryMap中获取行业名称
        String industryName = getIndustryNameFromMap(subscription.getIndustryId(), industryMap);
        // 从userId中获取操作人
        UserDto userDto = userDtoMap.get(subscription.getUserId());

        // 使用传入的统计数据（已优化N+1查询）
        if (statistics == null) {
            statistics = new VideoStatistics(0, null, 0, null, 0);
        }
        boolean isToday = Objects.nonNull(influencer.getLastSyncTime()) && influencer.getLastSyncTime().toLocalDate().equals(LocalDate.now());
        return InfluencerSubscriptionVo.builder()
                .subscriptionId(subscription.getId())
                .influencerId(influencer.getId())
                .platformType(influencer.getPlatformType())
                .platformUserId(influencer.getPlatformUserId())
                .platformAccount(influencer.getPlatformAccount())
                .avatar(influencer.getAvatar())
                .nickname(influencer.getNickname())
                .industry(industryName)
                .industryId(subscription.getIndustryId())
                .groupId(subscription.getGroupId())
                .autoSyncEnabled(subscription.getIsEnabled() == 1)
                .likeCountThreshold(subscription.getLikeCountThreshold())
                .updateTimeCondition(subscription.getUpdateTimeCondition())
                .videoCount(statistics.totalVideoCount()) // 使用从videoInfo表统计的总视频数，避免N+1查询
                .todayIncrement(statistics.todayIncrement())
                .todaySyncTime(isToday ? influencer.getLastSyncTime() : null)
                .threeDayIncrement(statistics.threeDayIncrement())
                .threeDayIncrementTime(influencer.getLastSyncTime())
                .operator(Objects.nonNull(userDto) ? userDto.getNickName() : "未知") // 获取当前用户名
                .userId(subscription.getUserId())
                .build();
    }

    /**
     * 从行业Map中获取行业名称
     *
     * @param industryId  行业ID
     * @param industryMap 行业信息Map
     * @return 行业名称
     */
    private String getIndustryNameFromMap(Long industryId, Map<Long, TradeVo> industryMap) {
        if (industryId == null) {
            return "未知行业";
        }

        // 从industryMap中获取行业名称，逻辑和分组名称获取相同
        TradeVo trade = industryMap.get(industryId);
        if (trade != null && StrUtil.isNotBlank(trade.getName())) {
            return trade.getName();
        }

        // 如果industryMap中没有找到，使用默认映射作为兜底
        return "未知行业";
    }

    /**
     * 批量获取达人视频统计数据（优化N+1查询）
     *
     * @param influencerIds 达人ID列表
     * @return 达人ID -> 视频统计数据的映射
     */
    private Map<Long, VideoStatistics> batchGetVideoStatistics(List<Long> influencerIds) {
        if (CollectionUtil.isEmpty(influencerIds)) {
            return new HashMap<>();
        }

        // 类型转换：Long -> String，匹配VideoInfoEntity.authorId的String类型
        List<String> authorIds = influencerIds.stream()
                .map(String::valueOf)
                .toList();

        LocalDate now = LocalDate.now();
        LocalDate threeDaysAgo = now.minusDays(2);

        // 1. 批量查询总视频数量（避免N+1查询）
        Map<String, Long> totalCountMap = videoInfoService.list(new LambdaQueryWrapper<VideoInfoEntity>()
                        .in(VideoInfoEntity::getAuthorId, authorIds)
                        .select(VideoInfoEntity::getAuthorId)) // 只查询authorId字段，提高性能
                .stream()
                .collect(Collectors.groupingBy(VideoInfoEntity::getAuthorId,
                        Collectors.counting()));

        // 2. 批量查询今日视频数量
        Map<String, Long> todayCountMap = videoInfoService.list(new LambdaQueryWrapper<VideoInfoEntity>()
                        .in(VideoInfoEntity::getAuthorId, authorIds)
                        .ge(VideoInfoEntity::getPublishTime, now.atStartOfDay())
                        .le(VideoInfoEntity::getPublishTime, now.atTime(LocalTime.MAX))
                        .select(VideoInfoEntity::getAuthorId)) // 只查询authorId字段，提高性能
                .stream()
                .collect(Collectors.groupingBy(VideoInfoEntity::getAuthorId,
                        Collectors.counting()));

        // 3. 批量查询3日视频数量
        Map<String, Long> threeDayCountMap = videoInfoService.list(new LambdaQueryWrapper<VideoInfoEntity>()
                        .in(VideoInfoEntity::getAuthorId, authorIds)
                        .ge(VideoInfoEntity::getPublishTime, threeDaysAgo.atStartOfDay())
                        .le(VideoInfoEntity::getPublishTime, now.atTime(LocalTime.MAX))
                        .select(VideoInfoEntity::getAuthorId)) // 只查询authorId字段，提高性能
                .stream()
                .collect(Collectors.groupingBy(VideoInfoEntity::getAuthorId,
                        Collectors.counting()));

        // 4. 构建结果Map（转换回Long类型的key）
        Map<Long, VideoStatistics> result = new HashMap<>();
        for (Long influencerId : influencerIds) {
            String authorIdStr = String.valueOf(influencerId);
            int totalCount = totalCountMap.getOrDefault(authorIdStr, 0L).intValue();
            int todayCount = todayCountMap.getOrDefault(authorIdStr, 0L).intValue();
            int threeDayCount = threeDayCountMap.getOrDefault(authorIdStr, 0L).intValue();
            result.put(influencerId, new VideoStatistics(todayCount, null, threeDayCount, null, totalCount));
        }

        return result;
    }

    /**
     * 获取当前用户订阅达人列表
     *
     * @return
     */
    public List<VideoInfluencerSubscriptionVo> getVideoInfluencerSubscriptionList(Long influencerId) {
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户信息无效");

        List<VideoUserInfluencerSubscriptionEntity> entityList = videoUserInfluencerSubscriptionService.list(new LambdaQueryWrapper<>(VideoUserInfluencerSubscriptionEntity.class)
                .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
                .eq(VideoUserInfluencerSubscriptionEntity::getUserId, userId)
                .eq(ObjectUtil.isNotEmpty(influencerId), VideoUserInfluencerSubscriptionEntity::getInfluencerId, influencerId)
        );
        if (CollectionUtil.isEmpty(entityList)) {
            return List.of();
        }
        List<Long> influencerIds = entityList.stream().map(VideoUserInfluencerSubscriptionEntity::getInfluencerId).toList();
        if (CollectionUtil.isEmpty(influencerIds)) {
            return List.of();
        }
        List<VideoInfluencerInfoEntity> influencerInfoEntities = videoInfluencerInfoService.listByIds(influencerIds);
        if (CollectionUtil.isEmpty(influencerInfoEntities)) {
            return List.of();
        }
        Map<Long, VideoInfluencerInfoEntity> influencerInfoEntityMap = influencerInfoEntities.stream()
                .collect(Collectors.toMap(VideoInfluencerInfoEntity::getId, Function.identity(), (existing, replacement) -> existing));
        List<VideoInfluencerSubscriptionVo> result = new ArrayList<>();
        entityList.forEach(item -> {
            VideoInfluencerSubscriptionVo influencerSubscriptionVo = BeanConvertUtils.convert(item, VideoInfluencerSubscriptionVo.class);
            influencerSubscriptionVo.setVideoInfluencerInfoVo(influencerInfoEntityMap.get(item.getInfluencerId()));
            result.add(influencerSubscriptionVo);
        });
        return result;
    }

    /**
     * 视频统计数据记录
     */
    private record VideoStatistics(Integer todayIncrement, LocalDateTime todaySyncTime,
                                   Integer threeDayIncrement, LocalDateTime threeDayIncrementTime,
                                   Integer totalVideoCount) {
    }

    /**
     * 验证行业ID是否存在
     *
     * @param industryId 行业ID
     */
    private void validateIndustryExists(Long industryId) {
        if (industryId == null) {
            throw new IllegalArgumentException("行业ID不能为空");
        }

        // 调用行业服务验证行业是否存在
        List<TradeVo> tradeVos = tradeFeign.listTradeByIds(List.of(industryId));
        if (CollectionUtil.isEmpty(tradeVos)) {
            throw new IllegalArgumentException("行业ID不存在: " + industryId);
        }
    }

    /**
     * 验证分组ID是否存在，并且如果用户是子账号需判断分组是否是自己的
     *
     * @param groupId  分组ID
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param userType 用户类型：0-普通用户, 1-后台管理员, 2-子账号
     */
    private void validateGroupExists(Long groupId, Long userId, Long tenantId, Integer userType) {
        if (groupId == null) {
            return; // null表示默认分组，无需验证
        }

        // 查询分组是否存在
        VideoUserSubscriptionGroupEntity group = videoUserSubscriptionGroupService.getById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("分组ID不存在: " + groupId);
        }

        // 验证租户权限
        if (!Objects.equals(group.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("无权限访问该分组: " + groupId);
        }

        // 如果是子账号，需要验证分组是否属于自己
        if (userType != null && userType == 2) {
            if (!Objects.equals(group.getUserId(), userId)) {
                throw new IllegalArgumentException("子账号无权限访问该分组: " + groupId);
            }
        }
    }

    /**
     * 后台管理-根据用户ID查询达人订阅信息
     *
     * @param userId 用户ID
     * @return 达人订阅信息列表
     * @author RayChou
     * @date 2025-11-04
     */
    public List<AdminInfluencerSubscriptionVo> getInfluencerSubscriptionsByUserId(Long userId) {
        // 1. 查询用户的所有达人订阅
        List<VideoUserInfluencerSubscriptionEntity> subscriptions = videoUserInfluencerSubscriptionService.list(
                new LambdaQueryWrapper<VideoUserInfluencerSubscriptionEntity>()
                        .eq(VideoUserInfluencerSubscriptionEntity::getUserId, userId)
                        .orderByDesc(VideoUserInfluencerSubscriptionEntity::getCreatedDate)
        );

        if (CollectionUtil.isEmpty(subscriptions)) {
            return List.of();
        }

        // 2. 收集所有达人ID和行业ID
        Set<Long> influencerIds = subscriptions.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getInfluencerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> industryIds = subscriptions.stream()
                .map(VideoUserInfluencerSubscriptionEntity::getIndustryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 批量查询达人信息
        Map<Long, VideoInfluencerInfoEntity> influencerMap = new HashMap<>();
        if (!influencerIds.isEmpty()) {
            List<VideoInfluencerInfoEntity> influencers = videoInfluencerInfoService.listByIds(influencerIds);
            if (CollectionUtil.isNotEmpty(influencers)) {
                influencerMap = influencers.stream()
                        .collect(Collectors.toMap(VideoInfluencerInfoEntity::getId, Function.identity()));
            }
        }

        // 4. 批量查询行业信息
        Map<Long, String> industryNameMap = new HashMap<>();
        if (!industryIds.isEmpty()) {
            List<TradeVo> tradeVos = tradeFeign.listTradeByIds(new ArrayList<>(industryIds));
            if (CollectionUtil.isNotEmpty(tradeVos)) {
                industryNameMap = tradeVos.stream()
                        .collect(Collectors.toMap(TradeVo::getId, TradeVo::getName));
            }
        }

        // 5. 批量查询视频数量（一次性查询，避免N+1问题）
        Map<Long, Integer> videoCountMap = new HashMap<>();
        if (!influencerIds.isEmpty()) {
            // 将 influencerId 转换为字符串列表（VideoInfoEntity.authorId 存储的是 influencer_id）
            List<String> authorIds = influencerIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            // 批量查询视频数量（一次SQL查询）
            Map<String, Integer> authorVideoCountMap = videoInfoService.batchCountVideoNumberByAuthorIds(authorIds);

            // 将结果映射回 influencerId（Long 类型）
            for (Long influencerId : influencerIds) {
                String authorId = String.valueOf(influencerId);
                Integer count = authorVideoCountMap.getOrDefault(authorId, 0);
                videoCountMap.put(influencerId, count);
            }
        }

        // 6. 构建返回结果
        Map<Long, VideoInfluencerInfoEntity> finalInfluencerMap = influencerMap;
        Map<Long, String> finalIndustryNameMap = industryNameMap;
        return subscriptions.stream()
                .map(subscription -> {
                    VideoInfluencerInfoEntity influencer = finalInfluencerMap.get(subscription.getInfluencerId());
                    Integer videoCount = videoCountMap.getOrDefault(subscription.getInfluencerId(), 0);

                    return AdminInfluencerSubscriptionVo.builder()
                            .subscriptionId(subscription.getId())
                            .nickname(influencer != null ? influencer.getNickname() : "未知")
                            .platformType(influencer != null ? influencer.getPlatformType() : 1)
                            .platformUserId(influencer != null ? influencer.getPlatformUserId() : null)
                            .platformAccount(influencer != null ? influencer.getPlatformAccount() : "未知")
                            .industryName(finalIndustryNameMap.getOrDefault(subscription.getIndustryId(), "未知"))
                            .isEnabled(subscription.getIsEnabled())
                            .likeCountThreshold(subscription.getLikeCountThreshold())
                            .updateTimeCondition(subscription.getUpdateTimeCondition())
                            .videoCount(videoCount)
                            .createdDate(subscription.getCreatedDate())
                            .build();
                })
                .collect(Collectors.toList());
    }

}
