package com.jiuyu.replay.video.project.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchSyncDataBo;
import com.jiuyu.replay.video.project.entity.VideoHotSearchDailyDataEntity;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;
import com.jiuyu.replay.video.project.entity.VideoHotSearchVideoEntity;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchSyncVideoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 爆款搜索数据同步事务服务
 * 专门处理事务性的数据同步操作
 *
 * @author RayChou
 * @date 2025-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoHotSearchSyncService {

    private final VideoInfoService videoInfoService;
    private final VideoHotSearchService videoHotSearchService;
    private final VideoHotSearchDailyDataService videoHotSearchDailyDataService;
    private final VideoHotSearchVideoService videoHotSearchVideoService;

    /**
     * 执行事务性同步流程
     * 在分布式锁保护下执行，确保事务生效
     *
     * @param syncDataBo 同步数据对象
     */
    @CustomRedissonLock(key = "'replay:lock:videoInfo:saveOrUpdateHotSearchKeyword:' + #args[0].platformType + ':' + #args[0].keyword")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<VideoInfoEntity> executeTransactionalSyncProcess(VideoHotSearchSyncDataBo syncDataBo) {
        // 1. 数据预处理：转换VO为Entity
        List<VideoInfoEntity> videoInfoEntities = convertVideoVoToEntity(syncDataBo.getVideoList());

        // 2. 保存视频信息：批量保存到数据库
        saveVideoInfoBatch(videoInfoEntities);

        // 3. 更新搜索记录：保存或更新关键字记录
        VideoHotSearchEntity hotSearchEntity = saveOrUpdateHotSearchKeyword(syncDataBo, videoInfoEntities);

        // 4. 更新统计数据：保存或更新每日数据
        saveOrUpdateDailyData(hotSearchEntity, syncDataBo, videoInfoEntities);

        // 5. 建立关联关系：保存视频与搜索的关联
        saveHotSearchVideoRelations(hotSearchEntity, videoInfoEntities, syncDataBo.getVideoList());

        return videoInfoEntities;
    }

    /**
     * 转换VideoHotSearchSyncVideoVo为VideoInfoEntity
     *
     * @param videoVoList 视频VO列表
     * @return 视频实体列表
     */
    private List<VideoInfoEntity> convertVideoVoToEntity(List<VideoHotSearchSyncVideoVo> videoVoList) {
        if (CollectionUtil.isEmpty(videoVoList)) {
            return new ArrayList<>();
        }
        return BeanConvertUtils.convertList(videoVoList, VideoInfoEntity.class);
    }

    /**
     * 保存视频信息：批量保存到数据库
     */
    private void saveVideoInfoBatch(List<VideoInfoEntity> videoInfoEntities) {
        if (CollectionUtil.isNotEmpty(videoInfoEntities)) {
            videoInfoService.batchSaveOrUpdate(videoInfoEntities, false);
        }
    }

    /**
     * 保存或更新关键字记录
     *
     * @param syncDataBo        同步数据对象
     * @param videoInfoEntities 视频实体列表
     * @return 关键字记录实体
     */
    private VideoHotSearchEntity saveOrUpdateHotSearchKeyword(VideoHotSearchSyncDataBo syncDataBo, List<VideoInfoEntity> videoInfoEntities) {
        // 查询是否已存在相同的关键字记录
        LambdaQueryWrapper<VideoHotSearchEntity> queryWrapper = new LambdaQueryWrapper<VideoHotSearchEntity>()
                .eq(VideoHotSearchEntity::getPlatformType, syncDataBo.getPlatformType())
                .eq(VideoHotSearchEntity::getSearchKeyword, syncDataBo.getKeyword().trim());

        VideoHotSearchEntity existingEntity = videoHotSearchService.getOne(queryWrapper);
        LocalDateTime now = LocalDateTime.now();

        if (existingEntity != null) {
            // 更新现有记录 - 需要计算真正的新增视频数量
            int actualNewVideoCount = calculateActualNewVideoCount(existingEntity, videoInfoEntities);
            existingEntity.setVideoCount(existingEntity.getVideoCount() + actualNewVideoCount);
            existingEntity.setLastSyncTime(now);
            existingEntity.setUpdateDate(now);

            videoHotSearchService.updateById(existingEntity);

            return existingEntity;
        } else {
            // 创建新记录 - 使用去重后的视频数量
            int uniqueVideoCount = calculateUniqueVideoCount(videoInfoEntities);
            VideoHotSearchEntity newEntity = VideoHotSearchEntity.builder()
                    .id(SnowflakeManager.nextValue())
                    .platformType(syncDataBo.getPlatformType())
                    .searchKeyword(syncDataBo.getKeyword().trim())
                    .videoCount(uniqueVideoCount)
                    .lastSyncTime(now)
                    .createdDate(now)
                    .updateDate(now)
                    .isDeleted((byte) 0)
                    .build();

            videoHotSearchService.save(newEntity);

            return newEntity;
        }
    }

    /**
     * 保存或更新每日数据
     *
     * @param hotSearchEntity   关键字记录实体
     * @param syncDataBo        同步数据对象
     * @param videoInfoEntities 视频实体列表
     */
    private void saveOrUpdateDailyData(VideoHotSearchEntity hotSearchEntity, VideoHotSearchSyncDataBo syncDataBo, List<VideoInfoEntity> videoInfoEntities) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 查询今日是否已有数据
        LambdaQueryWrapper<VideoHotSearchDailyDataEntity> queryWrapper = new LambdaQueryWrapper<VideoHotSearchDailyDataEntity>()
                .eq(VideoHotSearchDailyDataEntity::getSearchId, hotSearchEntity.getId())
                .eq(VideoHotSearchDailyDataEntity::getDataDate, today);

        VideoHotSearchDailyDataEntity existingDailyData = videoHotSearchDailyDataService.getOne(queryWrapper);
        // 计算真正的新增视频数量
        int actualNewVideoCount = calculateActualNewVideoCount(hotSearchEntity, videoInfoEntities);

        if (existingDailyData != null) {
            // 更新今日数据 - 只累加真正新增的视频数量
            int oldVideoCount = existingDailyData.getVideoCount();
            existingDailyData.setVideoCount(oldVideoCount + actualNewVideoCount);
            existingDailyData.setVideoIncrement(existingDailyData.getVideoIncrement() + actualNewVideoCount);
            existingDailyData.setCollectionTime(now);

            videoHotSearchDailyDataService.updateById(existingDailyData);
        } else {
            // 创建今日数据
            VideoHotSearchDailyDataEntity newDailyData = new VideoHotSearchDailyDataEntity();
            newDailyData.setId(SnowflakeManager.nextValue());
            newDailyData.setSearchId(hotSearchEntity.getId());
            newDailyData.setDataDate(today);
            newDailyData.setVideoCount(actualNewVideoCount);
            newDailyData.setVideoIncrement(actualNewVideoCount);
            newDailyData.setCollectionTime(now);

            videoHotSearchDailyDataService.save(newDailyData);
        }
    }

    /**
     * 增量保存视频关联关系
     * 基于videoId判断是否为新增视频关联关系
     *
     * @param hotSearchEntity   关键字记录实体
     * @param videoInfoEntities 视频信息实体列表（已保存到数据库，包含完整的videoId）
     * @param videoVoList       原始视频VO列表（包含达人信息）
     */
    private void saveHotSearchVideoRelations(VideoHotSearchEntity hotSearchEntity, List<VideoInfoEntity> videoInfoEntities, List<VideoHotSearchSyncVideoVo> videoVoList) {
        if (CollectionUtil.isEmpty(videoInfoEntities)) {
            return;
        }

        // 获取现有的视频关联关系，用于去重
        List<VideoHotSearchVideoEntity> existingRelations = videoHotSearchVideoService.list(new LambdaQueryWrapper<VideoHotSearchVideoEntity>()
                .eq(VideoHotSearchVideoEntity::getSearchId, hotSearchEntity.getId()));

        // 构建已存在的videoId集合
        Set<Long> existingVideoIds = existingRelations.stream()
                .map(VideoHotSearchVideoEntity::getVideoId)
                .collect(Collectors.toSet());

        // 构建videoId到VO的映射，用于获取达人信息
        Map<String, VideoHotSearchSyncVideoVo> videoVoMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(videoVoList)) {
            videoVoMap = videoVoList.stream()
                    .collect(Collectors.toMap(
                            vo -> vo.getPlatformType() + "_" + vo.getPlatformVideoId(),
                            Function.identity(),
                            (existing, replacement) -> existing // 如果有重复key，保留第一个
                    ));
        }

        // 构建新的关联关系列表（基于videoId去重）
        List<VideoHotSearchVideoEntity> newRelations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int sortOrder = existingRelations.size(); // 从现有数量开始排序

        // 反转视频集合用于精准排序
        Collections.reverse(videoInfoEntities);

        for (VideoInfoEntity videoInfo : videoInfoEntities) {
            // 跳过已存在的视频（基于videoId判断）
            if (existingVideoIds.contains(videoInfo.getId())) {
                continue;
            }

            // 获取对应的VO，用于获取达人信息
            String videoKey = videoInfo.getPlatformType() + "_" + videoInfo.getPlatformVideoId();
            VideoHotSearchSyncVideoVo videoVo = videoVoMap.get(videoKey);

            VideoHotSearchVideoEntity relation = new VideoHotSearchVideoEntity();
            relation.setId(SnowflakeManager.nextValue());
            relation.setSearchId(hotSearchEntity.getId());
            relation.setVideoId(videoInfo.getId());

            // 使用VO中的达人信息
            relation.setInfluencerPlatformType(videoVo.getInfluencerPlatformType());
            relation.setInfluencerPlatformUserId(videoVo.getInfluencerPlatformUserId());
            relation.setInfluencerNickname(videoVo.getInfluencerNickname());
            relation.setInfluencerAvatar(videoVo.getInfluencerAvatar());
            relation.setInfluencerFollowersCount(videoVo.getInfluencerFollowersCount());

            relation.setSortOrder(++sortOrder);
            relation.setCreatedDate(now);
            relation.setUpdateDate(now);
            relation.setIsDeleted((byte) 0);

            newRelations.add(relation);
            existingVideoIds.add(videoInfo.getId()); // 添加到已存在集合，避免本批次内重复
        }

        // 批量保存新的关联关系
        if (CollectionUtil.isNotEmpty(newRelations)) {
            videoHotSearchVideoService.saveBatch(newRelations);
        }
    }

    /**
     * 计算去重后的视频数量（用于新建记录）
     * 基于platformType和platformVideoId去重
     *
     * @param videoInfoEntities 视频信息列表
     * @return 去重后的视频数量
     */
    private int calculateUniqueVideoCount(List<VideoInfoEntity> videoInfoEntities) {
        if (CollectionUtil.isEmpty(videoInfoEntities)) {
            return 0;
        }

        // 基于platformType和platformVideoId去重
        Set<String> uniqueVideoKeys = videoInfoEntities.stream()
                .filter(video -> video.getPlatformType() != null && StrUtil.isNotBlank(video.getPlatformVideoId()))
                .map(video -> video.getPlatformType() + ":" + video.getPlatformVideoId())
                .collect(Collectors.toSet());

        int uniqueCount = uniqueVideoKeys.size();

        return uniqueCount;
    }

    /**
     * 计算真正的新增视频数量
     * 通过查询现有的视频关联关系，基于videoId判断本次同步中真正新增的视频数量
     *
     * @param hotSearchEntity   关键字记录实体
     * @param videoInfoEntities 本次同步的视频信息列表（已保存到数据库，包含完整的videoId）
     * @return 真正新增的视频数量
     */
    private int calculateActualNewVideoCount(VideoHotSearchEntity hotSearchEntity, List<VideoInfoEntity> videoInfoEntities) {
        if (CollectionUtil.isEmpty(videoInfoEntities)) {
            return 0;
        }

        // 获取现有的视频关联关系
        LambdaQueryWrapper<VideoHotSearchVideoEntity> existingWrapper = new LambdaQueryWrapper<VideoHotSearchVideoEntity>()
                .eq(VideoHotSearchVideoEntity::getSearchId, hotSearchEntity.getId());

        List<VideoHotSearchVideoEntity> existingRelations = videoHotSearchVideoService.list(existingWrapper);

        // 构建已存在的videoId集合
        Set<Long> existingVideoIds = existingRelations.stream()
                .map(VideoHotSearchVideoEntity::getVideoId)
                .collect(Collectors.toSet());

        // 计算本次同步中真正新增的视频数量（基于videoId判断，并去重）
        Set<Long> newVideoIds = videoInfoEntities.stream()
                .map(VideoInfoEntity::getId)
                .filter(videoId -> videoId != null && !existingVideoIds.contains(videoId))
                .collect(Collectors.toSet());

        int actualNewCount = newVideoIds.size();

        return actualNewCount;
    }


}
