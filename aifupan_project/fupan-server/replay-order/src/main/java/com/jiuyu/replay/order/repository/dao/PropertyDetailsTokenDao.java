package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.PropertyDetailsTokenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产使用记录关联aitoken消耗表
 * 
 * @author lj
 * @email 
 * @date 2025-03-21 18:45:03
 */
@Mapper
public interface PropertyDetailsTokenDao extends BaseMapper<PropertyDetailsTokenEntity> {
	
}
