package com.jiuyu.replay.video.project.producer;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.jiuyu.replay.video.project.bo.hotsearch.*;
import com.jiuyu.replay.video.project.entity.*;
import com.jiuyu.replay.video.project.service.*;
import com.jiuyu.replay.video.project.util.VideoCountBatchQueryUtil;
import com.jiuyu.replay.video.project.vo.admin.AdminHotSubscriptionVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchHistoryVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchSyncVideoVo;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchVideoInfoVo;
import com.jiuyu.replay.video.project.vo.subscription.VideoUserHotSubscriptionAllVo;
import com.jiuyu.replay.video.project.vo.subscription.VideoUserHotSubscriptionListVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 爆款搜索业务处理器
 *
 * @author RayChou
 * @date 2025-08-29
 * @description 爆款搜索相关业务逻辑处理，包括保存搜索数据、查询搜索结果等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoHotSearchProducer {

    private final VideoHotSearchSnapshotService videoHotSearchSnapshotService;
    private final VideoHotSearchRelationService videoHotSearchRelationService;
    private final VideoInfoService videoInfoService;
    private final VideoUserHotSubscriptionService videoUserHotSubscriptionService;
    private final VideoUserSubscriptionGroupService videoUserSubscriptionGroupService;
    private final UserFeign userFeign;
    private final TradeFeign tradeFeign;
    private final VideoHotSearchDailyDataService videoHotSearchDailyDataService;
    private final VideoHotSearchService videoHotSearchService;
    private final VideoHotSearchSyncService videoHotSearchSyncService;
    private final SystemKvService systemKvService;
    private final VideoHotSearchVideoService videoHotSearchVideoService;
    private final UserPropertyFeign userPropertyFeign;
    private final VideoCountBatchQueryUtil videoCountBatchQueryUtil;

    /**
     * 保存搜索爆款信息
     *
     * @param searchBO 搜索参数
     * @return 搜索快照id
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveSearchHotVideos(VideoHotSearchBo searchBO) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 创建搜索快照
        LocalDateTime createdDate = LocalDateTime.now();
        VideoHotSearchSnapshotEntity snapshot = VideoHotSearchSnapshotEntity.builder().id(SnowflakeManager.nextValue()).userId(userId).tenantId(tenantId).searchKeyword(searchBO.getSearchKeyword().trim()).platformType(searchBO.getPlatformType()).totalVideos(CollectionUtil.isNotEmpty(searchBO.getVideoList()) ? searchBO.getVideoList().size() : 0) // 搜索到的视频数量
                .searchTime(createdDate).createdDate(createdDate).updateDate(createdDate).build();

        if (CollectionUtil.isEmpty(searchBO.getVideoList())) {
            videoHotSearchSnapshotService.save(snapshot);
            return snapshot.getId();
        }

        // 保存视频信息、搜索快照和关联关系
        saveVideoInfoAndSnapshot(snapshot, searchBO, createdDate);

        // 扣减套餐资产
        boolean property = userPropertyFeign.useShortVideoProperty(userCacheVo.getId(), OrderEnums.commodityTypeCode.SEARCH_HOT_VIDEO_NUM.getCode(), -1L);
        if (!property) {
            throw new BusinessException("套餐资源不足！");
        }

        return snapshot.getId();
    }

    /**
     * 搜索爆款结果数据
     *
     * @param snapshotId 快照id
     * @param page       当前页
     * @param limit      每页条数
     * @return 返回搜索结果
     */
    public PageUtils<VideoHotSearchResultVo> searchHotVideos(Long snapshotId, Integer page, Integer limit, Byte sortCode, Byte sortSequence) {
        return queryHotSearchRecord(snapshotId, page, limit, sortCode, sortSequence);
    }

    /**
     * 根据查询条件获取搜索列表
     *
     * @param queryBo 查询条件
     * @return 返回搜索结果分页数据
     */
    public PageUtils<VideoHotSearchResultVo> getSearchList(VideoHotSearchListQueryBo queryBo) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证快照ID是否属于当前用户
        VideoHotSearchSnapshotEntity snapshot = videoHotSearchSnapshotService.getById(queryBo.getSnapshotId());
        if (snapshot == null || !snapshot.getUserId().equals(userId)) {
            return new PageUtils<>(queryBo.getPage(), queryBo.getLimit());
        }

        // 使用连表查询获取分页结果
        Page<VideoHotSearchResultVo> resultPage = videoHotSearchRelationService.queryHotSearchResultsWithJoin(queryBo, userId, tenantId);

        // 转换为PageUtils格式
        return new PageUtils<>(resultPage, resultPage.getRecords());
    }

    /**
     * 添加爆款订阅（分布式锁：同一租户下同一类型关键字）
     *
     * @param subscribeBo 订阅信息
     * @return 订阅ID
     */
    @CustomRedissonLock(key = "'replay:lock:hotSearch:subscribe:' + #args[2] + ':' + #args[0].platformType + ':' + #args[0].keyword")
    @Transactional(rollbackFor = Exception.class)
    public VideoUserHotSubscriptionEntity addSubscribe(VideoHotSubscriptionAddBo subscribeBo, UserCacheVo userCacheVo, Long tenantId) {

        Long userId = userCacheVo.getId();

        // 验证行业ID是否存在
        validateIndustryExists(subscribeBo.getIndustryId());

        // 验证分组ID是否存在（如果不为null）
        if (subscribeBo.getGroupId() != null) {
            validateGroupExists(subscribeBo.getGroupId(), userId, tenantId, userCacheVo.getUserType());
        }

        // 验证自动同步参数
        if (Objects.nonNull(subscribeBo.getAutoSyncEnabled()) && subscribeBo.getAutoSyncEnabled() == 1) {
            Assert.notNull(subscribeBo.getLikeCountThreshold(), "点赞阈值条件不能为空");
            Assert.notNull(subscribeBo.getUpdateTimeCondition(), "更新时间条件不能为空");
        } else {
            subscribeBo.setLikeCountThreshold(null);
            subscribeBo.setUpdateTimeCondition(null);
        }

        // 检查是否已存在相同的订阅
        LambdaQueryWrapper<VideoUserHotSubscriptionEntity> existsWrapper = new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>().eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId).eq(VideoUserHotSubscriptionEntity::getPlatformType, subscribeBo.getPlatformType()).eq(VideoUserHotSubscriptionEntity::getKeyword, subscribeBo.getKeyword());

        VideoUserHotSubscriptionEntity existingSubscription = videoUserHotSubscriptionService.getOne(existsWrapper);
        if (existingSubscription != null) {
            throw new BusinessException("已订阅该爆款！不能重复订阅");
        }

        // 构建订阅实体
        LocalDateTime now = LocalDateTime.now();
        VideoUserHotSubscriptionEntity subscription = new VideoUserHotSubscriptionEntity();
        subscription.setId(SnowflakeManager.nextValue());
        subscription.setUserId(userId);
        subscription.setTenantId(tenantId);
        subscription.setGroupId(subscribeBo.getGroupId());
        subscription.setKeyword(subscribeBo.getKeyword().trim());
        subscription.setPlatformType(subscribeBo.getPlatformType());
        subscription.setIndustryId(subscribeBo.getIndustryId());
        subscription.setSubscriptionLikeCountThreshold(subscribeBo.getLikeCountMin());
        subscription.setIsEnabled(subscribeBo.getAutoSyncEnabled());
        subscription.setLikeCountThreshold(subscribeBo.getLikeCountThreshold());
        subscription.setUpdateTimeCondition(subscribeBo.getUpdateTimeCondition());
        subscription.setMonitorFrequency(8); // 默认8小时监控频率
        subscription.setCreatedDate(now);
        subscription.setUpdateDate(now);
        subscription.setIsDeleted((byte) 0);

        // 保存订阅
        boolean save = videoUserHotSubscriptionService.save(subscription);
        if (save) {
            // 扣减套餐资产
            boolean property = userPropertyFeign.useShortVideoProperty(userCacheVo.getId(), OrderEnums.commodityTypeCode.SUBSCRIBE_HOT_VIDEO_NUM.getCode(), -1L);
            if (!property) {
                throw new BusinessException("套餐资源不足！");
            }
        }
        return subscription;
    }

    /**
     * 同步爆款搜索数据（C#客户端调用）
     *
     * @param syncDataBo 同步数据对象
     * @return 同步结果
     */
    public Boolean syncVideoHotSearchData(VideoHotSearchSyncDataBo syncDataBo) {
//        if (Objects.isNull(syncDataBo) || CollectionUtil.isEmpty(syncDataBo.getVideoList())) {
//            return false;
//        }
        // 在分布式锁保护下执行事务性同步流程
        // 手动设置为第三方同步过来的数据
        syncDataBo.setSnatchDataType((byte) 2);
        videoHotSearchSyncService.executeTransactionalSyncProcess(syncDataBo);
        return true;
    }

    /**
     * 查询订阅爆款列表
     *
     * @return 爆款订阅列表
     */
    public List<VideoUserHotSubscriptionListVo> getSubscriptionList() {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        if (Objects.isNull(tenantId) || Objects.equals(tenantId, 0L)) {
            return List.of();
        }

        // 查询租户的所有爆款订阅
        LambdaQueryWrapper<VideoUserHotSubscriptionEntity> queryWrapper = new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>().eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId).orderByDesc(VideoUserHotSubscriptionEntity::getCreatedDate);

        List<VideoUserHotSubscriptionEntity> subscriptionEntities = videoUserHotSubscriptionService.list(queryWrapper);
        if (CollectionUtil.isEmpty(subscriptionEntities)) {
            return List.of();
        }

        return processHotSubscriptionsByGroup(subscriptionEntities);
    }

    /**
     * 编辑爆款订阅
     *
     * @param editBo 编辑信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean editSubscription(VideoHotSubscriptionEditBo editBo) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证订阅是否存在且属于当前用户
        VideoUserHotSubscriptionEntity subscription = validateSubscriptionOwnership(editBo.getSubscriptionId(), userCacheVo, tenantId);

        // 验证行业ID是否存在 （如果不为null）
        if (editBo.getIndustryId() != null) {
            validateIndustryExists(editBo.getIndustryId());
        }

        // 验证分组ID是否存在（如果不为null）
        if (editBo.getGroupId() != null) {
            validateGroupExists(editBo.getGroupId(), userId, tenantId, userCacheVo.getUserType());
        }

        // 验证自动同步参数
        if (editBo.getAutoSyncEnabled() != null && editBo.getAutoSyncEnabled() == 1) {
            Assert.notNull(editBo.getLikeCountThreshold(), "点赞阈值条件不能为空");
            Assert.notNull(editBo.getUpdateTimeCondition(), "更新时间条件不能为空");
            subscription.setLikeCountThreshold(editBo.getLikeCountThreshold());
            subscription.setUpdateTimeCondition(editBo.getUpdateTimeCondition());
        } else {
            subscription.setLikeCountThreshold(null);
            subscription.setUpdateTimeCondition(null);
        }

        // 更新订阅信息
        subscription.setGroupId(editBo.getGroupId());
        subscription.setIndustryId(editBo.getIndustryId());
        subscription.setSubscriptionLikeCountThreshold(editBo.getLikeCountMin());
        subscription.setIsEnabled(editBo.getAutoSyncEnabled());
        subscription.setUpdateDate(LocalDateTime.now());

        // 保存更新
        return videoUserHotSubscriptionService.updateById(subscription);
    }

    /**
     * 删除爆款订阅
     *
     * @param subscriptionId 订阅ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSubscription(Long subscriptionId) {
        Assert.notNull(subscriptionId, "订阅ID不能为空");

        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证订阅是否存在且属于当前用户
        VideoUserHotSubscriptionEntity subscription = validateSubscriptionOwnership(subscriptionId, userCacheVo, tenantId);

        // 如果订阅时间在24小时内不能进行删除
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(24);
        if (subscription.getCreatedDate().isAfter(localDateTime)) {
            throw new BusinessException("新订阅爆款24小时内不能删除！");
        }
        // 逻辑删除
        boolean remove = videoUserHotSubscriptionService.removeById(subscription);
        if (remove) {
            // 增加套餐资产
            boolean property = userPropertyFeign.useShortVideoProperty(subscription.getUserId(), OrderEnums.commodityTypeCode.SUBSCRIBE_HOT_VIDEO_NUM.getCode(), 1L);
            if (!property) {
                throw new BusinessException("回退资源失败！");
            }
        }
        return remove;
    }

    /**
     * 分页查询搜索历史
     *
     * @param page  当前页
     * @param limit 每页条数
     * @return 返回搜索历史分页数据
     */
    public PageUtils<VideoHotSearchHistoryVo> getSearchHistory(Integer page, Integer limit) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 构建查询条件
        LambdaQueryWrapper<VideoHotSearchSnapshotEntity> queryWrapper = new LambdaQueryWrapper<VideoHotSearchSnapshotEntity>().eq(VideoHotSearchSnapshotEntity::getTenantId, tenantId).eq(VideoHotSearchSnapshotEntity::getUserId, userId).ge(VideoHotSearchSnapshotEntity::getSearchTime, LocalDateTime.now().minusDays(7)).orderByDesc(VideoHotSearchSnapshotEntity::getSearchTime);

        // 分页查询搜索快照
        Page<VideoHotSearchSnapshotEntity> snapshotPage = new Page<>(page, limit);
        Page<VideoHotSearchSnapshotEntity> resultPage = videoHotSearchSnapshotService.page(snapshotPage, queryWrapper);

        if (CollectionUtil.isEmpty(resultPage.getRecords())) {
            return new PageUtils<>(resultPage, Collections.emptyList());
        }

        // 构建返回结果
        List<VideoHotSearchHistoryVo> resultList = resultPage.getRecords().stream().map(snapshot -> VideoHotSearchHistoryVo.builder().historyId(snapshot.getId()).keyword(snapshot.getSearchKeyword()).resultCount(snapshot.getTotalVideos()).searchTime(snapshot.getSearchTime().toLocalDate()).build()).collect(Collectors.toList());

        return new PageUtils<>(resultPage, resultList);
    }

    /**
     * 查询爆款搜索记录
     *
     * @param snapshotId 快照id，为null时查询所有记录
     * @param page       当前页
     * @param limit      每页条数
     * @return 分页结果
     */
    private PageUtils<VideoHotSearchResultVo> queryHotSearchRecord(Long snapshotId, Integer page, Integer limit, Byte sortCode, Byte sortSequence) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证搜索快照是否属于当前用户
        VideoHotSearchSnapshotEntity snapshot = videoHotSearchSnapshotService.getById(snapshotId);
        if (snapshot == null || !snapshot.getUserId().equals(userId)) {
            return new PageUtils<>(page, limit);
        }

        // 使用XML查询方法，支持动态排序
        Page<VideoHotSearchResultVo> resultPage = videoHotSearchRelationService.queryHotSearchRecord(
                new Page<>(page, limit),
                snapshotId,
                tenantId,
                sortCode,
                sortSequence
        );

        return new PageUtils<>(resultPage, resultPage.getRecords());
    }

    /**
     * 保存视频信息、搜索快照和关联关系
     *
     * @param snapshot      搜索快照
     * @param searchBO      搜索业务对象
     * @param createdDate   创建时间
     */
    private void saveVideoInfoAndSnapshot(VideoHotSearchSnapshotEntity snapshot, VideoHotSearchBo searchBO, LocalDateTime createdDate) {
        // 构建Bo对象
        VideoHotSearchSyncDataBo hotSearchSyncDataBo = VideoHotSearchSyncDataBo.builder().platformType(searchBO.getPlatformType()).keyword(searchBO.getSearchKeyword()).videoList(new ArrayList<>()).build();
        List<VideoHotSearchSyncVideoVo> videoList = hotSearchSyncDataBo.getVideoList();
        searchBO.getVideoList().forEach(item -> {
            VideoHotSearchSyncVideoVo searchSyncVideoVo = VideoHotSearchSyncVideoVo.builder()
                    .platformVideoId(item.getPlatformVideoId())
                    .platformType(item.getPlatformType())
                    .videoHash(item.getVideoHash())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .coverUrl(item.getCoverUrl())
                    .videoUrl(item.getVideoUrl())
                    .authorId(item.getAuthorId())
                    .authorName(item.getAuthorName())
                    .duration(item.getDuration())
                    .publishTime(item.getPublishTime())
                    .likeCount(item.getLikeCount())
                    .commentCount(item.getCommentCount())
                    .shareCount(item.getShareCount())
                    .collectCount(item.getCollectCount())
                    .influencerPlatformType(item.getPlatformType())
                    .influencerPlatformUserId(item.getPlatformUserId())
                    .influencerNickname(item.getAuthorName())
                    .influencerAvatar(item.getInfluencerAvatar())
                    .influencerFollowersCount(item.getInfluencerFollowersCount()).build();
            videoList.add(searchSyncVideoVo);
        });
        // 1. 先批量保存视频信息
        videoHotSearchSyncService.executeTransactionalSyncProcess(hotSearchSyncDataBo);

        // 2.采用补偿机制采集数据 直接通过关键词查询前150条数据
        List<VideoHotSearchVideoEntity> videoHotSearchVideoEntities = videoHotSearchVideoService.listVideoHotSearchVideoByPlatformTypeAndKeyWord(searchBO.getPlatformType(), searchBO.getSearchKeyword(), 150);
        if (CollectionUtil.isNotEmpty(videoHotSearchVideoEntities)) {
            // 2.1 重新设置历史快照视频总数
            snapshot.setTotalVideos(videoHotSearchVideoEntities.size());

            // 2.2 反转集合用于精确排序
            Collections.reverse(videoHotSearchVideoEntities);

            // 2.4 构建关联关系实体列表（使用已生成的视频ID）
            List<VideoHotSearchRelationEntity> relationEntities = new ArrayList<>();
            for (int index = 0; index < videoHotSearchVideoEntities.size(); index++) {
                VideoHotSearchVideoEntity item = videoHotSearchVideoEntities.get(index);
                // 构建关联关系实体
                VideoHotSearchRelationEntity relationEntity = new VideoHotSearchRelationEntity();
                relationEntity.setId(SnowflakeManager.nextValue());
                relationEntity.setSnapshotId(snapshot.getId());
                relationEntity.setVideoId(item.getVideoId());
                relationEntity.setInfluencerPlatformType(item.getInfluencerPlatformType());
                relationEntity.setInfluencerPlatformUserId(item.getInfluencerPlatformUserId());
                relationEntity.setInfluencerNickname(item.getInfluencerNickname());
                relationEntity.setInfluencerAvatar(item.getInfluencerAvatar());
                relationEntity.setInfluencerFollowersCount(item.getInfluencerFollowersCount());
                relationEntity.setSortOrder(index + 1); // 排序从1开始
                relationEntity.setCreatedDate(createdDate);
                relationEntity.setUpdateDate(createdDate);
                relationEntity.setIsDeleted((byte) 0);
                relationEntities.add(relationEntity);
            }
            // 2.5 保存关联关系
            videoHotSearchRelationService.saveBatch(relationEntities);
        }

        // 3. 保存搜索快照
        videoHotSearchSnapshotService.save(snapshot);
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
            throw new IllegalArgumentException("分组ID不存在");
        }

        // 验证租户权限
        if (!Objects.equals(group.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("无权限访问该分组");
        }

        // 如果是子账号，需要验证分组是否属于自己
        if (userType != null && userType == 2) {
            if (!Objects.equals(group.getUserId(), userId)) {
                throw new IllegalArgumentException("子账号无权限访问该分组: " + groupId);
            }
        }
    }

    /**
     * 验证订阅所有权
     *
     * @param subscriptionId 订阅ID
     * @param userCacheVo    用户信息
     * @param tenantId       租户ID
     * @return 订阅实体
     */
    private VideoUserHotSubscriptionEntity validateSubscriptionOwnership(Long subscriptionId, UserCacheVo userCacheVo, Long tenantId) {
        Assert.notNull(subscriptionId, "订阅ID不能为空");

        LambdaQueryWrapper<VideoUserHotSubscriptionEntity> wrapper = new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>().eq(VideoUserHotSubscriptionEntity::getId, subscriptionId);

        VideoUserHotSubscriptionEntity subscription = videoUserHotSubscriptionService.getOne(wrapper);
        if (subscription == null) {
            throw new BusinessException("订阅不存在或无权限访问");
        }
        Assert.isTrue(Objects.equals(subscription.getTenantId(), tenantId), "订阅不存在");
        if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(subscription.getUserId(), userCacheVo.getId()), "订阅不存在");
        }

        return subscription;
    }

    /**
     * 按分组处理爆款订阅列表
     *
     * @param subscriptionEntities 订阅实体列表
     * @return 处理后的订阅列表
     */
    private List<VideoUserHotSubscriptionListVo> processHotSubscriptionsByGroup(List<VideoUserHotSubscriptionEntity> subscriptionEntities) {
        // 按分组ID分组
        Map<Long, List<VideoUserHotSubscriptionEntity>> groupedSubscriptions = subscriptionEntities.stream().collect(Collectors.groupingBy(entity -> entity.getGroupId() != null ? entity.getGroupId() : 0L, // null分组用0L表示
                LinkedHashMap::new, Collectors.toList()));

        // 获取所有分组ID（排除默认分组0L）
        Set<Long> groupIds = groupedSubscriptions.keySet().stream().filter(groupId -> !Objects.equals(groupId, 0L)).collect(Collectors.toSet());

        // 查询分组信息
        Map<Long, VideoUserSubscriptionGroupEntity> groupMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(groupIds)) {
            LambdaQueryWrapper<VideoUserSubscriptionGroupEntity> groupWrapper = new LambdaQueryWrapper<VideoUserSubscriptionGroupEntity>().in(VideoUserSubscriptionGroupEntity::getId, groupIds);

            List<VideoUserSubscriptionGroupEntity> groups = videoUserSubscriptionGroupService.list(groupWrapper);
            groupMap = groups.stream().collect(Collectors.toMap(VideoUserSubscriptionGroupEntity::getId, Function.identity(), (existing, replacement) -> existing));
        }

        // 获取所有行业ID
        List<Long> industryIds = subscriptionEntities.stream().map(VideoUserHotSubscriptionEntity::getIndustryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        // 查询行业信息
        Map<Long, TradeVo> industryMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(industryIds)) {
            List<TradeVo> tradeVos = tradeFeign.listTradeByIds(industryIds);
            if (CollectionUtil.isNotEmpty(tradeVos)) {
                industryMap = tradeVos.stream().collect(Collectors.toMap(TradeVo::getId, Function.identity(), (existing, replacement) -> existing));
            }
        }

        // 查询用户
        List<Long> userIds = subscriptionEntities.stream().map(VideoUserHotSubscriptionEntity::getUserId).distinct().toList();
        Map<Long, UserDto> userMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(userIds)) {
            List<UserDto> userDtoList = userFeign.listByIds(userIds);
            if (CollectionUtil.isNotEmpty(userDtoList)) {
                userMap = userDtoList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity(), (existing, replacement) -> existing));
            }
        }
        // 获取所有关键字搜索
        List<VideoHotSearchEntity> queryList = subscriptionEntities.stream().map(item -> VideoHotSearchEntity.builder().platformType(item.getPlatformType()).searchKeyword(item.getKeyword()).build()).toList();
        List<VideoHotSearchEntity> videoHotSearchEntityList = videoHotSearchService.listByPlatformTypeAndSearchKeywords(queryList);

        Map<String, VideoHotSearchEntity> videoHotSearchEntityMap = new HashMap<>();
        Map<Long, VideoHotSearchDailyDataEntity> dailyDataEntityMap = new HashMap<>();
        Map<Long, ThreeDayIncrementData> threeDayIncrementMap = new HashMap<>();

        if (CollectionUtil.isNotEmpty(videoHotSearchEntityList)) {
            List<Long> searchIds = videoHotSearchEntityList.stream().map(VideoHotSearchEntity::getId).toList();
            // 查询所有订阅关键字今日数据
            List<VideoHotSearchDailyDataEntity> dailyDataList = videoHotSearchDailyDataService.listDailyDataByKeywordsAndDataDate(searchIds, LocalDate.now());
            // 查询所有订阅关键字最近3天数据（用于累计增量计算）
            List<VideoHotSearchDailyDataEntity> recentThreeDaysDataList = videoHotSearchDailyDataService.listDailyDataByKeywordsAndDateRange(searchIds, LocalDate.now().minusDays(3), LocalDate.now());

            videoHotSearchEntityMap = videoHotSearchEntityList.stream().collect(Collectors.toMap(entity -> buildHotSearchKey(entity.getPlatformType(), entity.getSearchKeyword()), Function.identity(), (existing, replacement) -> existing));

            if (CollectionUtil.isNotEmpty(dailyDataList)) {
                dailyDataEntityMap = dailyDataList.stream().collect(Collectors.toMap(VideoHotSearchDailyDataEntity::getSearchId, Function.identity(), (existing, replacement) -> existing));
            }

            // 计算每个关键字最近3天的累计增量和最新更新时间
            threeDayIncrementMap = calculateThreeDayIncrements(recentThreeDaysDataList);
        }

        // 批量预加载视频数量数据（避免N+1查询）
        Map<String, Integer> videoCountCache = videoCountBatchQueryUtil.batchQueryVideoCountsByEntityMap(subscriptionEntities, videoHotSearchEntityMap);

        // 构建返回结果
        List<VideoUserHotSubscriptionListVo> result = new ArrayList<>();

        for (Map.Entry<Long, List<VideoUserHotSubscriptionEntity>> entry : groupedSubscriptions.entrySet()) {
            Long groupId = entry.getKey();
            List<VideoUserHotSubscriptionEntity> groupSubscriptions = entry.getValue();

            // 获取分组名称和创建时间，0L表示原始数据中groupId为null的默认分组
            String groupName;
            LocalDateTime createTime;
            if (groupId == 0L) {
                groupName = "默认分组"; // null表示默认分组
                createTime = LocalDateTime.MAX; // 设置为最大时间，确保排在第一位
            } else {
                VideoUserSubscriptionGroupEntity group = groupMap.get(groupId);
                groupName = group != null ? group.getGroupName() : "未知分组";
                createTime = group != null ? group.getCreatedDate() : LocalDateTime.MIN;
            }

            VideoUserHotSubscriptionListVo groupVo = buildSubscriptionGroupVo(groupId == 0L ? null : groupId, // 0L转回null，表示默认分组
                    groupName, createTime, groupSubscriptions, industryMap, dailyDataEntityMap, threeDayIncrementMap, videoHotSearchEntityMap, userMap, videoCountCache);
            result.add(groupVo);
        }

        // 排序：默认分组排在第一位，其他分组按创建时间倒序
        result.sort(Comparator.comparing(VideoUserHotSubscriptionListVo::getCreateTime).reversed());
        return result;
    }

    /**
     * 构建订阅分组VO
     *
     * @param groupId         分组ID
     * @param groupName       分组名称
     * @param createTime      创建时间
     * @param subscriptions   订阅列表
     * @param industryMap     行业信息映射
     * @param videoCountCache 视频数量缓存
     * @return 分组VO
     */
    private VideoUserHotSubscriptionListVo buildSubscriptionGroupVo(Long groupId, String groupName, LocalDateTime createTime, List<VideoUserHotSubscriptionEntity> subscriptions, Map<Long, TradeVo> industryMap, Map<Long, VideoHotSearchDailyDataEntity> dailyDataEntityMap, Map<Long, ThreeDayIncrementData> threeDayIncrementMap, Map<String, VideoHotSearchEntity> videoHotSearchEntityMap, Map<Long, UserDto> userMap, Map<String, Integer> videoCountCache) {
        VideoUserHotSubscriptionListVo groupVo = new VideoUserHotSubscriptionListVo();
        groupVo.setGroupId(groupId);
        groupVo.setGroupName(groupName);
        groupVo.setCreateTime(createTime);

        // 构建订阅详情列表
        List<VideoUserHotSubscriptionListVo.HotSubscriptionItemVo> subscriptionDetails = subscriptions.stream().map(subscription -> {
            VideoUserHotSubscriptionListVo.HotSubscriptionItemVo detail = new VideoUserHotSubscriptionListVo.HotSubscriptionItemVo();
            detail.setSubscriptionId(subscription.getId());
            detail.setKeyword(subscription.getKeyword());
            detail.setPlatformType(subscription.getPlatformType());
            // 设置行业名称
            TradeVo tradeVo = industryMap.get(subscription.getIndustryId());
            detail.setIndustry(tradeVo != null ? tradeVo.getName() : "未知行业");
            detail.setIndustryId(subscription.getIndustryId());
            detail.setGroupId(subscription.getGroupId());
            detail.setSubscriptionLikeCountThreshold(subscription.getSubscriptionLikeCountThreshold());
            detail.setAutoSyncEnabled(subscription.getIsEnabled());
            detail.setLikeCountThreshold(subscription.getLikeCountThreshold());
            detail.setUpdateTimeCondition(subscription.getUpdateTimeCondition());

            VideoHotSearchEntity videoHotSearchEntity = videoHotSearchEntityMap.get(buildHotSearchKey(subscription.getPlatformType(), subscription.getKeyword()));

            if (Objects.nonNull(videoHotSearchEntity)) {
                // 从缓存中获取视频数量，避免N+1查询
                detail.setVideoCount(videoCountBatchQueryUtil.getVideoCount(videoCountCache, videoHotSearchEntity.getId(), subscription.getSubscriptionLikeCountThreshold()));
                VideoHotSearchDailyDataEntity videoHotSearchDailyDataEntity = dailyDataEntityMap.get(videoHotSearchEntity.getId());
                if (Objects.nonNull(videoHotSearchDailyDataEntity)) {
                    detail.setTodayIncrement(videoHotSearchDailyDataEntity.getVideoIncrement());
                    detail.setTodaySyncTime(videoHotSearchDailyDataEntity.getCollectionTime());
                } else {
                    detail.setTodayIncrement(0);
                }
                // 使用累计增量方式计算3日增量
                ThreeDayIncrementData threeDayData = threeDayIncrementMap.get(videoHotSearchEntity.getId());
                if (Objects.nonNull(threeDayData)) {
                    detail.setThreeDayIncrement(threeDayData.increment());
                    detail.setThreeDayIncrementTime(threeDayData.updateTime());
                } else {
                    detail.setThreeDayIncrement(0);
                    detail.setThreeDayIncrementTime(null);
                }
            } else {
                detail.setVideoCount(0);
                detail.setTodayIncrement(0);
                detail.setThreeDayIncrement(0);
            }
            UserDto userDto = userMap.get(subscription.getUserId());
            detail.setOperator(userDto != null ? userDto.getNickName() : "未知");
            detail.setUserId(subscription.getUserId());
            return detail;
        }).collect(Collectors.toList());

        groupVo.setHotSubscriptionItemVos(subscriptionDetails);
        return groupVo;
    }


    /**
     * 获取当前用户订阅列表
     *
     * @return
     */
    public List<VideoUserHotSubscriptionAllVo> getSubscriptionListForClient() {
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户信息无效");

        List<VideoUserHotSubscriptionEntity> entityList = videoUserHotSubscriptionService.list(new LambdaQueryWrapper<>(VideoUserHotSubscriptionEntity.class).eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId).eq(VideoUserHotSubscriptionEntity::getUserId, userId));
        if (CollectionUtil.isEmpty(entityList)) {
            return List.of();
        }
        List<VideoUserHotSubscriptionAllVo> result = new ArrayList<>();
        entityList.forEach(item -> {
            VideoUserHotSubscriptionAllVo influencerSubscriptionVo = BeanConvertUtils.convert(item, VideoUserHotSubscriptionAllVo.class);
            result.add(influencerSubscriptionVo);
        });
        return result;
    }

    /**
     * 构建查找key
     *
     * @param platformType
     * @param keyword
     * @return
     */
    private String buildHotSearchKey(Byte platformType, String keyword) {
        return platformType + "_" + keyword;
    }

    /**
     * 根据平台类型和关键字获取视频信息
     *
     * @param platformType 平台类型
     * @param keyword      关键字
     * @return
     */
    public List<VideoHotSearchVideoInfoVo> getVideoListByPlatformTypeAndKeyword(Byte platformType, String keyword) {
        SystemKvEntity syncShortVideoInterval = systemKvService.getByKey("sync_short_video_min_interval");
        if (Objects.isNull(syncShortVideoInterval)) {
            return List.of();
        }

        VideoHotSearchEntity hotSearchEntities = videoHotSearchService.getOne(new LambdaQueryWrapper<>(VideoHotSearchEntity.class).eq(VideoHotSearchEntity::getPlatformType, platformType).eq(VideoHotSearchEntity::getSearchKeyword, keyword.trim()));

        if (Objects.isNull(hotSearchEntities)) {
            return List.of();
        }
        if (Objects.equals(syncShortVideoInterval.getKvValue(), "-1")) {
            return buildVideoHotSearchVideoInfoVoData(hotSearchEntities.getId());
        }
        // 超过同步时间需要同步
        boolean after = LocalDateTime.now().minusSeconds(Long.parseLong(syncShortVideoInterval.getKvValue())).isAfter(hotSearchEntities.getLastSyncTime());
        if (after) {
            return List.of();
        }
        return buildVideoHotSearchVideoInfoVoData(hotSearchEntities.getId());
    }

    /**
     * 组装返回数据
     *
     * @param searchId 搜索ID
     */
    private List<VideoHotSearchVideoInfoVo> buildVideoHotSearchVideoInfoVoData(Long searchId) {
        List<VideoHotSearchVideoEntity> videoHotSearchVideoEntities = videoHotSearchVideoService.list(new LambdaQueryWrapper<>(VideoHotSearchVideoEntity.class).eq(VideoHotSearchVideoEntity::getSearchId, searchId));
        if (CollectionUtil.isEmpty(videoHotSearchVideoEntities)) {
            return List.of();
        }
        Set<Long> videoIds = videoHotSearchVideoEntities.stream().map(VideoHotSearchVideoEntity::getVideoId).collect(Collectors.toSet());

        List<VideoInfoEntity> videoInfoEntityList = videoInfoService.listByIds(videoIds);
        if (CollectionUtil.isEmpty(videoInfoEntityList)) {
            return Collections.emptyList();
        }
        Map<Long, VideoInfoEntity> videoInfoEntityMap = videoInfoEntityList.stream().collect(Collectors.toMap(VideoInfoEntity::getId, Function.identity(), (existing, replacement) -> existing));
        List<VideoHotSearchVideoInfoVo> videoHotSearchVideoInfoVos = new ArrayList<>();
        videoHotSearchVideoEntities.forEach(item -> {
            if (videoInfoEntityMap.containsKey(item.getVideoId())) {
                VideoInfoEntity videoInfoEntity = videoInfoEntityMap.get(item.getVideoId());
                VideoHotSearchVideoInfoVo videoHotSearchVideoInfoVo = BeanConvertUtils.convert(item, VideoHotSearchVideoInfoVo.class);
                BeanUtils.copyProperties(videoInfoEntity, videoHotSearchVideoInfoVo);
                videoHotSearchVideoInfoVos.add(videoHotSearchVideoInfoVo);
            }
        });
        return videoHotSearchVideoInfoVos;
    }

    /**
     * 查询视频列表根据订阅条件查询
     *
     * @param queryBo
     * @return
     */
    public PageUtils<VideoHotSearchResultVo> getVideoListBySubscriptionCondition(VideoHotSearchListSubscriptionQueryBo queryBo) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        // 获取订阅信息
        VideoUserHotSubscriptionEntity subscriptionEntity = videoUserHotSubscriptionService.getById(queryBo.getSubscriptionId());
        Assert.notNull(subscriptionEntity, "订阅不存在！");

        // 验证订阅ID是否属于当前租户
        if (!subscriptionEntity.getTenantId().equals(tenantId)) {
            return new PageUtils<>(queryBo.getPage(), queryBo.getLimit());
        }
        // 根据关键词查询爆款信息
        VideoHotSearchEntity hotSearchEntity = videoHotSearchService.getOne(new LambdaQueryWrapper<>(VideoHotSearchEntity.class)
                .eq(VideoHotSearchEntity::getPlatformType, subscriptionEntity.getPlatformType())
                .eq(VideoHotSearchEntity::getSearchKeyword, subscriptionEntity.getKeyword()));
        if (Objects.isNull(hotSearchEntity)) {
            return new PageUtils<>(queryBo.getPage(), queryBo.getLimit());
        }

        // 使用连表查询获取分页结果
        Page<VideoHotSearchResultVo> resultPage = videoInfoService.queryVideoListByIdsAndCondition(tenantId, hotSearchEntity.getId(), queryBo, subscriptionEntity.getSubscriptionLikeCountThreshold());

        // 转换为PageUtils格式
        return new PageUtils<>(resultPage, resultPage.getRecords());
    }

    /**
     * 查询爆款示例列表
     *
     * @param queryBo
     * @return
     */
    public PageUtils<VideoHotSearchResultVo> getVideoListByExampleCondition(VideoHotSearchListExampleQueryBo queryBo) {

        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 根据关键词查询爆款信息
        VideoHotSearchEntity hotSearchEntity = videoHotSearchService.getOne(new LambdaQueryWrapper<>(VideoHotSearchEntity.class)
                .eq(VideoHotSearchEntity::getPlatformType, queryBo.getPlatformType())
                .eq(VideoHotSearchEntity::getSearchKeyword, queryBo.getSearchKeyword()));
        if (Objects.isNull(hotSearchEntity)) {
            return new PageUtils<>(queryBo.getPage(), queryBo.getLimit());
        }
        // 转换实体类型
        VideoHotSearchListSubscriptionQueryBo subscriptionQueryBo = new VideoHotSearchListSubscriptionQueryBo();
        BeanUtils.copyProperties(queryBo, subscriptionQueryBo);
        // 使用连表查询获取分页结果
        Page<VideoHotSearchResultVo> resultPage = videoInfoService.queryVideoListByIdsAndCondition(tenantId, hotSearchEntity.getId(), subscriptionQueryBo, null);

        // 转换为PageUtils格式
        return new PageUtils<>(resultPage, resultPage.getRecords());
    }

    /**
     * 3日增量数据
     */
    private record ThreeDayIncrementData(int increment, LocalDateTime updateTime) {

    }

    /**
     * 计算每个关键字最近3天的累计增量
     * 基于累计增量方式，与达人数据计算逻辑保持一致
     *
     * @param recentThreeDaysDataList 最近3天的数据列表
     * @return 每个searchId对应的3日增量数据
     */
    private Map<Long, ThreeDayIncrementData> calculateThreeDayIncrements(List<VideoHotSearchDailyDataEntity> recentThreeDaysDataList) {
        if (CollectionUtil.isEmpty(recentThreeDaysDataList)) {
            return new HashMap<>();
        }

        // 按searchId分组
        Map<Long, List<VideoHotSearchDailyDataEntity>> groupedBySearchId = recentThreeDaysDataList.stream().collect(Collectors.groupingBy(VideoHotSearchDailyDataEntity::getSearchId));

        Map<Long, ThreeDayIncrementData> result = new HashMap<>();

        for (Map.Entry<Long, List<VideoHotSearchDailyDataEntity>> entry : groupedBySearchId.entrySet()) {
            Long searchId = entry.getKey();
            List<VideoHotSearchDailyDataEntity> dataList = entry.getValue();

            // 累加最近3天的增量
            int threeDayIncrement = dataList.stream().mapToInt(data -> data.getVideoIncrement() != null ? data.getVideoIncrement() : 0).sum();

            // 获取最近的更新时间（按日期倒序排列后取第一个）
            LocalDateTime updateTime = dataList.stream().max(Comparator.comparing(VideoHotSearchDailyDataEntity::getDataDate))  // 找最大日期
                    .map(VideoHotSearchDailyDataEntity::getCollectionTime).orElse(null);

            result.put(searchId, new ThreeDayIncrementData(threeDayIncrement, updateTime));
        }

        return result;
    }

    /**
     * 后台管理-根据用户ID查询爆款订阅信息
     *
     * @param userId 用户ID
     * @return 爆款订阅信息列表
     * @author RayChou
     * @date 2025-11-04
     */
    public List<AdminHotSubscriptionVo> getHotSubscriptionsByUserId(Long userId) {
        // 1. 查询用户的所有爆款订阅
        List<VideoUserHotSubscriptionEntity> subscriptions = videoUserHotSubscriptionService.list(
                new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>()
                        .eq(VideoUserHotSubscriptionEntity::getUserId, userId)
                        .orderByDesc(VideoUserHotSubscriptionEntity::getCreatedDate)
        );

        if (CollectionUtil.isEmpty(subscriptions)) {
            return List.of();
        }

        // 2. 收集所有行业ID
        Set<Long> industryIds = subscriptions.stream()
                .map(VideoUserHotSubscriptionEntity::getIndustryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 批量查询行业信息
        Map<Long, String> industryNameMap = new HashMap<>();
        if (!industryIds.isEmpty()) {
            List<TradeVo> tradeVos = tradeFeign.listTradeByIds(new ArrayList<>(industryIds));
            if (CollectionUtil.isNotEmpty(tradeVos)) {
                industryNameMap = tradeVos.stream()
                        .collect(Collectors.toMap(TradeVo::getId, TradeVo::getName));
            }
        }

        // 4. 批量查询爆款搜索ID（通过平台类型和关键词）
        List<VideoHotSearchEntity> queryList = subscriptions.stream()
                .map(subscription -> VideoHotSearchEntity.builder()
                        .platformType(subscription.getPlatformType())
                        .searchKeyword(subscription.getKeyword())
                        .build())
                .distinct()
                .collect(Collectors.toList());

        List<VideoHotSearchEntity> hotSearchEntities = videoHotSearchService.listByPlatformTypeAndSearchKeywords(queryList);
        final Map<String, VideoHotSearchEntity> hotSearchMap = CollectionUtil.isNotEmpty(hotSearchEntities)
                ? hotSearchEntities.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getPlatformType() + "_" + entity.getSearchKeyword(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ))
                : new HashMap<>();

        // 6. 构建返回结果
        Map<Long, String> finalIndustryNameMap = industryNameMap;
        return subscriptions.stream()
                .map(subscription -> {
                    String hotSearchKey = subscription.getPlatformType() + "_" + subscription.getKeyword();
                    VideoHotSearchEntity hotSearchEntity = hotSearchMap.get(hotSearchKey);
                    Integer videoCount = hotSearchEntity != null ? hotSearchEntity.getVideoCount() : 0;
                    return AdminHotSubscriptionVo.builder()
                            .subscriptionId(subscription.getId())
                            .keyword(subscription.getKeyword())
                            .industryName(finalIndustryNameMap.getOrDefault(subscription.getIndustryId(), "未知"))
                            .subscriptionLikeCountThreshold(subscription.getSubscriptionLikeCountThreshold())
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
