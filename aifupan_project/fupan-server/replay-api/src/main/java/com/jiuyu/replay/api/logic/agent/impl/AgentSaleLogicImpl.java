package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.AgentSaleBll;
import com.jiuyu.replay.agent.bo.AgentSaleBo;
import com.jiuyu.replay.agent.bo.AgentSaleListBo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleListVo;
import com.jiuyu.replay.api.logic.agent.AgentSaleLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Service
public class AgentSaleLogicImpl implements AgentSaleLogic {

    @Resource
    private AgentSaleBll agentSaleBll;


    @Override
    public R<PageUtils<AgentSaleListVo>> queryPage(AgentSaleListBo agentSaleListBo) {

        return agentSaleBll.queryPage(agentSaleListBo);
    }

    @Override
    public R<AgentSaleInfoVo> info(Long id) {

        return agentSaleBll.info(id);
    }

    @Override
    public R<String> save(AgentSaleBo agentSaleBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        agentSaleBo.setCreateUserId(user.getId());

        return agentSaleBll.save(agentSaleBo);
    }

    @Override
    public R<String> update(AgentSaleBo agentSaleBo) {

        return agentSaleBll.update(agentSaleBo);
    }

    @Override
    public R<String> delete(Long id) {

        return agentSaleBll.delete(id);
    }

    @Override
    public R<List<AgentSaleInfoVo>> listByAgentId(Long agentId) {

        return agentSaleBll.listByAgentId(agentId);
    }


}

