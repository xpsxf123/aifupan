package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.PackageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套餐表(用户版本)
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Mapper
public interface PackageDao extends BaseMapper<PackageEntity> {
	
}
