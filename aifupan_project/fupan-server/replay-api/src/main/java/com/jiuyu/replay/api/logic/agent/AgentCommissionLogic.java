package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.agent.vo.CommissionRecordsVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.AgentCommissionListVo;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.agent.bo.AgentCommissionBo;
import com.jiuyu.replay.agent.bo.AgentCommissionListBo;

import java.util.List;


/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface AgentCommissionLogic {


    /**
     * 代理商佣金列表
     * @param agentCommissionListBo 代理商佣金列表查询参数
     * @return
     */
    R<PageUtils<AgentCommissionListVo>> queryPage(AgentCommissionListBo agentCommissionListBo);

    /**
    * 代理商佣金信息
    * @param id 代理商佣金id
    * @return
    */
    R<AgentCommissionInfoVo> info(Long id);

    /**
     * 新增代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    R<String> save(AgentCommissionBo agentCommissionBo);

    /**
     * 修改代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    R<String> update(AgentCommissionBo agentCommissionBo);

    /**
     * 删除代理商佣金
     * @param id 代理商佣金id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 佣金结算记录
     * @param agentId
     * @return
     */
    R<List<CommissionRecordsVo>> commissionRecords(Long agentId);
}

