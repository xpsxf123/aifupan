package com.jiuyu.replay.video.project.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.dao.VideoHotSearchDailyDataDao;
import com.jiuyu.replay.video.project.entity.VideoHotSearchDailyDataEntity;
import com.jiuyu.replay.video.project.service.VideoHotSearchDailyDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 搜爆款同步历史表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
@Service
public class VideoHotSearchDailyDataServiceImpl extends ServiceImpl<VideoHotSearchDailyDataDao, VideoHotSearchDailyDataEntity> implements VideoHotSearchDailyDataService {


    @Override
    public List<VideoHotSearchDailyDataEntity> listDailyDataByKeywordsAndDataDate(Collection<Long> searchIds, LocalDate date) {
        return list(new LambdaQueryWrapper<VideoHotSearchDailyDataEntity>()
                .in(VideoHotSearchDailyDataEntity::getSearchId, searchIds)
                .eq(VideoHotSearchDailyDataEntity::getDataDate, date));
    }

    @Override
    public List<VideoHotSearchDailyDataEntity> listDailyDataByKeywordsAndDateRange(Collection<Long> searchIds, LocalDate startDate, LocalDate endDate) {
        return list(new LambdaQueryWrapper<VideoHotSearchDailyDataEntity>()
                .select(VideoHotSearchDailyDataEntity::getSearchId,
                        VideoHotSearchDailyDataEntity::getVideoIncrement,
                        VideoHotSearchDailyDataEntity::getDataDate,
                        VideoHotSearchDailyDataEntity::getCollectionTime)
                .in(VideoHotSearchDailyDataEntity::getSearchId, searchIds)
                .gt(VideoHotSearchDailyDataEntity::getDataDate, startDate)  // 大于开始日期
                .le(VideoHotSearchDailyDataEntity::getDataDate, endDate)    // 小于等于结束日期
                .orderByDesc(VideoHotSearchDailyDataEntity::getDataDate));
    }
}
