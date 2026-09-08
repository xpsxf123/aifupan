package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelListVo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelInfoVo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelListBo;


/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
public interface AgentSalePromotionChannelLogic {


    /**
     * 代理商销售-渠道关联表列表
     * @param agentSalePromotionChannelListBo 代理商销售-渠道关联表列表查询参数
     * @return
     */
    R<PageUtils<AgentSalePromotionChannelListVo>> queryPage(AgentSalePromotionChannelListBo agentSalePromotionChannelListBo);

    /**
    * 代理商销售-渠道关联表信息
    * @param id 代理商销售-渠道关联表id
    * @return
    */
    R<AgentSalePromotionChannelInfoVo> info(Long id);

    /**
     * 新增代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    R<String> save(AgentSalePromotionChannelBo agentSalePromotionChannelBo);

    /**
     * 修改代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    R<String> update(AgentSalePromotionChannelBo agentSalePromotionChannelBo);

    /**
     * 删除代理商销售-渠道关联表
     * @param id 代理商销售-渠道关联表id
     * @return
     */
    R<String> delete(Long id);


}

