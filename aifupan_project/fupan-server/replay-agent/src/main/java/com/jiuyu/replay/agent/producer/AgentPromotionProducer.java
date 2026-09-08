package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;

import com.jiuyu.replay.agent.vo.AgentPromotionListVo;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.bo.AgentPromotionBo;
import com.jiuyu.replay.agent.bo.AgentPromotionListBo;

import java.util.List;


/**
 * 代理商推广渠道
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface AgentPromotionProducer {


    /**
     * 代理商推广渠道列表
     * @param agentPromotionListBo 代理商推广渠道列表查询参数
     * @return
     */
    PageUtils<AgentPromotionListVo> queryPage(AgentPromotionListBo agentPromotionListBo);

    /**
    * 代理商推广渠道信息
    * @param id 代理商推广渠道id
    * @return
    */
    AgentPromotionInfoVo info(Long id);

    /**
     * 新增代理商推广渠道
     * @param agentPromotionBo 代理商推广渠道对象
     * @return
     */
     AgentPromotionInfoVo save(AgentPromotionBo agentPromotionBo);

    /**
     * 修改代理商推广渠道
     * @param agentPromotionBo 代理商推广渠道对象
     * @return
     */
    void update(AgentPromotionBo agentPromotionBo);

    /**
     * 删除代理商推广渠道
     * @param id 代理商推广渠道id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取代理商的推广渠道列表
     * @param agentId 代理商id
     * @return
     */
    List<AgentPromotionInfoVo> listByAgentId(Long agentId);

    List<InviteUrlPromotionVo> getNameById(List<InviteUrlPromotionVo> urlPromotionVos);

    List<Long> selectQuery(AgentPromotionBo agentPromotionBo);
}

