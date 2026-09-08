package com.jiuyu.replay.activity.repository.dao;

import com.jiuyu.replay.activity.entity.UserInviteEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-邀请关联
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
@Mapper
public interface UserInviteDao extends BaseMapper<UserInviteEntity> {
	
}
