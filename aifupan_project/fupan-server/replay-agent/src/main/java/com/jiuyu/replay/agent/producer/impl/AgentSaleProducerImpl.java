package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.AgentSaleBo;
import com.jiuyu.replay.agent.bo.AgentSaleListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.entity.AgentPromotionEntity;
import com.jiuyu.replay.agent.entity.AgentSaleEntity;
import com.jiuyu.replay.agent.entity.AgentSalePromotionChannelEntity;
import com.jiuyu.replay.agent.producer.AgentSaleProducer;
import com.jiuyu.replay.agent.repository.service.AgentPromotionService;
import com.jiuyu.replay.agent.repository.service.AgentSalePromotionChannelService;
import com.jiuyu.replay.agent.repository.service.AgentSaleService;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.vo.AgentPromotionVo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Service
public class AgentSaleProducerImpl implements AgentSaleProducer {

    @Resource
    private AgentSaleService agentSaleService;
    @Resource
    private AgentSalePromotionChannelService agentSalePromotionChannelService;
    @Resource
    private AgentPromotionService agentPromotionService;
    @Resource
    private AgentProperties agentProperties;


    @Override
    public PageUtils<AgentSaleListVo> queryPage(AgentSaleListBo agentSaleListBo) {
        QueryWrapper<AgentSaleEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(agentSaleListBo.getKeyword())){
            wrapper.like("name", agentSaleListBo.getKeyword());
        }

        IPage<AgentSaleEntity> iPage = agentSaleService.page(new Query<AgentSaleEntity>().getPage(agentSaleListBo.getPage(), agentSaleListBo.getLimit()), wrapper);

        PageUtils<AgentSaleListVo> pageUtils = new PageUtils<>(agentSaleListBo.getPage(), agentSaleListBo.getLimit(), iPage);

        List<AgentSaleEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AgentSaleListVo> vos = records.stream().map(item -> {
                AgentSaleListVo agentSaleVo = new AgentSaleListVo();
                BeanUtils.copyProperties(item, agentSaleVo);
                return agentSaleVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AgentSaleInfoVo info(Long id) {

        AgentSaleEntity agentSaleEntity = agentSaleService.getById(id);
        if(agentSaleEntity != null) {
            AgentSaleInfoVo agentSaleInfoVo = new AgentSaleInfoVo();
            BeanUtils.copyProperties(agentSaleEntity, agentSaleInfoVo);

            // 封装销售的渠道列表
            List<AgentSalePromotionChannelEntity> promotionChannelEntities = this.agentSalePromotionChannelService.list(new QueryWrapper<AgentSalePromotionChannelEntity>().eq("agent_sale_id", agentSaleInfoVo.getId()));
            if(promotionChannelEntities != null && promotionChannelEntities.size() > 0) {
                List<Long> promotionIds = promotionChannelEntities.stream().map(AgentSalePromotionChannelEntity::getPromotionChannelId).collect(Collectors.toList());
                List<AgentPromotionEntity> agentPromotionEntities = this.agentPromotionService.listByIds(promotionIds);
                if(agentPromotionEntities != null && agentPromotionEntities.size() > 0) {
                    List<AgentPromotionInfoVo> agentPromotionInfoVos = agentPromotionEntities.stream().map(item -> {
                        AgentPromotionInfoVo agentPromotionInfoVo = new AgentPromotionInfoVo();
                        BeanUtils.copyProperties(item, agentPromotionInfoVo);
                        for (AgentSalePromotionChannelEntity promotionChannelEntity : promotionChannelEntities) {
                            if(promotionChannelEntity.getPromotionChannelId().equals(item.getId())) {
                                agentPromotionInfoVo.setUrl(agentProperties.getUrl() + promotionChannelEntity.getSaleUrlCode());
                                break;
                            }
                        }

                        return agentPromotionInfoVo;
                    }).collect(Collectors.toList());
                    agentSaleInfoVo.setAgentPromotionList(agentPromotionInfoVos);
                    agentSaleInfoVo.setAgentPromotionStr(agentPromotionInfoVos.stream().map(AgentPromotionVo::getPromotionName).collect(Collectors.joining(",")));
                }
            }

            return agentSaleInfoVo;
        }

        return null;
    }

    @Override
    public AgentSaleInfoVo save(AgentSaleBo agentSaleBo) {

         AgentSaleEntity agentSaleEntity = new AgentSaleEntity();
         BeanUtils.copyProperties(agentSaleBo, agentSaleEntity);
         agentSaleEntity.setId(SnowflakeManager.nextValue());
         agentSaleEntity.setCreateDate(new Date());
         agentSaleEntity.setUpdateDate(new Date());

         agentSaleService.save(agentSaleEntity);

         AgentSaleInfoVo agentSaleInfoVo = new AgentSaleInfoVo();
         BeanUtils.copyProperties(agentSaleEntity, agentSaleInfoVo);

         return agentSaleInfoVo;
     }

    @Override
    public void update(AgentSaleBo agentSaleBo) {

        AgentSaleEntity agentSaleEntity = new AgentSaleEntity();
        BeanUtils.copyProperties(agentSaleBo, agentSaleEntity);
        agentSaleEntity.setUpdateDate(new Date());

        agentSaleService.updateById(agentSaleEntity);
    }

    @Override
    public void deleteById(Long id) {

        agentSaleService.removeById(id);
    }

    @Override
    public List<AgentSaleInfoVo> listByAgentId(Long agentId) {

        QueryWrapper<AgentSaleEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId);
        List<AgentSaleEntity> agentSaleEntities = this.agentSaleService.list(wrapper);

        if(agentSaleEntities != null && agentSaleEntities.size() > 0) {

            List<Long> agentSaleIds = agentSaleEntities.stream().map(AgentSaleEntity::getId).toList();

            // 获取代理商销售人员关联的推广渠道
            List<AgentPromotionEntity> agentPromotionEntities = null;
            List<AgentSalePromotionChannelEntity> agentSalePromotionChannelEntities = this.agentSalePromotionChannelService.list(new QueryWrapper<AgentSalePromotionChannelEntity>().in("agent_sale_id", agentSaleIds));
            if(agentSalePromotionChannelEntities != null && agentSalePromotionChannelEntities.size() > 0) {
                Set<Long> promotionIds = agentSalePromotionChannelEntities.stream().map(AgentSalePromotionChannelEntity::getPromotionChannelId).collect(Collectors.toSet());
                agentPromotionEntities = this.agentPromotionService.listByIds(promotionIds);
            }

            List<AgentPromotionEntity> finalAgentPromotionEntities = agentPromotionEntities;
            List<AgentSaleInfoVo> agentSaleInfoVos = agentSaleEntities.stream().map(item -> {
                AgentSaleInfoVo agentSaleInfoVo = new AgentSaleInfoVo();
                BeanUtils.copyProperties(item, agentSaleInfoVo);

                // 封装代理商销售人员关联的推广渠道
                List<AgentPromotionInfoVo> agentPromotionList = new LinkedList<>();
                agentSaleInfoVo.setAgentPromotionStr("");
                if (finalAgentPromotionEntities != null && finalAgentPromotionEntities.size() > 0) {
                    for (AgentSalePromotionChannelEntity agentSalePromotionChannelEntity : agentSalePromotionChannelEntities) {
                        if (agentSalePromotionChannelEntity.getAgentSaleId().equals(agentSaleInfoVo.getId())) {
                            for (AgentPromotionEntity agentPromotionEntity : finalAgentPromotionEntities) {
                                if (agentPromotionEntity.getId().equals(agentSalePromotionChannelEntity.getPromotionChannelId())) {
                                    AgentPromotionInfoVo agentPromotionInfoVo = new AgentPromotionInfoVo();
                                    BeanUtils.copyProperties(agentPromotionEntity, agentPromotionInfoVo);
                                    agentPromotionInfoVo.setUrl(agentProperties.getUrl() + agentPromotionInfoVo.getPromotionUrlCode());
                                    agentPromotionList.add(agentPromotionInfoVo);
                                    break;
                                }
                            }
                        }
                    }
                }
                agentSaleInfoVo.setAgentPromotionList(agentPromotionList);
                if (agentPromotionList.size() > 0) {
                    agentSaleInfoVo.setAgentPromotionStr(agentPromotionList.stream().map(AgentPromotionVo::getPromotionName).collect(Collectors.joining(",")));
                }

                return agentSaleInfoVo;

            }).collect(Collectors.toList());

            return agentSaleInfoVos;
        }

        return null;
    }

    @Override
    public List<AgentSaleInfoVo> listByIds(Collection<Long> agentSaleIds) {

        if(agentSaleIds != null && agentSaleIds.size() > 0) {
            List<AgentSaleEntity> agentSaleEntities = this.agentSaleService.listByIds(agentSaleIds);
            if(agentSaleEntities != null && agentSaleEntities.size() > 0) {
                List<AgentSaleInfoVo> agentSaleInfoVos = agentSaleEntities.stream().map(item -> {
                    AgentSaleInfoVo agentSaleInfoVo = new AgentSaleInfoVo();
                    BeanUtils.copyProperties(item, agentSaleInfoVo);
                    return agentSaleInfoVo;
                }).collect(Collectors.toList());

                return agentSaleInfoVos;
            }
        }

        return null;
    }


}

