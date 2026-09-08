package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoUserVideoEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户-视频关联表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Mapper
public interface VideoUserVideoDao extends BaseMapper<VideoUserVideoEntity> {

}
