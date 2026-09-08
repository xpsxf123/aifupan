package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.AgentPromotionBll;
import com.jiuyu.replay.agent.bo.AgentPromotionBo;
import com.jiuyu.replay.agent.bo.AgentPromotionListBo;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.vo.AgentPromotionListVo;
import com.jiuyu.replay.api.logic.agent.AgentPromotionLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 代理商推广渠道
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
public class AgentPromotionLogicImpl implements AgentPromotionLogic {

    @Resource
    private AgentPromotionBll agentPromotionBll;


    @Override
    public R<PageUtils<AgentPromotionListVo>> queryPage(AgentPromotionListBo agentPromotionListBo) {

        return agentPromotionBll.queryPage(agentPromotionListBo);
    }

    @Override
    public R<AgentPromotionInfoVo> info(Long id) {

        return agentPromotionBll.info(id);
    }

    @Override
    public R<String> save(AgentPromotionBo agentPromotionBo) {

        return agentPromotionBll.save(agentPromotionBo);
    }

    @Override
    public R<String> update(AgentPromotionBo agentPromotionBo) {

        return agentPromotionBll.update(agentPromotionBo);
    }

    @Override
    public R<String> delete(Long id) {

        return agentPromotionBll.delete(id);
    }

    @Override
    public R<List<AgentPromotionInfoVo>> listByAgentId(Long agentId) {

        return agentPromotionBll.listByAgentId(agentId);
    }


}

