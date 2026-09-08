package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoInfoDailyDataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 视频每日数据表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
@Mapper
public interface VideoInfoDailyDataDao extends BaseMapper<VideoInfoDailyDataEntity> {

    /**
     * 查询每个视频最近的同步数据
     *
     * @param videoIds    视频ID列表
     * @param currentDate 当前日期
     * @return 每个视频最近的同步数据
     */
    List<VideoInfoDailyDataEntity> getLatestDailyDataByVideoIds(
            @Param("videoIds") List<Long> videoIds,
            @Param("currentDate") LocalDate currentDate
    );

}
