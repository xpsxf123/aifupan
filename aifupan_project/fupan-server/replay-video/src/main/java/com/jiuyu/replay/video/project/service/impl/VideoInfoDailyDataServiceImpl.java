package com.jiuyu.replay.video.project.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.dao.VideoInfoDailyDataDao;
import com.jiuyu.replay.video.project.entity.VideoInfoDailyDataEntity;
import com.jiuyu.replay.video.project.service.VideoInfoDailyDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 视频每日数据表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
@Service
public class VideoInfoDailyDataServiceImpl extends ServiceImpl<VideoInfoDailyDataDao, VideoInfoDailyDataEntity> implements VideoInfoDailyDataService {

    @Override
    public List<VideoInfoDailyDataEntity> getLatestDailyDataByVideoIds(List<Long> videoIds, LocalDate currentDate) {
        if (CollectionUtil.isEmpty(videoIds)) {
            return new ArrayList<>();
        }

        // 如果视频ID数量超过1000，自动使用分批处理
        if (videoIds.size() > 1000) {
            return getLatestDailyDataByVideoIdsBatch(videoIds, currentDate, 1000);
        }

        return baseMapper.getLatestDailyDataByVideoIds(videoIds, currentDate);
    }

    @Override
    public List<VideoInfoDailyDataEntity> getLatestDailyDataByVideoIdsBatch(List<Long> videoIds, LocalDate currentDate, int batchSize) {
        if (CollectionUtil.isEmpty(videoIds)) {
            return new ArrayList<>();
        }

        List<VideoInfoDailyDataEntity> result = new ArrayList<>();

        // 分批处理
        List<List<Long>> batches = CollectionUtil.split(videoIds, batchSize);
        for (List<Long> batch : batches) {
            List<VideoInfoDailyDataEntity> batchResult = baseMapper.getLatestDailyDataByVideoIds(batch, currentDate);
            result.addAll(batchResult);
        }

        return result;
    }

}
