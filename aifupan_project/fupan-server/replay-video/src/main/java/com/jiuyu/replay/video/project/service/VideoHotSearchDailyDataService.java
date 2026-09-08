package com.jiuyu.replay.video.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoHotSearchDailyDataEntity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 搜爆款同步历史表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
public interface VideoHotSearchDailyDataService extends IService<VideoHotSearchDailyDataEntity> {

    /**
     * 查询所有订阅关键字数据根据日期
     *
     * @param searchIds 搜索id集合
     * @param date      日期
     * @return 每日数据列表
     */
    List<VideoHotSearchDailyDataEntity> listDailyDataByKeywordsAndDataDate(Collection<Long> searchIds, LocalDate date);

    /**
     * 查询所有订阅关键字在指定日期范围内的数据
     * 用于累计增量计算
     *
     * @param searchIds 搜索id集合
     * @param startDate 开始日期（不包含）
     * @param endDate   结束日期（包含）
     * @return 日期范围内的数据列表
     */
    List<VideoHotSearchDailyDataEntity> listDailyDataByKeywordsAndDateRange(Collection<Long> searchIds, LocalDate startDate, LocalDate endDate);
}
