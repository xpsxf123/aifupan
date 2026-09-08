package com.jiuyu.replay.common.repository.dao;

import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置的键值对
 * 
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Mapper
public interface SystemKvDao extends BaseMapper<SystemKvEntity> {
	
}
