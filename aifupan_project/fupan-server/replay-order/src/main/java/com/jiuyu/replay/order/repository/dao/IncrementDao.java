package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.IncrementEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 增量包表
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Mapper
public interface IncrementDao extends BaseMapper<IncrementEntity> {
	
}
