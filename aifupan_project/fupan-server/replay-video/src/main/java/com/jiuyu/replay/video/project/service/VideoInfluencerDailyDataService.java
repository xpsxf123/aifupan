package com.jiuyu.replay.video.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoInfluencerDailyDataEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 达人每日数据表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
public interface VideoInfluencerDailyDataService extends IService<VideoInfluencerDailyDataEntity> {

    /**
     * 查询每个达人最近的同步数据
     *
     * @param influencerIds 达人ID列表
     * @param currentDate   当前日期
     * @return 每个达人最近的同步数据
     */
    List<VideoInfluencerDailyDataEntity> getLatestDailyDataByInfluencerIds(List<Long> influencerIds, LocalDate currentDate);

}
