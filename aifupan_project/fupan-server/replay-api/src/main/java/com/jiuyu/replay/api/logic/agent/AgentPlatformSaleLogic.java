package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

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
public interface AgentPlatformSaleLogic {


    /**
     * 代理商平台销售列表
     * @param agentPlatformSaleListBo 代理商平台销售列表查询参数
     * @return
     */
    R<PageUtils<AgentPlatformSaleListVo>> queryPage(AgentPlatformSaleListBo agentPlatformSaleListBo);

    /**
    * 代理商平台销售信息
    * @param id 代理商平台销售id
    * @return
    */
    R<AgentPlatformSaleInfoVo> info(Long id);

    /**
     * 新增代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    R<String> save(AgentPlatformSaleBo agentPlatformSaleBo);

    /**
     * 修改代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    R<String> update(AgentPlatformSaleBo agentPlatformSaleBo);

    /**
     * 删除代理商平台销售
     * @param id 代理商平台销售id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取代理商的平台销售列表
     * @param agentId 代理商id
     * @return
     */
    R<List<AgentPlatformSaleInfoVo>> listByAgentId(Long agentId, Integer salesType);
}

