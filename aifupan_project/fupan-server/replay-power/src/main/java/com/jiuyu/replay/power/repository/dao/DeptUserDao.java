package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.entity.DeptUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门对应下的用户
 *
 * @author jxy
 * @date 2024-07-08
 */
@Mapper
public interface DeptUserDao extends BaseMapper<DeptUserEntity> {

}
