package com.jiuyu.replay.video.project.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.video.project.dao.VideoInfluencerInfoDao;
import com.jiuyu.replay.video.project.entity.VideoInfluencerDailyDataEntity;
import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import com.jiuyu.replay.video.project.service.VideoInfluencerDailyDataService;
import com.jiuyu.replay.video.project.service.VideoInfluencerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author RayChou
 * @date 2025/8/18 16:05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoInfluencerInfoServiceImpl extends ServiceImpl<VideoInfluencerInfoDao, VideoInfluencerInfoEntity> implements VideoInfluencerInfoService {

    private final RedissonClient redissonClient;
    private final VideoInfluencerDailyDataService videoInfluencerDailyDataService;

    /**
     * 批量保存或更新达人数据（使用分布式锁保证原子性）
     *
     * @param infoEntities 达人信息实体列表
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void batchSaveOrUpdate(List<VideoInfluencerInfoEntity> infoEntities) {
        if (CollectionUtil.isEmpty(infoEntities)) {
            return;
        }

        // 构建锁的key集合，基于平台类型和平台用户ID
        Set<String> lockKeys = infoEntities.stream()
                .map(entity -> buildLockKey(entity.getPlatformType(), entity.getPlatformUserId()))
                .collect(Collectors.toSet());

        // 创建多个锁
        RLock[] locks = lockKeys.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);

        // 使用 MultiLock 确保所有锁都获取成功
        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean lockAcquired = false;

        try {
            // 尝试获取所有锁，最多等待10秒，锁定30秒后自动释放
            lockAcquired = multiLock.tryLock(10, 30, TimeUnit.SECONDS);

            if (!lockAcquired) {
                log.warn("获取分布式多重锁失败，lockKeys: {}, 数据量: {}", lockKeys, infoEntities.size());
                throw new BusinessException("达人信息正在处理中，请稍后重试");
            }
            // 执行批量保存或更新操作
            performBatchSaveOrUpdate(infoEntities);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式多重锁被中断，lockKeys: {}, 数据量: {}", lockKeys, infoEntities.size(), e);
            throw new BusinessException("操作被中断，请重试");
        } finally {
            // 只有成功获取锁的情况下才释放
            if (lockAcquired) {
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    log.error("释放分布式多重锁异常，lockKeys: {}", lockKeys, e);
                }
            }
        }
    }

    /**
     * 单个保存或更新达人数据（使用分布式锁保证原子性）
     *
     * @param infoEntity 达人信息实体
     * @return 保存或更新后的达人信息实体
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public VideoInfluencerInfoEntity singleSaveOrUpdate(VideoInfluencerInfoEntity infoEntity) {
        if (infoEntity == null) {
            throw new IllegalArgumentException("达人信息实体不能为空");
        }

        // 构建锁的key
        String lockKey = buildLockKey(infoEntity.getPlatformType(), infoEntity.getPlatformUserId());
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false;

        try {
            // 尝试获取锁，最多等待10秒，锁定30秒后自动释放
            lockAcquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

            if (!lockAcquired) {
                log.warn("获取分布式锁失败，lockKey: {}", lockKey);
                throw new BusinessException("达人信息正在处理中，请稍后重试");
            }
            // 执行单个保存或更新操作
            return performSingleSaveOrUpdate(infoEntity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，lockKey: {}", lockKey, e);
            throw new BusinessException("操作被中断，请重试");
        } finally {
            // 只有成功获取锁的情况下才释放
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("释放分布式锁异常，lockKey: {}", lockKey, e);
                }
            }
        }
    }

    /**
     * 执行实际的批量保存或更新操作
     *
     * @param infoEntities 达人信息实体列表
     */
    private void performBatchSaveOrUpdate(List<VideoInfluencerInfoEntity> infoEntities) {
        List<VideoInfluencerInfoEntity> insertList = new ArrayList<>();
        List<VideoInfluencerInfoEntity> updateList = new ArrayList<>();
        List<VideoInfluencerInfoEntity> allList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 先对输入数据去重，避免相同业务键的重复处理
        Map<String, VideoInfluencerInfoEntity> uniqueEntitiesMap = infoEntities.stream()
                .collect(Collectors.toMap(
                        entity -> buildBusinessKey(entity.getPlatformType(), entity.getPlatformUserId()),
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("发现重复的达人数据，保留最后一个，platformType: {}, platformUserId: {}",
                                    replacement.getPlatformType(), replacement.getPlatformUserId());
                            return replacement; // 保留最后一个
                        }
                ));

        // 创建业务键到原始实体列表的映射，用于后续ID回填（支持一对多）
        Map<String, List<VideoInfluencerInfoEntity>> businessKeyToOriginalEntitiesMap = new HashMap<>();
        for (VideoInfluencerInfoEntity entity : infoEntities) {
            String businessKey = buildBusinessKey(entity.getPlatformType(), entity.getPlatformUserId());
            businessKeyToOriginalEntitiesMap.computeIfAbsent(businessKey, k -> new ArrayList<>()).add(entity);
        }

        // 记录重复数据的统计信息
        businessKeyToOriginalEntitiesMap.forEach((businessKey, entities) -> {
            if (entities.size() > 1) {
                log.warn("[达人数据] 发现重复业务键的达人数据，业务键: {}, 数量: {}", businessKey, entities.size());
            }
        });

        // 2. 批量查询已存在的数据（避免N+1查询）
        Map<String, VideoInfluencerInfoEntity> existingEntitiesMap = batchLoadExistingInfluencerEntities(uniqueEntitiesMap.keySet());

        // 3. 分离新增和更新的数据（内存操作，无数据库查询）
        uniqueEntitiesMap.values().forEach(entity -> {
            String businessKey = buildBusinessKey(entity.getPlatformType(), entity.getPlatformUserId());
            VideoInfluencerInfoEntity existingEntity = existingEntitiesMap.get(businessKey);

            if (Objects.nonNull(existingEntity)) {
                // 更新现有记录
                BeanUtils.copyProperties(entity, existingEntity, "id", "createdDate", "updateDate", "isDeleted");
                // 未设置达人同步时间原因：未同步达人短视频数据-只是拉取了达人的基础信息
                //existingEntity.setLastSyncTime(now);
                existingEntity.setUpdateDate(now);
                updateList.add(existingEntity);
                allList.add(existingEntity);

                // 将ID回填到所有相同业务键的原始实体中
                List<VideoInfluencerInfoEntity> originalEntities = businessKeyToOriginalEntitiesMap.get(businessKey);
                if (originalEntities != null) {
                    for (VideoInfluencerInfoEntity originalEntity : originalEntities) {
                        originalEntity.setId(existingEntity.getId());
                    }
                }
            } else {
                // 新增记录
                entity.setId(SnowflakeManager.nextValue());
                entity.setCreatedDate(now);
                // 未设置达人同步时间原因：未同步达人短视频数据-只是拉取了达人的基础信息
                //entity.setLastSyncTime(now);
                entity.setUpdateDate(now);
                entity.setIsDeleted((byte) 0);
                insertList.add(entity);
                allList.add(entity);

                // 将ID回填到所有相同业务键的原始实体中
                List<VideoInfluencerInfoEntity> originalEntities = businessKeyToOriginalEntitiesMap.get(businessKey);
                if (originalEntities != null) {
                    for (VideoInfluencerInfoEntity originalEntity : originalEntities) {
                        originalEntity.setId(entity.getId());
                    }
                }
            }
        });

        // 执行批量更新
        if (CollectionUtil.isNotEmpty(updateList)) {
            updateBatchById(updateList);
        }

        // 执行批量新增
        if (CollectionUtil.isNotEmpty(insertList)) {
            saveBatch(insertList);
        }

        // 查询达人历史表中是否存在今日同步数据 如果存在进行更新如果不存在进行新增 采用批量更新或插入方式
        saveDailyDataBatch(allList);
    }

    /**
     * 执行实际的单个保存或更新操作
     *
     * @param infoEntity 达人信息实体
     * @return 保存或更新后的达人信息实体
     */
    private VideoInfluencerInfoEntity performSingleSaveOrUpdate(VideoInfluencerInfoEntity infoEntity) {
        VideoInfluencerInfoEntity existingEntity = getOne(
                new LambdaQueryWrapper<VideoInfluencerInfoEntity>()
                        .eq(VideoInfluencerInfoEntity::getPlatformType, infoEntity.getPlatformType())
                        .eq(VideoInfluencerInfoEntity::getPlatformUserId, infoEntity.getPlatformUserId())
        );
        VideoInfluencerInfoEntity result;
        if (Objects.nonNull(existingEntity)) {
            // 更新现有记录
            BeanUtils.copyProperties(infoEntity, existingEntity, "id", "createdDate", "updateDate", "isDeleted");
            existingEntity.setUpdateDate(LocalDateTime.now());

            updateById(existingEntity);
            result = existingEntity;
        } else {
            // 新增记录
            infoEntity.setId(SnowflakeManager.nextValue());
            infoEntity.setCreatedDate(LocalDateTime.now());
            infoEntity.setUpdateDate(LocalDateTime.now());
            infoEntity.setIsDeleted((byte) 0);

            save(infoEntity);

            result = infoEntity;
        }

        // 查询达人历史表中是否存在今日同步数据 如果存在进行更新如果不存在进行新增
        saveDailyDataSingle(result);

        return result;
    }

    /**
     * 构建分布式锁的key
     *
     * @param platformType   平台类型
     * @param platformUserId 平台用户ID
     * @return 锁的key
     */
    private String buildLockKey(Byte platformType, String platformUserId) {
        String format = LockKeyPrefix.INFLUENCER.getLockKey("save_or_update:%d:%s");
        return String.format(format, platformType, platformUserId);
    }

    /**
     * 构建业务唯一键
     *
     * @param platformType   平台类型
     * @param platformUserId 平台用户ID
     * @return 业务唯一键
     */
    private String buildBusinessKey(Byte platformType, String platformUserId) {
        return platformType + ":" + platformUserId;
    }

    /**
     * 批量保存达人每日数据
     *
     * @param influencerEntities 达人信息实体集合
     */
    private void saveDailyDataBatch(List<VideoInfluencerInfoEntity> influencerEntities) {
        if (CollectionUtil.isEmpty(influencerEntities)) {
            return;
        }

        LocalDate today = LocalDate.now();

        // 优化：批量处理，避免N+1查询，返回带操作类型的数据
        List<InfluencerDailyDataWithOperation> dataWithOperations = processInfluencerDailyDataBatch(influencerEntities, today);

        // 执行优化的批量保存或更新（避免MyBatis-Plus的N次SELECT查询）
        if (CollectionUtil.isNotEmpty(dataWithOperations)) {
            batchSaveOrUpdateInfluencerDailyData(dataWithOperations);
        }
    }

    /**
     * 批量处理达人每日数据（优化版本，避免N+1查询）
     *
     * @param influencerEntities 达人信息实体列表
     * @param dataDate           数据日期
     * @return 处理后的带操作类型的每日数据列表
     */
    private List<InfluencerDailyDataWithOperation> processInfluencerDailyDataBatch(List<VideoInfluencerInfoEntity> influencerEntities, LocalDate dataDate) {
        if (CollectionUtil.isEmpty(influencerEntities)) {
            return new ArrayList<>();
        }

        // 1. 过滤有效达人并获取ID
        List<VideoInfluencerInfoEntity> validInfluencers = influencerEntities.stream()
                .filter(influencer -> influencer != null && influencer.getId() != null)
                .toList();

        if (CollectionUtil.isEmpty(validInfluencers)) {
            return new ArrayList<>();
        }

        List<Long> influencerIds = validInfluencers.stream()
                .map(VideoInfluencerInfoEntity::getId)
                .toList();

        // 2. 批量预加载数据（只需2次数据库查询）
        Map<Long, VideoInfluencerDailyDataEntity> existingDataMap = loadExistingInfluencerDailyData(influencerIds, dataDate);
        Map<Long, VideoInfluencerDailyDataEntity> lastSyncDataMap = loadLastSyncInfluencerData(influencerIds, dataDate);

        // 3. 批量处理（内存操作，无数据库查询）
        List<InfluencerDailyDataWithOperation> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (VideoInfluencerInfoEntity influencer : validInfluencers) {
            try {
                InfluencerDailyDataWithOperation dataWithOp = processInfluencerDailyDataSingle(
                        influencer, dataDate, now,
                        existingDataMap.get(influencer.getId()),
                        lastSyncDataMap.get(influencer.getId())
                );
                result.add(dataWithOp);
            } catch (Exception e) {
                log.error("处理达人每日数据失败，达人ID: {}, 平台类型: {}, 平台用户ID: {}",
                        influencer.getId(), influencer.getPlatformType(), influencer.getPlatformUserId(), e);
            }
        }
        return result;
    }

    /**
     * 批量加载已存在的达人实体（避免N+1查询）
     * 使用XML查询替换MyBatis-Plus方式
     *
     * @param businessKeys 业务键集合，格式为"platformType:platformUserId"
     * @return 已存在的达人实体映射，key为业务键，value为达人实体
     */
    private Map<String, VideoInfluencerInfoEntity> batchLoadExistingInfluencerEntities(Set<String> businessKeys) {
        if (CollectionUtil.isEmpty(businessKeys)) {
            return new HashMap<>();
        }

        // 解析业务键，构建查询条件
        List<VideoInfluencerInfoDao.InfluencerQueryCondition> queryConditions = businessKeys.stream()
                .map(keyStr -> {
                    String[] parts = keyStr.split(":");
                    if (parts.length != 2) {
                        log.warn("无效的业务键格式: {}", keyStr);
                        return null;
                    }
                    return new VideoInfluencerInfoDao.InfluencerQueryCondition(Byte.valueOf(parts[0]), parts[1]);
                })
                .filter(Objects::nonNull)
                .toList();

        if (CollectionUtil.isEmpty(queryConditions)) {
            return new HashMap<>();
        }

        // 使用XML查询方式执行批量查询
        List<VideoInfluencerInfoEntity> existingEntities = baseMapper.batchQueryByPlatformAndUserIds(queryConditions);

        // 构建返回映射
        return existingEntities.stream()
                .collect(Collectors.toMap(
                        entity -> buildBusinessKey(entity.getPlatformType(), entity.getPlatformUserId()),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 批量加载今日已存在的达人数据
     */
    private Map<Long, VideoInfluencerDailyDataEntity> loadExistingInfluencerDailyData(List<Long> influencerIds, LocalDate dataDate) {
        List<VideoInfluencerDailyDataEntity> existingDataList = videoInfluencerDailyDataService.list(
                new LambdaQueryWrapper<VideoInfluencerDailyDataEntity>()
                        .in(VideoInfluencerDailyDataEntity::getInfluencerId, influencerIds)
                        .eq(VideoInfluencerDailyDataEntity::getDataDate, dataDate)
        );

        return existingDataList.stream()
                .collect(Collectors.toMap(VideoInfluencerDailyDataEntity::getInfluencerId, data -> data));
    }

    /**
     * 批量加载最近一次同步的达人数据
     */
    private Map<Long, VideoInfluencerDailyDataEntity> loadLastSyncInfluencerData(List<Long> influencerIds, LocalDate dataDate) {
        List<VideoInfluencerDailyDataEntity> lastSyncDataList =
                videoInfluencerDailyDataService.getLatestDailyDataByInfluencerIds(influencerIds, dataDate);

        return lastSyncDataList.stream()
                .collect(Collectors.toMap(VideoInfluencerDailyDataEntity::getInfluencerId, data -> data));
    }

    /**
     * 处理单个达人的每日数据（纯内存操作版本）
     * 返回带操作类型的数据，解决雪花ID判断问题
     */
    private InfluencerDailyDataWithOperation processInfluencerDailyDataSingle(
            VideoInfluencerInfoEntity influencer,
            LocalDate dataDate,
            LocalDateTime now,
            VideoInfluencerDailyDataEntity existingData,
            VideoInfluencerDailyDataEntity lastSyncData) {

        // 计算增量
        DailyDataIncrement increment = calculateDailyIncrement(influencer, lastSyncData);

        VideoInfluencerDailyDataEntity dailyData;
        boolean isInsert;

        if (existingData != null) {
            // 更新现有记录
            dailyData = existingData;
            isInsert = false;
        } else {
            // 创建新记录
            dailyData = new VideoInfluencerDailyDataEntity();
            dailyData.setId(SnowflakeManager.nextValue()); // 设置雪花ID
            dailyData.setInfluencerId(influencer.getId());
            dailyData.setDataDate(dataDate);
            dailyData.setCreatedDate(now);
            dailyData.setIsDeleted((byte) 0);
            isInsert = true; // 明确标记为新增
        }

        // 设置当天最新数据
        updateDailyDataFields(dailyData, influencer, increment, now);

        // 返回包装对象，包含数据和操作类型
        return new InfluencerDailyDataWithOperation(dailyData, isInsert);
    }

    /**
     * 单个保存达人每日数据
     *
     * @param influencerEntity 达人信息实体
     */
    private void saveDailyDataSingle(VideoInfluencerInfoEntity influencerEntity) {
        if (influencerEntity == null || influencerEntity.getId() == null) {
            log.warn("达人信息实体或ID为空，跳过每日数据保存");
            return;
        }

        LocalDate today = LocalDate.now();

        try {
            VideoInfluencerDailyDataEntity dailyData = processInfluencerDailyData(influencerEntity, today);
            if (dailyData != null) {
                videoInfluencerDailyDataService.saveOrUpdate(dailyData);
            }
        } catch (Exception e) {
            log.error("处理达人每日数据失败，达人ID: {}, 平台类型: {}, 平台用户ID: {}",
                    influencerEntity.getId(), influencerEntity.getPlatformType(), influencerEntity.getPlatformUserId(), e);
        }
    }

    /**
     * 处理达人每日数据（核心方法）
     *
     * @param influencerEntity 达人信息实体
     * @param dataDate         数据日期
     * @return 处理后的每日数据实体
     */
    private VideoInfluencerDailyDataEntity processInfluencerDailyData(VideoInfluencerInfoEntity influencerEntity, LocalDate dataDate) {
        if (influencerEntity == null || influencerEntity.getId() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // 查询今日是否已存在数据
        VideoInfluencerDailyDataEntity existingDailyData = videoInfluencerDailyDataService.getOne(
                new LambdaQueryWrapper<VideoInfluencerDailyDataEntity>()
                        .eq(VideoInfluencerDailyDataEntity::getInfluencerId, influencerEntity.getId())
                        .eq(VideoInfluencerDailyDataEntity::getDataDate, dataDate)
        );

        // 查询最近一次同步的数据用于计算增量（而不是固定前一天）
        VideoInfluencerDailyDataEntity lastSyncData = videoInfluencerDailyDataService.getOne(
                new LambdaQueryWrapper<VideoInfluencerDailyDataEntity>()
                        .eq(VideoInfluencerDailyDataEntity::getInfluencerId, influencerEntity.getId())
                        .lt(VideoInfluencerDailyDataEntity::getDataDate, dataDate)  // 小于当前日期
                        .orderByDesc(VideoInfluencerDailyDataEntity::getDataDate)   // 按日期倒序
                        .last("LIMIT 1")  // 只取最新的一条
        );

        // 计算增量（当前值 - 最近一次同步值）
        DailyDataIncrement increment = calculateDailyIncrement(influencerEntity, lastSyncData);

        VideoInfluencerDailyDataEntity dailyData;
        if (existingDailyData != null) {
            // 更新现有记录
            dailyData = existingDailyData;
        } else {
            // 创建新记录
            dailyData = new VideoInfluencerDailyDataEntity();
            dailyData.setId(SnowflakeManager.nextValue());
            dailyData.setInfluencerId(influencerEntity.getId());
            dailyData.setDataDate(dataDate);
            dailyData.setCreatedDate(now);
            dailyData.setIsDeleted((byte) 0);
        }

        // 设置当天最新数据
        updateDailyDataFields(dailyData, influencerEntity, increment, now);

        return dailyData;
    }

    /**
     * 计算每日数据增量
     * 基于最近一次同步的数据计算增量，而不是固定的前一天数据
     *
     * @param influencerEntity 达人信息实体
     * @param lastSyncData     最近一次同步的数据
     * @return 增量数据
     */
    private DailyDataIncrement calculateDailyIncrement(VideoInfluencerInfoEntity influencerEntity,
                                                       VideoInfluencerDailyDataEntity lastSyncData) {
        if (lastSyncData == null) {
            // 如果没有历史同步数据，增量就等于0（首次同步）
            return new DailyDataIncrement(
                    0L,
                    0L,
                    0, 0L
            );
        }

        Long followersIncrement = calculateIncrement(influencerEntity.getFollowersCount(), lastSyncData.getFollowersCount());
        Long followingIncrement = calculateIncrement(influencerEntity.getFollowingCount(), lastSyncData.getFollowingCount());
        Long videoIncrement = calculateIncrement(Long.valueOf(influencerEntity.getVideoCount()), Long.valueOf(lastSyncData.getVideoCount()));
        Long likeIncrement = calculateIncrement(influencerEntity.getLikeCount(), lastSyncData.getLikeCount());

        return new DailyDataIncrement(followersIncrement, followingIncrement, videoIncrement.intValue(), likeIncrement);
    }

    /**
     * 更新每日数据字段
     *
     * @param dailyData        每日数据实体
     * @param influencerEntity 达人信息实体
     * @param increment        增量数据
     * @param updateTime       更新时间
     */
    private void updateDailyDataFields(VideoInfluencerDailyDataEntity dailyData,
                                       VideoInfluencerInfoEntity influencerEntity,
                                       DailyDataIncrement increment,
                                       LocalDateTime updateTime) {
        // 设置当天最新数据
        dailyData.setFollowersCount(influencerEntity.getFollowersCount());
        dailyData.setFollowingCount(influencerEntity.getFollowingCount());
        dailyData.setVideoCount(influencerEntity.getVideoCount());
        dailyData.setLikeCount(influencerEntity.getLikeCount());

        // 设置增量数据
        dailyData.setFollowersIncrement(increment.followersIncrement());
        dailyData.setFollowingIncrement(increment.followingIncrement());
        dailyData.setVideoIncrement(increment.videoIncrement());
        dailyData.setLikeIncrement(increment.likeIncrement());

        // 设置元数据
        dailyData.setCollectionTime(updateTime);
        dailyData.setDataSource((byte) 3); // 3-API同步
        dailyData.setUpdatedDate(updateTime);
    }

    /**
     * 计算增量值
     *
     * @param currentValue  当前值
     * @param previousValue 之前值
     * @return 增量值
     */
    private Long calculateIncrement(Long currentValue, Long previousValue) {
        if (currentValue == null || previousValue == null) {
            return null;
        }
        return currentValue - previousValue;
    }

    /**
     * 每日数据增量内部类
     */
    private record DailyDataIncrement(Long followersIncrement, Long followingIncrement, Integer videoIncrement,
                                      Long likeIncrement) {
    }

    /**
     * 带操作类型的达人每日数据包装类
     * 用于解决雪花ID导致的新增/更新判断问题
     */
    private record InfluencerDailyDataWithOperation(VideoInfluencerDailyDataEntity data, boolean isInsert) {
    }

    /**
     * 优化的批量保存或更新达人每日数据
     * 避免MyBatis-Plus saveOrUpdateBatch的N次SELECT查询问题
     *
     * @param dataWithOperations 带操作类型的数据列表
     * @return 是否成功
     */
    private boolean batchSaveOrUpdateInfluencerDailyData(List<InfluencerDailyDataWithOperation> dataWithOperations) {
        if (CollectionUtil.isEmpty(dataWithOperations)) {
            return true;
        }

        try {
            // 分离新增和更新的数据
            List<VideoInfluencerDailyDataEntity> toInsert = new ArrayList<>();
            List<VideoInfluencerDailyDataEntity> toUpdate = new ArrayList<>();

            for (InfluencerDailyDataWithOperation dataWithOp : dataWithOperations) {
                if (dataWithOp.isInsert()) {
                    toInsert.add(dataWithOp.data());
                } else {
                    toUpdate.add(dataWithOp.data());
                }
            }

            // 批量插入新数据
            if (CollectionUtil.isNotEmpty(toInsert)) {
                boolean insertSuccess = videoInfluencerDailyDataService.saveBatch(toInsert);
                if (!insertSuccess) {
                    log.error("批量插入达人每日数据失败，数量: {}", toInsert.size());
                    return false;
                }
            }

            // 批量更新已有数据
            if (CollectionUtil.isNotEmpty(toUpdate)) {
                boolean updateSuccess = videoInfluencerDailyDataService.updateBatchById(toUpdate);
                if (!updateSuccess) {
                    log.error("批量更新达人每日数据失败，数量: {}", toUpdate.size());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("批量保存或更新达人每日数据异常", e);
            return false;
        }
    }
}
