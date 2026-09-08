package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.agent.vo.AgentSaleListVo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.bo.AgentSaleBo;
import com.jiuyu.replay.agent.bo.AgentSaleListBo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleListVo;

import java.util.Collection;
import java.util.List;


/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
public interface AgentSaleProducer {


    /**
     * 代理商销售列表
     * @param agentSaleListBo 代理商销售列表查询参数
     * @return
     */
    PageUtils<AgentSaleListVo> queryPage(AgentSaleListBo agentSaleListBo);

    /**
    * 代理商销售信息
    * @param id 代理商销售id
    * @return
    */
    AgentSaleInfoVo info(Long id);

    /**
     * 新增代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
     AgentSaleInfoVo save(AgentSaleBo agentSaleBo);

    /**
     * 修改代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    void update(AgentSaleBo agentSaleBo);

    /**
     * 删除代理商销售
     * @param id 代理商销售id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取代理商的销售列表
     * @param agentId 代理商id
     * @return
     */
    List<AgentSaleInfoVo> listByAgentId(Long agentId);

    /**
     * 根据代理商销售id集合获取代理商销售列表
     * @param agentSaleIds 代理商销售id集合
     * @return
     */
    List<AgentSaleInfoVo> listByIds(Collection<Long> agentSaleIds);
}

