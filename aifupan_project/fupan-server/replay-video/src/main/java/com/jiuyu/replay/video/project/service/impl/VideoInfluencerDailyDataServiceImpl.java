package com.jiuyu.replay.video.project.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.dao.VideoInfluencerDailyDataDao;
import com.jiuyu.replay.video.project.entity.VideoInfluencerDailyDataEntity;
import com.jiuyu.replay.video.project.service.VideoInfluencerDailyDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 达人每日数据表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
@Service
public class VideoInfluencerDailyDataServiceImpl extends ServiceImpl<VideoInfluencerDailyDataDao, VideoInfluencerDailyDataEntity> implements VideoInfluencerDailyDataService {

    @Override
    public List<VideoInfluencerDailyDataEntity> getLatestDailyDataByInfluencerIds(List<Long> influencerIds, LocalDate currentDate) {
        if (CollectionUtil.isEmpty(influencerIds)) {
            return new ArrayList<>();
        }

        // 如果达人ID数量超过1000，自动使用分批处理
        if (influencerIds.size() > 1000) {
            List<VideoInfluencerDailyDataEntity> result = new ArrayList<>();
            List<List<Long>> batches = CollectionUtil.split(influencerIds, 1000);
            for (List<Long> batch : batches) {
                List<VideoInfluencerDailyDataEntity> batchResult = baseMapper.getLatestDailyDataByInfluencerIds(batch, currentDate);
                result.addAll(batchResult);
            }
            return result;
        }

        return baseMapper.getLatestDailyDataByInfluencerIds(influencerIds, currentDate);
    }

}
