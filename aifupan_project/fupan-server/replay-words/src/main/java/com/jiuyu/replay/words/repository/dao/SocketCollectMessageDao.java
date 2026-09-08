package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.SocketCollectMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * websocket采集的信息
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Mapper
public interface SocketCollectMessageDao extends BaseMapper<SocketCollectMessageEntity> {
	
}
