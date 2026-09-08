package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.agent.vo.AgentPlatformSaleListVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleBo;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleListBo;

import java.util.List;


/**
 * 代理商平台销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
public interface AgentPlatformSaleProducer {


    /**
     * 代理商平台销售列表
     * @param agentPlatformSaleListBo 代理商平台销售列表查询参数
     * @return
     */
    PageUtils<AgentPlatformSaleListVo> queryPage(AgentPlatformSaleListBo agentPlatformSaleListBo);

    /**
    * 代理商平台销售信息
    * @param id 代理商平台销售id
    * @return
    */
    AgentPlatformSaleInfoVo info(Long id);

    /**
     * 新增代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
     AgentPlatformSaleInfoVo save(AgentPlatformSaleBo agentPlatformSaleBo);

    /**
     * 修改代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    void update(AgentPlatformSaleBo agentPlatformSaleBo);

    /**
     * 删除代理商平台销售
     * @param id 代理商平台销售id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据代理商id获取平台销售列表
     *
     * @param agentId   代理商id
     * @param salesType
     * @return
     */
    List<AgentPlatformSaleInfoVo> listByAgentId(Long agentId, Integer salesType);

    /**
     * 根据代理商id和销售id获取平台销售信息
     * @param agentId 代理商id
     * @param saleId 销售id
     * @return
     */
    AgentPlatformSaleInfoVo infoByAgentIdAndSaleId(Long agentId, Long saleId);

    /**
     * 获取代理轮询到的平台销售
     * @param agentId 代理商id
     * @return 平台销售id
     */
    Long getPollingSaleId(Long agentId);

    /**
     * 根据销售id获取代理商平台销售数量
     *
     * @param salesId 销售id
     * @return 数量
     */
    long countBySaleId(Long salesId);
}

