package com.jiuyu.replay.video.project.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 爆款表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
@Mapper
public interface VideoHotSearchDao extends BaseMapper<VideoHotSearchEntity> {

    /**
     * 批量查询数据结果
     *
     * @param queryList 根据多个平台类型和关键词
     * @return
     */
    List<VideoHotSearchEntity> listByPlatformTypeAndSearchKeywords(@Param("queryList") List<VideoHotSearchEntity> queryList);
}
