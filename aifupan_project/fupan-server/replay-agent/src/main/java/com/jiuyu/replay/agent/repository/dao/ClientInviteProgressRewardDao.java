package com.jiuyu.replay.agent.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.agent.entity.ClientInviteProgressRewardEntity;
import org.springframework.stereotype.Component;

/**
 * 邀请进度奖励
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Mapper
@Component("agentClientInviteProgressRewardDao")
public interface ClientInviteProgressRewardDao extends BaseMapper<ClientInviteProgressRewardEntity> {
	
}
