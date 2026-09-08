package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.AgentSaleListVo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.bo.AgentSaleBo;
import com.jiuyu.replay.agent.bo.AgentSaleListBo;

import java.util.List;


/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
public interface AgentSaleLogic {


    /**
     * 代理商销售列表
     * @param agentSaleListBo 代理商销售列表查询参数
     * @return
     */
    R<PageUtils<AgentSaleListVo>> queryPage(AgentSaleListBo agentSaleListBo);

    /**
    * 代理商销售信息
    * @param id 代理商销售id
    * @return
    */
    R<AgentSaleInfoVo> info(Long id);

    /**
     * 新增代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    R<String> save(AgentSaleBo agentSaleBo);

    /**
     * 修改代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    R<String> update(AgentSaleBo agentSaleBo);

    /**
     * 删除代理商销售
     * @param id 代理商销售id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取代理商的销售列表
     * @param agentId 代理商id
     * @return
     */
    R<List<AgentSaleInfoVo>> listByAgentId(Long agentId);
}

