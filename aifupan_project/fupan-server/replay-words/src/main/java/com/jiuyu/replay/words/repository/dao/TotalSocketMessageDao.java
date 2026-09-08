package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.TotalSocketMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播场次的websocket记录统计
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
@Mapper
public interface TotalSocketMessageDao extends BaseMapper<TotalSocketMessageEntity> {
	
}
