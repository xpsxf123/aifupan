package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.AgentCommissionBll;
import com.jiuyu.replay.agent.bo.AgentCommissionBo;
import com.jiuyu.replay.agent.bo.AgentCommissionListBo;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.agent.vo.AgentCommissionListVo;
import com.jiuyu.replay.agent.vo.CommissionRecordsVo;
import com.jiuyu.replay.api.logic.agent.AgentCommissionLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
public class AgentCommissionLogicImpl implements AgentCommissionLogic {

    @Resource
    private AgentCommissionBll agentCommissionBll;


    @Override
    public R<PageUtils<AgentCommissionListVo>> queryPage(AgentCommissionListBo agentCommissionListBo) {

        return agentCommissionBll.queryPage(agentCommissionListBo);
    }

    @Override
    public R<AgentCommissionInfoVo> info(Long id) {

        return agentCommissionBll.info(id);
    }

    @Override
    public R<String> save(AgentCommissionBo agentCommissionBo) {

        return agentCommissionBll.save(agentCommissionBo);
    }

    @Override
    public R<String> update(AgentCommissionBo agentCommissionBo) {

        return agentCommissionBll.update(agentCommissionBo);
    }

    @Override
    public R<String> delete(Long id) {

        return agentCommissionBll.delete(id);
    }

    @Override
    public R<List<CommissionRecordsVo>> commissionRecords(Long agentId) {
        return agentCommissionBll.commissionRecords(agentId);
    }
}

