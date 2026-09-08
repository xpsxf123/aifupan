package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelListVo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelInfoVo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelListBo;

import java.util.List;


/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
public interface AgentSalePromotionChannelProducer {


    /**
     * 代理商销售-渠道关联表列表
     * @param agentSalePromotionChannelListBo 代理商销售-渠道关联表列表查询参数
     * @return
     */
    PageUtils<AgentSalePromotionChannelListVo> queryPage(AgentSalePromotionChannelListBo agentSalePromotionChannelListBo);

    /**
    * 代理商销售-渠道关联表信息
    * @param id 代理商销售-渠道关联表id
    * @return
    */
    AgentSalePromotionChannelInfoVo info(Long id);

    /**
     * 新增代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
     AgentSalePromotionChannelInfoVo save(AgentSalePromotionChannelBo agentSalePromotionChannelBo);

    /**
     * 修改代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    void update(AgentSalePromotionChannelBo agentSalePromotionChannelBo);

    /**
     * 删除代理商销售-渠道关联表
     * @param id 代理商销售-渠道关联表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 保存销售与渠道的关联关系
     * @param saleId 销售id
     * @param agentSalePromotionChannelBos 销售推广渠道集合
     */
    void batchSaveSalePromotion(Long saleId, List<AgentSalePromotionChannelBo> agentSalePromotionChannelBos);

    /**
     * 根据销售id删除关联关系
     * @param saleId 销售id
     */
    void deleteBySaleId(Long saleId);
}

