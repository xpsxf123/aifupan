package com.jiuyu.replay.video.project.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.shandard.Entry;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.video.common.enums.PlatformTypeEnum;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchListSubscriptionQueryBo;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.entity.VideoInfoDailyDataEntity;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.service.VideoInfoDailyDataService;
import com.jiuyu.replay.video.project.service.VideoInfoService;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
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
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频基础信息表服务实现类
 *
 * @author RayChou
 * @description 视频信息的业务逻辑实现，提供原子性的保存或更新操作
 * @since 2025-08-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoInfoServiceImpl extends ServiceImpl<VideoInfoDao, VideoInfoEntity> implements VideoInfoService {

    private final RedissonClient redissonClient;
    private final VideoInfoDailyDataService videoInfoDailyDataService;
    private final VideoInfoDao videoInfoDao;

    /**
     * 批量保存或更新视频信息（使用分布式锁保证原子性）
     *
     * @param videoEntities 视频信息实体列表
     * @param isSyncDaily   是否同步每日数据
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void batchSaveOrUpdate(List<VideoInfoEntity> videoEntities, Boolean isSyncDaily) {
        if (CollectionUtil.isEmpty(videoEntities)) {
            return;
        }

        // 构建锁的key集合，基于platformType和platformVideoId
        Set<String> lockKeys = videoEntities.stream()
                .filter(entity -> entity.getPlatformType() != null && StrUtil.isNotBlank(entity.getPlatformVideoId()))
                .map(entity -> buildLockKey(entity.getPlatformType(), entity.getPlatformVideoId()))
                .collect(Collectors.toSet());

        if (lockKeys.isEmpty()) {
            throw new IllegalArgumentException("视频信息中没有有效的platformType和platformVideoId");
        }

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
                log.warn("获取分布式多重锁失败，lockKeys: {}, 数据量: {}", lockKeys, videoEntities.size());
                throw new BusinessException("视频信息正在处理中，请稍后重试");
            }

            // 执行批量保存或更新操作
            performBatchSaveOrUpdate(videoEntities, isSyncDaily);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式多重锁被中断，lockKeys: {}, 数据量: {}", lockKeys, videoEntities.size(), e);
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
     * 单个保存或更新视频信息（使用分布式锁保证原子性）
     *
     * @param videoEntity 视频信息实体
     * @return 保存或更新后的视频信息实体
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public VideoInfoEntity singleSaveOrUpdate(VideoInfoEntity videoEntity) {
        if (videoEntity == null) {
            throw new IllegalArgumentException("视频信息实体不能为空");
        }
        if (videoEntity.getPlatformType() == null) {
            throw new IllegalArgumentException("platformType不能为空");
        }
        if (StrUtil.isBlank(videoEntity.getPlatformVideoId())) {
            throw new IllegalArgumentException("platformVideoId不能为空");
        }

        // 构建锁的key
        String lockKey = buildLockKey(videoEntity.getPlatformType(), videoEntity.getPlatformVideoId());
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false;

        try {
            // 尝试获取锁，最多等待10秒，锁定30秒后自动释放
            lockAcquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

            if (!lockAcquired) {
                log.warn("获取分布式锁失败，lockKey: {}", lockKey);
                throw new BusinessException("视频信息正在处理中，请稍后重试");
            }
            // 执行单个保存或更新操作
            return performSingleSaveOrUpdate(videoEntity);

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

    @Override
    public Page<VideoHotSearchResultVo> queryVideoListByIdsAndCondition(Long tenantId, Long searchId, VideoHotSearchListSubscriptionQueryBo queryBo, Integer subscriptionLikeCountThreshold) {
        // 创建分页对象
        Page<VideoHotSearchResultVo> page = new Page<>(queryBo.getPage(), queryBo.getLimit());
        if (queryBo.getPublishTimeValue() != null) {
            // 计算发布时间固定值
            LocalDate now = LocalDate.now();
            switch (queryBo.getPublishTimeValue()) {
                case -1: // 只看今天
                    queryBo.setPublishStartTime(now.atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                case 0: // 近三天
                    queryBo.setPublishStartTime(now.minusDays(2).atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                case 1: // 近一周
                    queryBo.setPublishStartTime(now.minusWeeks(1).atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                case 2: // 近半个月
                    queryBo.setPublishStartTime(now.minusDays(15).atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                case 3: // 近一个月
                    queryBo.setPublishStartTime(now.minusMonths(1).atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                case 4: // 近3个月
                    queryBo.setPublishStartTime(now.minusMonths(3).atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                case 5: // 近6个月
                    queryBo.setPublishStartTime(now.minusMonths(6).atStartOfDay());
                    queryBo.setPublishEndTime(null);
                    break;
                default:
                    queryBo.setPublishStartTime(null);
                    queryBo.setPublishEndTime(null);
            }
        }
        if (queryBo.getVideoUpdateTimeType() != null) {
            LocalDate now = LocalDate.now();
            LocalDateTime startTime = queryBo.getVideoUpdateTimeType() == 1 ? now.atStartOfDay() : now.minusDays(2).atStartOfDay();
            LocalDateTime endTime = now.atTime(LocalTime.MAX);
            return videoInfoDao.queryVideoListByIdsAndCondition(page, tenantId, searchId, queryBo, subscriptionLikeCountThreshold, startTime, endTime);
        }
        return videoInfoDao.queryVideoListByIdsAndCondition(page, tenantId, searchId, queryBo, subscriptionLikeCountThreshold, null, null);
    }

    @Override
    public Integer countVideoNumberBySearchIdAndLikeCount(Long searchId, Integer likeCount) {
        return videoInfoDao.countVideoNumberBySearchIdAndLikeCount(searchId, likeCount);
    }

    @Override
    public Map<String, Integer> batchCountVideoNumberBySearchIdAndLikeCount(List<VideoInfoDao.VideoCountQuery> queryParams) {
        if (CollectionUtil.isEmpty(queryParams)) {
            return new HashMap<>();
        }

        List<VideoInfoDao.VideoCountResult> results = videoInfoDao.batchCountVideoNumberBySearchIdAndLikeCount(queryParams);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> result.getSearchId() + ":" + result.getLikeCount(),
                        VideoInfoDao.VideoCountResult::getVideoCount
                ));
    }

    @Override
    public Map<String, Integer> batchCountVideoNumberByAuthorIds(List<String> authorIds) {
        if (CollectionUtil.isEmpty(authorIds)) {
            return new HashMap<>();
        }

        List<VideoInfoDao.AuthorVideoCountResult> results = videoInfoDao.batchCountVideoNumberByAuthorIds(authorIds);

        return results.stream()
                .collect(Collectors.toMap(
                        VideoInfoDao.AuthorVideoCountResult::getAuthorId,
                        VideoInfoDao.AuthorVideoCountResult::getVideoCount
                ));
    }

    /**
     * 执行实际的批量保存或更新操作
     *
     * @param videoEntities 视频信息实体列表
     */
    private void performBatchSaveOrUpdate(List<VideoInfoEntity> videoEntities, Boolean isSyncDaily) {
        List<VideoInfoEntity> insertList = new ArrayList<>();
        List<VideoInfoEntity> updateList = new ArrayList<>();
        List<VideoInfoEntity> allList = new ArrayList<>();

        // 1. 先对输入数据去重，避免相同业务键的重复处理
        Map<String, VideoInfoEntity> uniqueEntitiesMap = videoEntities.stream()
                .collect(Collectors.toMap(
                        entity -> buildBusinessKey(entity.getPlatformType(), entity.getPlatformVideoId()),
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("发现重复的视频数据，保留最后一个，platformType: {}, platformVideoId: {}",
                                    replacement.getPlatformType(), replacement.getPlatformVideoId());
                            return replacement; // 保留最后一个
                        }
                ));

        // 创建业务键到原始实体列表的映射，用于后续ID回填（支持一对多）
        Map<String, List<VideoInfoEntity>> businessKeyToOriginalEntitiesMap = new HashMap<>();
        for (VideoInfoEntity entity : videoEntities) {
            String businessKey = buildBusinessKey(entity.getPlatformType(), entity.getPlatformVideoId());
            businessKeyToOriginalEntitiesMap.computeIfAbsent(businessKey, k -> new ArrayList<>()).add(entity);
        }

        // 记录重复数据的统计信息
        businessKeyToOriginalEntitiesMap.forEach((businessKey, entities) -> {
            if (entities.size() > 1) {
                log.warn("[短视频数据] 发现重复业务键的视频数据，业务键: {}, 数量: {}", businessKey, entities.size());
            }
        });

        // 2. 批量查询已存在的数据（避免N+1查询）
        Map<String, VideoInfoEntity> existingEntitiesMap = batchLoadExistingVideoEntities(uniqueEntitiesMap.keySet());

        // 3. 分离新增和更新的数据（内存操作，无数据库查询）
        uniqueEntitiesMap.values().forEach(entity -> {
            String businessKey = buildBusinessKey(entity.getPlatformType(), entity.getPlatformVideoId());
            VideoInfoEntity existingEntity = existingEntitiesMap.get(businessKey);

            if (Objects.nonNull(existingEntity)) {
                // 更新现有记录
                BeanUtils.copyProperties(entity, existingEntity, "videoHash", "id", "createdDate", "updateDate", "isDeleted", "extractStatus", "analysisStatus", "extractTime", "analysisTime", "extractErrorReason");
                existingEntity.setUpdateDate(LocalDateTime.now());
                updateList.add(existingEntity);
                allList.add(existingEntity);

                // 将ID回填到所有相同业务键的原始实体中
                List<VideoInfoEntity> originalEntities = businessKeyToOriginalEntitiesMap.get(businessKey);
                if (originalEntities != null) {
                    for (VideoInfoEntity originalEntity : originalEntities) {
                        originalEntity.setId(existingEntity.getId());
                    }
                }

            } else {
                // 新增记录
                entity.setId(SnowflakeManager.nextValue());
                entity.setCreatedDate(LocalDateTime.now());
                entity.setUpdateDate(LocalDateTime.now());
                entity.setIsDeleted((byte) 0);
                // 设置默认提取状态
                entity.setExtractStatus((byte) 0); // 默认未提取
                entity.setExtractTime(null);
                entity.setAnalysisStatus((byte) 0); // 默认未分析
                entity.setAnalysisTime(null);
                insertList.add(entity);
                allList.add(entity);

                // 将ID回填到所有相同业务键的原始实体中
                List<VideoInfoEntity> originalEntities = businessKeyToOriginalEntitiesMap.get(businessKey);
                if (originalEntities != null) {
                    for (VideoInfoEntity originalEntity : originalEntities) {
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

        if (isSyncDaily) {
            // 查询视频基础历史表中是否存在今日同步数据 如果存在进行更新如果不存在进行新增 采用批量更新或插入方式
            saveDailyDataBatch(allList);
        }
    }

    /**
     * 执行实际的单个保存或更新操作
     *
     * @param videoEntity 视频信息实体
     * @return 保存或更新后的视频信息实体
     */
    private VideoInfoEntity performSingleSaveOrUpdate(VideoInfoEntity videoEntity) {
        VideoInfoEntity existingEntity = getOne(
                new LambdaQueryWrapper<VideoInfoEntity>()
                        .eq(VideoInfoEntity::getPlatformType, videoEntity.getPlatformType())
                        .eq(VideoInfoEntity::getPlatformVideoId, videoEntity.getPlatformVideoId())
        );
        VideoInfoEntity result;
        if (Objects.nonNull(existingEntity)) {
            // 更新现有记录
            BeanUtils.copyProperties(videoEntity, existingEntity, "videoHash", "id", "createdDate", "updateDate", "isDeleted", "extractStatus", "extractTime", "extractErrorReason", "analysisStatus", "analysisTime");
            existingEntity.setUpdateDate(LocalDateTime.now());

            updateById(existingEntity);
            result = existingEntity;
        } else {
            // 新增记录
            videoEntity.setId(SnowflakeManager.nextValue());
            videoEntity.setCreatedDate(LocalDateTime.now());
            videoEntity.setUpdateDate(LocalDateTime.now());
            videoEntity.setIsDeleted((byte) 0);
            // 设置默认提取状态
            videoEntity.setExtractStatus((byte) 0); // 默认未提取
            videoEntity.setExtractTime(null);
            videoEntity.setAnalysisTime(null);
            videoEntity.setAnalysisStatus((byte) 0); // 默认未分析

            save(videoEntity);

            result = videoEntity;
        }

        // 查询视频基础历史表中是否存在今日同步数据 如果存在进行更新如果不存在进行新增
        saveDailyDataSingle(existingEntity);

        return result;
    }

    /**
     * 构建分布式锁的key
     *
     * @param platformType    平台类型
     * @param platformVideoId 平台视频ID
     * @return 锁的key
     */
    private String buildLockKey(Byte platformType, String platformVideoId) {
        String format = LockKeyPrefix.VIDEO_INFO.getLockKey("save_or_update:%d:%s");
        return String.format(format, platformType, platformVideoId);
    }

    /**
     * 构建业务唯一键
     *
     * @param platformType    平台类型
     * @param platformVideoId 平台视频ID
     * @return 业务唯一键
     */
    private String buildBusinessKey(Byte platformType, String platformVideoId) {
        return platformType + ":" + platformVideoId;
    }

    /**
     * 批量保存视频每日数据
     *
     * @param videoEntities 视频信息实体集合
     */
    private void saveDailyDataBatch(List<VideoInfoEntity> videoEntities) {
        if (CollectionUtil.isEmpty(videoEntities)) {
            return;
        }

        LocalDate today = LocalDate.now();

        // 优化：批量处理，避免N+1查询，返回带操作类型的数据
        List<DailyDataWithOperation> dataWithOperations = processVideoDailyDataBatch(videoEntities, today);

        // 执行优化的批量保存或更新（避免MyBatis-Plus的N次SELECT查询）
        if (CollectionUtil.isNotEmpty(dataWithOperations)) {
            boolean success = batchSaveOrUpdateDailyData(dataWithOperations);
        }
    }

    /**
     * 批量处理视频每日数据（优化版本，避免N+1查询）
     *
     * @param videoEntities 视频信息实体列表
     * @param dataDate      数据日期
     * @return 处理后的带操作类型的每日数据列表
     */
    private List<DailyDataWithOperation> processVideoDailyDataBatch(List<VideoInfoEntity> videoEntities, LocalDate dataDate) {
        if (CollectionUtil.isEmpty(videoEntities)) {
            return new ArrayList<>();
        }

        // 1. 过滤有效视频并获取ID
        List<VideoInfoEntity> validVideos = videoEntities.stream()
                .filter(video -> video != null && video.getId() != null)
                .toList();

        if (CollectionUtil.isEmpty(validVideos)) {
            return new ArrayList<>();
        }

        List<Long> videoIds = validVideos.stream()
                .map(VideoInfoEntity::getId)
                .toList();

        // 2. 批量预加载数据（只需2次数据库查询）
        Map<Long, VideoInfoDailyDataEntity> existingDataMap = loadExistingDailyData(videoIds, dataDate);
        Map<Long, VideoInfoDailyDataEntity> lastSyncDataMap = loadLastSyncData(videoIds, dataDate);

        // 3. 批量处理（内存操作，无数据库查询）
        List<DailyDataWithOperation> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (VideoInfoEntity video : validVideos) {
            try {
                DailyDataWithOperation dataWithOp = processVideoDailyDataSingle(
                        video, dataDate, now,
                        existingDataMap.get(video.getId()),
                        lastSyncDataMap.get(video.getId())
                );
                result.add(dataWithOp);
            } catch (Exception e) {
                log.error("处理视频每日数据失败，视频ID: {}, 平台类型: {}, 平台视频ID: {}",
                        video.getId(), video.getPlatformType(), video.getPlatformVideoId(), e);
            }
        }
        return result;
    }

    /**
     * 批量加载已存在的视频实体（避免N+1查询）
     * 使用XML查询替换MyBatis-Plus方式
     *
     * @param businessKeys 业务键集合，格式为"platformType:platformVideoId"
     * @return 已存在的视频实体映射，key为业务键，value为视频实体
     */
    private Map<String, VideoInfoEntity> batchLoadExistingVideoEntities(Set<String> businessKeys) {
        if (CollectionUtil.isEmpty(businessKeys)) {
            return new HashMap<>();
        }
        Map<Byte, List<String>> platformVideoMapping = businessKeys.stream().map(keyStr -> {
            String[] parts = keyStr.split(":");
            if (parts.length != 2) {
                return null;
            }
            return new Entry<>(Byte.valueOf(parts[0]), parts[1]);
        }).filter(Objects::nonNull).collect(Collectors.groupingBy(Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toList())));

        if (EmptyUtil.isEmpty(platformVideoMapping)) {
            return new HashMap<>();
        }

        // 使用XML查询方式执行批量查询
        List<VideoInfoEntity> existingEntities = baseMapper.batchQueryByPlatformAndVideoIds(platformVideoMapping.values().stream().flatMap(Collection::stream).distinct().toList(), platformVideoMapping.keySet());
        if (EmptyUtil.isEmpty(existingEntities)) {
            return new HashMap<>();
        }
        // 构建返回映射
        return existingEntities.stream()
            .filter(item -> {
                List<String> videoIds = platformVideoMapping.get(item.getPlatformType());
                if (EmptyUtil.isEmpty(videoIds)) {
                    return false;
                }
                return videoIds.contains(item.getPlatformVideoId());
            })
                .collect(Collectors.toMap(
                        entity -> buildBusinessKey(entity.getPlatformType(), entity.getPlatformVideoId()),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 批量加载今日已存在的数据
     */
    private Map<Long, VideoInfoDailyDataEntity> loadExistingDailyData(List<Long> videoIds, LocalDate dataDate) {
        List<VideoInfoDailyDataEntity> existingDataList = videoInfoDailyDataService.list(
                new LambdaQueryWrapper<VideoInfoDailyDataEntity>()
                        .in(VideoInfoDailyDataEntity::getVideoId, videoIds)
                        .eq(VideoInfoDailyDataEntity::getDataDate, dataDate)
        );

        return existingDataList.stream()
                .collect(Collectors.toMap(VideoInfoDailyDataEntity::getVideoId, data -> data));
    }

    /**
     * 批量加载最近一次同步的数据
     */
    private Map<Long, VideoInfoDailyDataEntity> loadLastSyncData(List<Long> videoIds, LocalDate dataDate) {
        List<VideoInfoDailyDataEntity> lastSyncDataList =
                videoInfoDailyDataService.getLatestDailyDataByVideoIds(videoIds, dataDate);

        return lastSyncDataList.stream()
                .collect(Collectors.toMap(VideoInfoDailyDataEntity::getVideoId, data -> data));
    }

    /**
     * 处理单个视频的每日数据（纯内存操作版本）
     * 返回带操作类型的数据，解决雪花ID判断问题
     */
    private DailyDataWithOperation processVideoDailyDataSingle(
            VideoInfoEntity video,
            LocalDate dataDate,
            LocalDateTime now,
            VideoInfoDailyDataEntity existingData,
            VideoInfoDailyDataEntity lastSyncData) {

        // 计算增量
        VideoDailyDataIncrement increment = calculateVideoDailyIncrement(video, lastSyncData);

        VideoInfoDailyDataEntity dailyData;
        boolean isInsert;

        if (existingData != null) {
            // 更新现有记录
            dailyData = existingData;
            isInsert = false;
        } else {
            // 创建新记录
            dailyData = new VideoInfoDailyDataEntity();
            dailyData.setId(SnowflakeManager.nextValue()); // 设置雪花ID
            dailyData.setVideoId(video.getId());
            dailyData.setDataDate(dataDate);
            dailyData.setCreatedDate(now);
            dailyData.setIsDeleted((byte) 0);
            isInsert = true; // 明确标记为新增
        }

        // 设置当天最新数据
        updateVideoDailyDataFields(dailyData, video, increment, now);

        // 返回包装对象，包含数据和操作类型
        return new DailyDataWithOperation(dailyData, isInsert);
    }

    /**
     * 单个保存视频每日数据
     *
     * @param videoEntity 视频信息实体
     */
    private void saveDailyDataSingle(VideoInfoEntity videoEntity) {
        if (videoEntity == null || videoEntity.getId() == null) {
            log.warn("视频信息实体或ID为空，跳过每日数据保存");
            return;
        }

        LocalDate today = LocalDate.now();

        try {
            VideoInfoDailyDataEntity dailyData = processVideoDailyData(videoEntity, today);
            if (dailyData != null) {
                boolean success = videoInfoDailyDataService.saveOrUpdate(dailyData);
            }
        } catch (Exception e) {
            log.error("处理视频每日数据失败，视频ID: {}, 平台类型: {}, 平台视频ID: {}",
                    videoEntity.getId(), videoEntity.getPlatformType(), videoEntity.getPlatformVideoId(), e);
        }
    }

    /**
     * 处理视频每日数据（核心方法）
     *
     * @param videoEntity 视频信息实体
     * @param dataDate    数据日期
     * @return 处理后的每日数据实体
     */
    private VideoInfoDailyDataEntity processVideoDailyData(VideoInfoEntity videoEntity, LocalDate dataDate) {
        if (videoEntity == null || videoEntity.getId() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // 查询今日是否已存在数据
        VideoInfoDailyDataEntity existingDailyData = videoInfoDailyDataService.getOne(
                new LambdaQueryWrapper<VideoInfoDailyDataEntity>()
                        .eq(VideoInfoDailyDataEntity::getVideoId, videoEntity.getId())
                        .eq(VideoInfoDailyDataEntity::getDataDate, dataDate)
        );

        // 查询最近一次同步的数据用于计算增量（而不是固定前一天）
        VideoInfoDailyDataEntity lastSyncData = videoInfoDailyDataService.getOne(
                new LambdaQueryWrapper<VideoInfoDailyDataEntity>()
                        .eq(VideoInfoDailyDataEntity::getVideoId, videoEntity.getId())
                        .lt(VideoInfoDailyDataEntity::getDataDate, dataDate)  // 小于当前日期
                        .orderByDesc(VideoInfoDailyDataEntity::getDataDate)   // 按日期倒序
                        .last("LIMIT 1")  // 只取最新的一条
        );

        // 计算增量（当前值 - 最近一次同步值）
        VideoDailyDataIncrement increment = calculateVideoDailyIncrement(videoEntity, lastSyncData);

        VideoInfoDailyDataEntity dailyData;
        if (existingDailyData != null) {
            // 更新现有记录
            dailyData = existingDailyData;
        } else {
            // 创建新记录
            dailyData = new VideoInfoDailyDataEntity();
            dailyData.setId(SnowflakeManager.nextValue());
            dailyData.setVideoId(videoEntity.getId());
            dailyData.setDataDate(dataDate);
            dailyData.setCreatedDate(now);
            dailyData.setIsDeleted((byte) 0);
        }

        // 设置当天最新数据
        updateVideoDailyDataFields(dailyData, videoEntity, increment, now);

        return dailyData;
    }

    /**
     * 计算视频每日数据增量
     * 基于最近一次同步的数据计算增量，而不是固定的前一天数据
     *
     * @param videoEntity  视频信息实体
     * @param lastSyncData 最近一次同步的数据
     * @return 增量数据
     */
    private VideoDailyDataIncrement calculateVideoDailyIncrement(VideoInfoEntity videoEntity,
                                                                 VideoInfoDailyDataEntity lastSyncData) {
        if (lastSyncData == null) {
            // 如果最近一次同步数据为空，增量就等于0
            return new VideoDailyDataIncrement(
                    0L,
                    0L,
                    0L,
                    0L
            );
        }

        Long likeIncrement = calculateIncrement(videoEntity.getLikeCount(), lastSyncData.getLikeCount());
        Long commentIncrement = calculateIncrement(videoEntity.getCommentCount(), lastSyncData.getCommentCount());
        Long shareIncrement = calculateIncrement(videoEntity.getShareCount(), lastSyncData.getShareCount());
        Long collectIncrement = calculateIncrement(videoEntity.getCollectCount(), lastSyncData.getCollectCount());

        return new VideoDailyDataIncrement(likeIncrement, commentIncrement, shareIncrement, collectIncrement);
    }

    /**
     * 更新视频每日数据字段
     *
     * @param dailyData   每日数据实体
     * @param videoEntity 视频信息实体
     * @param increment   增量数据
     * @param updateTime  更新时间
     */
    private void updateVideoDailyDataFields(VideoInfoDailyDataEntity dailyData,
                                            VideoInfoEntity videoEntity,
                                            VideoDailyDataIncrement increment,
                                            LocalDateTime updateTime) {
        // 设置当天最新数据
        dailyData.setLikeCount(videoEntity.getLikeCount());
        dailyData.setCommentCount(videoEntity.getCommentCount());
        dailyData.setShareCount(videoEntity.getShareCount());
        dailyData.setCollectCount(videoEntity.getCollectCount());

        // 设置增量数据
        dailyData.setLikeIncrement(increment.likeIncrement());
        dailyData.setCommentIncrement(increment.commentIncrement());
        dailyData.setShareIncrement(increment.shareIncrement());
        dailyData.setCollectIncrement(increment.collectIncrement());

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
     * 视频每日数据增量内部类
     */
    private record VideoDailyDataIncrement(Long likeIncrement, Long commentIncrement,
                                           Long shareIncrement, Long collectIncrement) {
    }

    /**
     * 带操作类型的每日数据包装类
     * 用于解决雪花ID导致的新增/更新判断问题
     */
    private record DailyDataWithOperation(VideoInfoDailyDataEntity data, boolean isInsert) {
    }

    /**
     * 优化的批量保存或更新每日数据
     * 避免MyBatis-Plus saveOrUpdateBatch的N次SELECT查询问题
     *
     * @param dataWithOperations 带操作类型的数据列表
     * @return 是否成功
     */
    private boolean batchSaveOrUpdateDailyData(List<DailyDataWithOperation> dataWithOperations) {
        if (CollectionUtil.isEmpty(dataWithOperations)) {
            return true;
        }

        try {
            // 分离新增和更新的数据
            List<VideoInfoDailyDataEntity> toInsert = new ArrayList<>();
            List<VideoInfoDailyDataEntity> toUpdate = new ArrayList<>();

            for (DailyDataWithOperation dataWithOp : dataWithOperations) {
                if (dataWithOp.isInsert()) {
                    toInsert.add(dataWithOp.data());
                } else {
                    toUpdate.add(dataWithOp.data());
                }
            }

            // 批量插入新数据
            if (CollectionUtil.isNotEmpty(toInsert)) {
                boolean insertSuccess = videoInfoDailyDataService.saveBatch(toInsert);
                if (!insertSuccess) {
                    log.error("批量插入视频每日数据失败，数量: {}", toInsert.size());
                    return false;
                }
            }

            // 批量更新已有数据
            if (CollectionUtil.isNotEmpty(toUpdate)) {
                boolean updateSuccess = videoInfoDailyDataService.updateBatchById(toUpdate);
                if (!updateSuccess) {
                    log.error("批量更新视频每日数据失败，数量: {}", toUpdate.size());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("批量保存或更新视频每日数据异常", e);
            return false;
        }
    }
}
