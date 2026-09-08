package com.jiuyu.replay.agent.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.agent.entity.ClientInviteActivityEntity;
import org.springframework.stereotype.Component;

/**
 * 邀请活动
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
//@Mapper
@Component("agentClientInviteActivityDao")
public interface ClientInviteActivityDao extends BaseMapper<ClientInviteActivityEntity> {
	
}
