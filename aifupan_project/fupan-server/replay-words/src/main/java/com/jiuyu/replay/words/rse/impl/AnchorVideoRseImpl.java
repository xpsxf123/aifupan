package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.dto.words.ShareVideoCloudDto;
import com.jiuyu.replay.generic.dto.words.UserVideoCountDto;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.video.SubUserVideoListVo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.rse.AnchorVideoRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnchorVideoRseImpl implements AnchorVideoRse {

    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private WordsProperties wordsProperties;

    @Override
    public AnchorVideoInfoVo infoUserVideoByVideoId(String videoId, Long userId) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("video_id", videoId);
        AnchorVideoEntity anchorVideoEntity = this.anchorVideoService.getOne(wrapper);

        if (anchorVideoEntity != null) {
            return BeanConvertUtils.convert(anchorVideoEntity, AnchorVideoInfoVo.class);
        }

        return null;
    }

    @Override
    public String shareVideoToCloud(ShareVideoCloudDto shareVideoCloudDto) {

        AnchorVideoEntity anchorVideoEntity = BeanConvertUtils.convert(shareVideoCloudDto, AnchorVideoEntity.class);
        anchorVideoEntity.setPlayUrl(shareVideoCloudDto.getOnlineFileUrl());
        anchorVideoEntity.setUploadStatus(1);
        anchorVideoEntity.setUpdateDate(new Date());
        String shareUrl = wordsProperties.getCloudSpaceUrl() + "onlineAnalysis/0/" + shareVideoCloudDto.getVideoId();
        anchorVideoEntity.setShareUrl(shareUrl);
        this.anchorVideoService.updateById(anchorVideoEntity);

        return shareUrl;
    }

    @Override
    public List<UserVideoCountDto> getMonthlyVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        // 获取当前月份的开始和结束时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);

        // 转换为DTO列表
        return getUserVideoCountByTimeRange(userIds, tenantId, monthStart, monthEnd);
    }

    @Override
    public List<UserVideoCountDto> getYesterdayVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        // 获取昨日的开始和结束时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = yesterdayStart.withHour(23).withMinute(59).withSecond(59);

        // 转换为DTO列表
        return getUserVideoCountByTimeRange(userIds, tenantId, yesterdayStart, yesterdayEnd);
    }

    @Override
    public List<SubUserVideoListVo> getVideoListByUserIdAndTenantId(Long userId, Long tenantId) {
        // 查询子账号的录制视频列表
        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorVideoEntity::getUserId, userId);
        wrapper.eq(AnchorVideoEntity::getTenantId, tenantId);
        wrapper.ge(AnchorVideoEntity::getDuration, 60);
        List<AnchorVideoEntity> anchorVideoEntities = anchorVideoService.list(wrapper);

        if (anchorVideoEntities == null || anchorVideoEntities.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为VO
        return BeanConvertUtils.convertList(anchorVideoEntities, SubUserVideoListVo.class);
    }

    @Override
    public void deleteLocalVideoByIds(List<String> videoIds) {

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new LambdaQueryWrapper<AnchorVideoEntity>().in(AnchorVideoEntity::getVideoId, videoIds));
        if(anchorVideoEntities != null && !anchorVideoEntities.isEmpty()) {
            for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                anchorVideoEntity.setLocalVideoStatus(1);
                anchorVideoEntity.setUpdateDate(new Date());
            }
            this.anchorVideoService.updateBatchById(anchorVideoEntities);
        }
    }

    @Override
    public Long countUserVideo(Long userId, Long tenantId) {

        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorVideoEntity::getUserId, userId);
        wrapper.eq(AnchorVideoEntity::getTenantId, tenantId);
        wrapper.ge(AnchorVideoEntity::getDuration, 60);
        wrapper.eq(AnchorVideoEntity::getIsRecording, 0);

        return this.anchorVideoService.count(wrapper);
    }

    @Override
    public Map<String, String> listLatestVideoBySecUidsAndDuration(List<String> secUids, Integer duration) {
        if (secUids == null || secUids.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 第一步：每个主播只取最新视频的id（GROUP BY聚合，避免返回大量冗余数据）
        QueryWrapper<AnchorVideoEntity> maxIdWrapper = new QueryWrapper<>();
        maxIdWrapper.select("sec_uid, MAX(id) as id")
                .in("sec_uid", secUids)
                .ge("duration", duration)
                .eq("analysis_status", 2)
                .groupBy("sec_uid");
        List<Map<String, Object>> maxIdResults = anchorVideoService.listMaps(maxIdWrapper);

        if (maxIdResults == null || maxIdResults.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 第二步：根据聚合出的id查询视频详情
        List<Long> maxIds = maxIdResults.stream()
                .map(m -> Long.valueOf(m.get("id").toString()))
                .collect(Collectors.toList());

        LambdaQueryWrapper<AnchorVideoEntity> videoWrapper = new LambdaQueryWrapper<>();
        videoWrapper.in(AnchorVideoEntity::getId, maxIds)
                .select(AnchorVideoEntity::getSecUid, AnchorVideoEntity::getVideoId);
        List<AnchorVideoEntity> videos = anchorVideoService.list(videoWrapper);

        if (videos == null || videos.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, String> latestVideoMap = new LinkedHashMap<>();
        for (AnchorVideoEntity video : videos) {
            latestVideoMap.put(video.getSecUid(), video.getVideoId());
        }

        return latestVideoMap;
    }

    /**
     * 根据时间范围获取用户视频数量
     *
     * @param userIds        用户ID列表
     * @param tenantId       租户id
     * @param startTime      开始时间
     * @param endTime        结束时间
     */
    private List<UserVideoCountDto> getUserVideoCountByTimeRange(List<Long> userIds, Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        QueryWrapper<AnchorVideoEntity> queryWrapper = new QueryWrapper<AnchorVideoEntity>()
                .select("user_id, COUNT(*) as video_count")
                .in("user_id", userIds)
                .eq("tenant_id", tenantId)
                .ge("start_time", startTimeStr)
                .le("start_time", endTimeStr)
                .ge("duration", 60)
                .eq("is_recording", 0)
                .groupBy("user_id");

        // 执行查询
        List<Map<String, Object>> queryResult = anchorVideoService.listMaps(queryWrapper);

        // 创建用户ID到数量的映射
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : queryResult) {
            Long userId = Long.valueOf(row.get("user_id").toString());
            Integer count = Integer.valueOf(row.get("video_count").toString());
            countMap.put(userId, count);
        }

        // 转换为DTO列表
        List<UserVideoCountDto> result = new ArrayList<>();
        for (Long userId : userIds) {
            UserVideoCountDto dto = new UserVideoCountDto();
            dto.setUserId(userId);
            dto.setVideoCount(countMap.getOrDefault(userId, 0));
            result.add(dto);
        }

        return result;
    }

    @Override
    public void updateCloudRename(String videoId, String cloudRename) {
        anchorVideoService.update(new LambdaUpdateWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, videoId)
                .set(AnchorVideoEntity::getCloudRename, cloudRename)
                .set(AnchorVideoEntity::getUpdateDate, new Date())
        );
    }


}
