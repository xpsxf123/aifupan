package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoInfluencerDailyDataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 达人每日数据表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
@Mapper
public interface VideoInfluencerDailyDataDao extends BaseMapper<VideoInfluencerDailyDataEntity> {

    /**
     * 查询每个达人最近的同步数据
     *
     * @param influencerIds 达人ID列表
     * @param currentDate   当前日期
     * @return 每个达人最近的同步数据
     */
    List<VideoInfluencerDailyDataEntity> getLatestDailyDataByInfluencerIds(
            @Param("influencerIds") List<Long> influencerIds,
            @Param("currentDate") LocalDate currentDate
    );

}
