package com.jiuyu.replay.agent.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.agent.repository.dao.AgentDao;
import com.jiuyu.replay.agent.entity.AgentEntity;
import com.jiuyu.replay.agent.repository.service.AgentService;


@Service("agentService")
public class AgentServiceImpl extends ServiceImpl<AgentDao, AgentEntity> implements AgentService {



}