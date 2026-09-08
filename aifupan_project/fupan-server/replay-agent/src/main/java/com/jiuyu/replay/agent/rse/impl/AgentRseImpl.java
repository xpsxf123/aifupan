package com.jiuyu.replay.agent.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.agent.entity.AgentEntity;
import com.jiuyu.replay.agent.repository.service.AgentService;
import com.jiuyu.replay.agent.rse.AgentRse;
import com.jiuyu.replay.generic.vo.agent.AgentInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class AgentRseImpl implements AgentRse {

    @Resource
    private AgentService agentService;

    @Override
    public AgentInfoVo info(Long id, Integer agentStatus) {
        QueryWrapper<AgentEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        if(agentStatus != null) {
            wrapper.eq("agent_status", agentStatus);
        }

        AgentEntity agentEntity = agentService.getOne(wrapper);
        if(agentEntity != null) {
            AgentInfoVo agentInfoVo = new AgentInfoVo();
            BeanUtils.copyProperties(agentEntity, agentInfoVo);
            return agentInfoVo;
        }

        return null;
    }
}
