package com.jiuyu.replay.agent.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.agent.entity.AgentPlatformSaleEntity;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;

import java.util.List;

/**
 * 代理商平台销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
public interface AgentPlatformSaleService extends IService<AgentPlatformSaleEntity> {


    /**
     * 代理商获取有效的平台销售
     *
     * @param agentId 代理商id
     * @return 有效的平台销售
     */
    List<AgentPlatformSaleInfoVo> getValidAgentSaleList(Long agentId);

    /**
     * 根据代理商id获取平台销售列表
     *
     * @param agentId   代理商id
     * @param salesType 销售类型
     * @return 平台销售列表
     */
    List<AgentPlatformSaleInfoVo> listAgentSales(Long agentId, Integer salesType);
}

