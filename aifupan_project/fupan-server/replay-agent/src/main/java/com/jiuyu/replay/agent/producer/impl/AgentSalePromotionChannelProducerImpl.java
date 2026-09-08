package com.jiuyu.replay.agent.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelListVo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelInfoVo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelListBo;
import com.jiuyu.replay.agent.repository.service.AgentSalePromotionChannelService;
import com.jiuyu.replay.agent.entity.AgentSalePromotionChannelEntity;
import com.jiuyu.replay.agent.producer.AgentSalePromotionChannelProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Service
public class AgentSalePromotionChannelProducerImpl implements AgentSalePromotionChannelProducer {

    @Resource
    private AgentSalePromotionChannelService agentSalePromotionChannelService;


    @Override
    public PageUtils<AgentSalePromotionChannelListVo> queryPage(AgentSalePromotionChannelListBo agentSalePromotionChannelListBo) {
        QueryWrapper<AgentSalePromotionChannelEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(agentSalePromotionChannelListBo.getKeyword())){
            wrapper.like("name", agentSalePromotionChannelListBo.getKeyword());
        }

        IPage<AgentSalePromotionChannelEntity> iPage = agentSalePromotionChannelService.page(new Query<AgentSalePromotionChannelEntity>().getPage(agentSalePromotionChannelListBo.getPage(), agentSalePromotionChannelListBo.getLimit()), wrapper);

        PageUtils<AgentSalePromotionChannelListVo> pageUtils = new PageUtils<>(agentSalePromotionChannelListBo.getPage(), agentSalePromotionChannelListBo.getLimit(), iPage);

        List<AgentSalePromotionChannelEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AgentSalePromotionChannelListVo> vos = records.stream().map(item -> {
                AgentSalePromotionChannelListVo agentSalePromotionChannelVo = new AgentSalePromotionChannelListVo();
                BeanUtils.copyProperties(item, agentSalePromotionChannelVo);
                return agentSalePromotionChannelVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AgentSalePromotionChannelInfoVo info(Long id) {

        AgentSalePromotionChannelEntity agentSalePromotionChannelEntity = agentSalePromotionChannelService.getById(id);
        if(agentSalePromotionChannelEntity != null) {
            AgentSalePromotionChannelInfoVo agentSalePromotionChannelInfoVo = new AgentSalePromotionChannelInfoVo();
            BeanUtils.copyProperties(agentSalePromotionChannelEntity, agentSalePromotionChannelInfoVo);
            return agentSalePromotionChannelInfoVo;
        }

        return null;
    }

    @Override
    public AgentSalePromotionChannelInfoVo save(AgentSalePromotionChannelBo agentSalePromotionChannelBo) {

         AgentSalePromotionChannelEntity agentSalePromotionChannelEntity = new AgentSalePromotionChannelEntity();
         BeanUtils.copyProperties(agentSalePromotionChannelBo, agentSalePromotionChannelEntity);
         agentSalePromotionChannelEntity.setId(SnowflakeManager.nextValue());
         agentSalePromotionChannelEntity.setCreateDate(new Date());
         agentSalePromotionChannelEntity.setUpdateDate(new Date());

         agentSalePromotionChannelService.save(agentSalePromotionChannelEntity);

         AgentSalePromotionChannelInfoVo agentSalePromotionChannelInfoVo = new AgentSalePromotionChannelInfoVo();
         BeanUtils.copyProperties(agentSalePromotionChannelEntity, agentSalePromotionChannelInfoVo);

         return agentSalePromotionChannelInfoVo;
     }

    @Override
    public void update(AgentSalePromotionChannelBo agentSalePromotionChannelBo) {

        AgentSalePromotionChannelEntity agentSalePromotionChannelEntity = new AgentSalePromotionChannelEntity();
        BeanUtils.copyProperties(agentSalePromotionChannelBo, agentSalePromotionChannelEntity);
        agentSalePromotionChannelEntity.setUpdateDate(new Date());

        agentSalePromotionChannelService.updateById(agentSalePromotionChannelEntity);
    }

    @Override
    public void deleteById(Long id) {

        agentSalePromotionChannelService.removeById(id);
    }

    @Override
    public void batchSaveSalePromotion(Long saleId, List<AgentSalePromotionChannelBo> agentSalePromotionChannelBos) {

        if(agentSalePromotionChannelBos != null && agentSalePromotionChannelBos.size() > 0) {
            List<AgentSalePromotionChannelEntity> agentSalePromotionChannelEntities = agentSalePromotionChannelBos.stream().map(item -> {
                AgentSalePromotionChannelEntity agentSalePromotionChannelEntity = new AgentSalePromotionChannelEntity();
                agentSalePromotionChannelEntity.setId(SnowflakeManager.nextValue());
                agentSalePromotionChannelEntity.setAgentSaleId(saleId);
                agentSalePromotionChannelEntity.setPromotionChannelId(item.getPromotionChannelId());
                agentSalePromotionChannelEntity.setSaleUrlCode(item.getSaleUrlCode());
                agentSalePromotionChannelEntity.setCreateDate(new Date());
                agentSalePromotionChannelEntity.setUpdateDate(new Date());
                return agentSalePromotionChannelEntity;
            }).collect(Collectors.toList());

            this.agentSalePromotionChannelService.saveBatch(agentSalePromotionChannelEntities);
        }
    }

    @Override
    public void deleteBySaleId(Long saleId) {

        this.agentSalePromotionChannelService.remove(new QueryWrapper<AgentSalePromotionChannelEntity>().eq("agent_sale_id", saleId));
    }


}

