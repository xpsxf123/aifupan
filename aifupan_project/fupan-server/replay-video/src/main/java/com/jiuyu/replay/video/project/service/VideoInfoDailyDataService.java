package com.jiuyu.replay.video.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoInfoDailyDataEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 视频每日数据表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
public interface VideoInfoDailyDataService extends IService<VideoInfoDailyDataEntity> {

    /**
     * 查询每个视频最近的同步数据
     *
     * @param videoIds    视频ID列表
     * @param currentDate 当前日期
     * @return 每个视频最近的同步数据
     */
    List<VideoInfoDailyDataEntity> getLatestDailyDataByVideoIds(List<Long> videoIds, LocalDate currentDate);

    /**
     * 查询每个视频最近的同步数据（分批处理版本）
     * 当视频ID数量很大时，自动分批查询以避免SQL参数过多的问题
     *
     * @param videoIds    视频ID列表
     * @param currentDate 当前日期
     * @param batchSize   每批处理的数量，默认1000
     * @return 每个视频最近的同步数据
     */
    List<VideoInfoDailyDataEntity> getLatestDailyDataByVideoIdsBatch(List<Long> videoIds, LocalDate currentDate, int batchSize);

}
