package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.AgentSalePromotionChannelBll;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelListBo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelInfoVo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelListVo;
import com.jiuyu.replay.api.logic.agent.AgentSalePromotionChannelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Service
public class AgentSalePromotionChannelLogicImpl implements AgentSalePromotionChannelLogic {

    @Resource
    private AgentSalePromotionChannelBll agentSalePromotionChannelBll;


    @Override
    public R<PageUtils<AgentSalePromotionChannelListVo>> queryPage(AgentSalePromotionChannelListBo agentSalePromotionChannelListBo) {

        return agentSalePromotionChannelBll.queryPage(agentSalePromotionChannelListBo);
    }

    @Override
    public R<AgentSalePromotionChannelInfoVo> info(Long id) {

        return agentSalePromotionChannelBll.info(id);
    }

    @Override
    public R<String> save(AgentSalePromotionChannelBo agentSalePromotionChannelBo) {

        return agentSalePromotionChannelBll.save(agentSalePromotionChannelBo);
    }

    @Override
    public R<String> update(AgentSalePromotionChannelBo agentSalePromotionChannelBo) {

        return agentSalePromotionChannelBll.update(agentSalePromotionChannelBo);
    }

    @Override
    public R<String> delete(Long id) {

        return agentSalePromotionChannelBll.delete(id);
    }


}

