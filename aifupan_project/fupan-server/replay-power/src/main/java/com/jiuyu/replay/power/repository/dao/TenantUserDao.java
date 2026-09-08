package com.jiuyu.replay.power.repository.dao;

import com.jiuyu.replay.power.entity.TenantUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户-用户-关联表
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Mapper
public interface TenantUserDao extends BaseMapper<TenantUserEntity> {
	
}
