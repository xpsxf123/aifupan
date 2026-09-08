package com.jiuyu.replay.agent.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.agent.bo.AgentSaleBo;
import com.jiuyu.replay.agent.bo.AgentSaleListBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.agent.producer.AgentSaleProducer;
import com.jiuyu.replay.agent.producer.AgentSalePromotionChannelProducer;
import com.jiuyu.replay.agent.producer.InviteUrlCodeProducer;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleListVo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Component
public class AgentSaleBll {

    @Resource
    private AgentSaleProducer agentSaleProducer;
    @Resource
    private AgentSalePromotionChannelProducer agentSalePromotionChannelProducer;
    @Resource
    private InviteUrlCodeProducer inviteUrlCodeProducer;
    @Resource
    private AgentProducer agentProducer;


    /**
     * 代理商销售列表
     * @param agentSaleListBo 代理商销售列表查询参数
     * @return
     */
    public R<PageUtils<AgentSaleListVo>> queryPage(AgentSaleListBo agentSaleListBo) {

        return R.ok("获取成功", agentSaleProducer.queryPage(agentSaleListBo));
    }

    /**
    * 代理商销售信息
    * @param id 代理商销售id
    * @return
    */
    public R<AgentSaleInfoVo> info(Long id) {

        AgentSaleInfoVo agentSaleInfoVo = agentSaleProducer.info(id);
        return R.ok("获取成功", agentSaleInfoVo);
    }

    /**
     * 新增代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(AgentSaleBo agentSaleBo) {

        // 保存代理商销售信息
        AgentSaleInfoVo agentSaleInfoVo = agentSaleProducer.save(agentSaleBo);

        // 保存销售与渠道的关联关系
        this.batchSaveSalePromotion(agentSaleBo.getPromotionIds(), agentSaleBo.getAgentId(), agentSaleInfoVo.getId());


        return R.ok("添加成功");
    }

    /**
     * 修改代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(AgentSaleBo agentSaleBo) {

        if(!StringUtils.isEmpty(agentSaleBo.getSaleName())) {
            // 获取当前不是修改状态，删除原销售与渠道的关联关系
            agentSalePromotionChannelProducer.deleteBySaleId(agentSaleBo.getId());
            // 删除销售与邀请链接code的关联关系
            inviteUrlCodeProducer.deleteByAgentSaleId(agentSaleBo.getId());
        }

        agentSaleProducer.update(agentSaleBo);

        // 保存销售与渠道的关联关系
        this.batchSaveSalePromotion(agentSaleBo.getPromotionIds(), agentSaleBo.getAgentId(), agentSaleBo.getId());

        return R.ok("修改成功");
    }

    /**
     * 保存销售与渠道的关联关系
     * @param promotionIds 渠道id集合
     * @param agentId 代理商id
     * @param agentSaleId 代理商销售id
     */
    private void batchSaveSalePromotion(List<Long> promotionIds, Long agentId, Long agentSaleId) {
        if(promotionIds != null && promotionIds.size() > 0) {

            List<AgentSalePromotionChannelBo> agentSalePromotionChannelBos = new LinkedList<>();

            for (Long promotionId : promotionIds) {
                // 设置邀请链接code
                InviteUrlCodeBo inviteUrlCodeBo = new InviteUrlCodeBo();
                inviteUrlCodeBo.setAgentId(agentId);
                inviteUrlCodeBo.setPromotionId(promotionId);
                inviteUrlCodeBo.setAgentSaleId(agentSaleId);
                inviteUrlCodeBo.setCodeType(3);
                InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeProducer.save(inviteUrlCodeBo);

                AgentSalePromotionChannelBo agentSalePromotionChannelBo = new AgentSalePromotionChannelBo();
                agentSalePromotionChannelBo.setPromotionChannelId(promotionId);
                agentSalePromotionChannelBo.setSaleUrlCode(inviteUrlCodeInfoVo.getUrlCode());
                agentSalePromotionChannelBos.add(agentSalePromotionChannelBo);
            }

            this.agentSalePromotionChannelProducer.batchSaveSalePromotion(agentSaleId, agentSalePromotionChannelBos);
        }
    }

    /**
     * 删除代理商销售
     * @param id 代理商销售id
     * @return
     */
    public R<String> delete(Long id) {

        agentSaleProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 获取代理商的销售列表
     * @param agentId 代理商id
     * @return
     */
    public R<List<AgentSaleInfoVo>> listByAgentId(Long agentId) {

        List<AgentSaleInfoVo> agentSaleInfoVos = agentSaleProducer.listByAgentId(agentId);

        return R.ok(agentSaleInfoVos);
    }

    /**
     * 根据代理商销售id集合获取代理商销售列表
     * @param agentSaleIds 代理商销售id集合
     * @return
     */
    public R<List<AgentSaleInfoVo>> listByIds(Collection<Long> agentSaleIds) {

        List<AgentSaleInfoVo> agentSaleInfoVos = this.agentSaleProducer.listByIds(agentSaleIds);
        return R.ok(agentSaleInfoVos);
    }

    /**
     * 根据渠道ID获取代理商销售列表
     *
     * @param channelId 渠道ID
     * @return 代理商销售列表
     */
    public List<AgentSaleInfoVo> listByChannelId(String channelId) {
        BusinessException.requireNonEmpty(channelId, "channelId不能为空");

        AgentInfoVo agentSale = agentProducer.getByChannelId(channelId);

        if (agentSale == null) {
            return new ArrayList<>();
        }

        List<AgentSaleInfoVo> agentSaleList = agentSaleProducer.listByAgentId(agentSale.getId());

        if (ObjectUtil.isEmpty(agentSaleList)) {
            return new ArrayList<>();
        }

        return agentSaleList;
    }
}

