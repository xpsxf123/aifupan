package com.jiuyu.replay.agent.repository.service.impl;

import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.agent.repository.dao.AgentPlatformSaleDao;
import com.jiuyu.replay.agent.entity.AgentPlatformSaleEntity;
import com.jiuyu.replay.agent.repository.service.AgentPlatformSaleService;

import java.util.ArrayList;
import java.util.List;


@Service("agentPlatformSaleService")
public class AgentPlatformSaleServiceImpl extends ServiceImpl<AgentPlatformSaleDao, AgentPlatformSaleEntity> implements AgentPlatformSaleService {


    @Override
    public List<AgentPlatformSaleInfoVo> getValidAgentSaleList(Long agentId) {
        if (agentId == null) return new ArrayList<>();
        return baseMapper.getValidAgentSaleList(agentId);
    }

    @Override
    public List<AgentPlatformSaleInfoVo> listAgentSales(Long agentId, Integer salesType) {
        return baseMapper.listAgentSales(agentId, salesType);
    }
}