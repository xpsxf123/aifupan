package com.jiuyu.replay.video.project.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.video.common.enums.ExtractStatusEnum;
import com.jiuyu.replay.video.project.bo.VideoExtractRemoteVideoBo;
import com.jiuyu.replay.video.project.entity.VideoUserVideoEntity;
import com.jiuyu.replay.video.project.vo.VideoExtractVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频提取事务服务
 * 专门处理事务性的视频提取操作
 *
 * @author RayChou
 * @date 2025-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoExtractTransactionService {

    private final VideoUserVideoService videoUserVideoService;

    /**
     * 执行事务性的批量创建逻辑
     * 在分布式锁保护下执行，确保事务在锁释放前提交
     *
     * @param videoExtractRemoteVideoBo 远程视频信息
     * @param userCacheVo               用户信息
     * @param tenantId                  租户ID
     * @param videoInfoVos              视频信息列表
     * @return 提取结果列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<VideoExtractVo> executeTransactionalBatchCreate(
            VideoExtractRemoteVideoBo videoExtractRemoteVideoBo,
            UserCacheVo userCacheVo,
            Long tenantId,
            List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> videoInfoVos) {

        Long userId = userCacheVo.getId();
        Map<Long, VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> videoInfoVoMap = videoInfoVos.stream()
                .collect(Collectors.toMap(VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo::getVideoId,
                        Function.identity(), (existing, replacement) -> existing));
        List<Long> videoIds = videoInfoVos.stream()
                .map(VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo::getVideoId)
                .distinct().toList();
        Byte sourceType = videoExtractRemoteVideoBo.getSourceType();
        Long sourceId = videoExtractRemoteVideoBo.getSourceId();

        // 检查是否已经存在提取记录
        List<VideoUserVideoEntity> existingUserVideos = videoUserVideoService.list(
                Wrappers.<VideoUserVideoEntity>lambdaQuery()
                        .eq(VideoUserVideoEntity::getTenantId, tenantId)
                        .in(VideoUserVideoEntity::getVideoId, videoIds)
        );

        Map<Long, VideoUserVideoEntity> existingUserVideoMap = existingUserVideos.stream()
                .collect(Collectors.toMap(VideoUserVideoEntity::getVideoId, Function.identity(),
                        (existing, replacement) -> existing));

        // 过滤出需要新建的视频
        List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> newVideoInfoEntities = videoInfoVos.stream()
                .filter(video -> !existingUserVideoMap.containsKey(video.getVideoId()))
                .toList();

        // 批量创建新的用户视频关联记录
        List<VideoUserVideoEntity> newUserVideoEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo videoInfo : newVideoInfoEntities) {
            VideoUserVideoEntity userVideoEntity = new VideoUserVideoEntity();
            userVideoEntity.setId(SnowflakeManager.nextValue());
            userVideoEntity.setVideoTitle(videoInfo.getTitle());
            userVideoEntity.setUserId(userId);
            userVideoEntity.setTenantId(tenantId);
            userVideoEntity.setVideoId(videoInfo.getVideoId());
            userVideoEntity.setSourceType(sourceType);
            userVideoEntity.setSourceId(sourceId);
            userVideoEntity.setCreatedDate(now);
            userVideoEntity.setUpdateDate(now);
            userVideoEntity.setIsDeleted((byte) 0);

            // 智能判断初始状态
            /*if (videoInfo.getExtractStatus() != null && videoInfo.getExtractStatus() == 1) {
                // 如果视频已提取文案，直接标记为成功
                userVideoEntity.setExtractStatus(ExtractStatusEnum.COMPLETED.getCode());
                userVideoEntity.setExtractTime(LocalDateTime.now());
            } else {
                // 否则标记为待处理
                userVideoEntity.setExtractStatus(ExtractStatusEnum.PENDING.getCode());
            }*/

            userVideoEntity.setExtractStatus(ExtractStatusEnum.PENDING.getCode());

            newUserVideoEntities.add(userVideoEntity);
        }

        // 批量保存新记录
        if (CollectionUtil.isNotEmpty(newUserVideoEntities)) {
            boolean saveResult = videoUserVideoService.saveBatch(newUserVideoEntities);
            if (!saveResult) {
                throw new BusinessException("批量创建提取任务失败");
            }
        }

        // 构建返回结果：包含新建的记录 + 已存在的记录
        List<VideoUserVideoEntity> items = new ArrayList<>();

        // 添加新建的记录
        if (CollectionUtil.isNotEmpty(newUserVideoEntities)) {
            items.addAll(newUserVideoEntities);
        }

        // 添加已存在的记录
        if (CollectionUtil.isNotEmpty(existingUserVideos)) {
            items.addAll(existingUserVideos);
        }

        List<VideoExtractVo> videoExtractVos = BeanConvertUtils.convertList(items, VideoExtractVo.class);
        // 添加videoUrl
        videoExtractVos.forEach(item -> {
            item.setVideoUrl(videoInfoVoMap.get(item.getVideoId()).getVideoUrl());
            item.setAuthorId(videoInfoVoMap.get(item.getVideoId()).getAuthorId());
            item.setAuthorName(videoInfoVoMap.get(item.getVideoId()).getAuthorName());
        });

        return videoExtractVos;
    }
}
