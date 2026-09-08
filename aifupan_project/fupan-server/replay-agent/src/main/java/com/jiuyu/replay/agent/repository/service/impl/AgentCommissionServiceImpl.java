package com.jiuyu.replay.agent.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.agent.repository.dao.AgentCommissionDao;
import com.jiuyu.replay.agent.entity.AgentCommissionEntity;
import com.jiuyu.replay.agent.repository.service.AgentCommissionService;


@Service("agentCommissionService")
public class AgentCommissionServiceImpl extends ServiceImpl<AgentCommissionDao, AgentCommissionEntity> implements AgentCommissionService {



}