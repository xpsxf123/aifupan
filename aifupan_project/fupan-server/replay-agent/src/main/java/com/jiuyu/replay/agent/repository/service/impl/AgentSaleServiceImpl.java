package com.jiuyu.replay.agent.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.agent.repository.dao.AgentSaleDao;
import com.jiuyu.replay.agent.entity.AgentSaleEntity;
import com.jiuyu.replay.agent.repository.service.AgentSaleService;


@Service("agentSaleService")
public class AgentSaleServiceImpl extends ServiceImpl<AgentSaleDao, AgentSaleEntity> implements AgentSaleService {



}