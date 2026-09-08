package com.jiuyu.replay.agent.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.agent.entity.ClientInviteRewardRecordDetailEntity;
import org.springframework.stereotype.Component;

/**
 * 邀请奖励明细记录
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Mapper
@Component("agentClientInviteRewardRecordDetailDao")
public interface ClientInviteRewardRecordDetailDao extends BaseMapper<ClientInviteRewardRecordDetailEntity> {
	
}
