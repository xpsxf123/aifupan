package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.entity.UserBusinessEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户业务表
 *
 * @author jxy
 * @date 2024-07-08
 */
@Mapper
public interface UserBusinessDao extends BaseMapper<UserBusinessEntity> {

}
