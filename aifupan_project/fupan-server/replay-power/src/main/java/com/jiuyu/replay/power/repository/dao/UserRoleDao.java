package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联表
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@Mapper
public interface UserRoleDao extends BaseMapper<UserRoleEntity> {
    /**
     * 查询是超管用户
     */
    @Select("select user_id AS userId from tb_user_role where role_id=1")
    List<Long> selectUserIdByRoleId( );
}
