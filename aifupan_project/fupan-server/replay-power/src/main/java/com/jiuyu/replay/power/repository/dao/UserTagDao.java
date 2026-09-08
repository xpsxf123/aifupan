package com.jiuyu.replay.power.repository.dao;

import com.jiuyu.replay.power.entity.UserTagEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-标签-关联
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@Mapper
public interface UserTagDao extends BaseMapper<UserTagEntity> {
	
}
