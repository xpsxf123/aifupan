package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 热搜视频邮箱账号 DAO
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 热搜邮箱账号数据访问层，提供账号的增删改查操作
 */
@Mapper
public interface VideoHotSearchEmailAccountDao extends BaseMapper<VideoHotSearchEmailAccountEntity> {

}

