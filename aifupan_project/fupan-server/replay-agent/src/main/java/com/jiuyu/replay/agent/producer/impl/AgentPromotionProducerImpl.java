package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.AgentPromotionBo;
import com.jiuyu.replay.agent.bo.AgentPromotionListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.entity.AgentPromotionEntity;
import com.jiuyu.replay.agent.producer.AgentPromotionProducer;
import com.jiuyu.replay.agent.repository.service.AgentPromotionService;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.vo.AgentPromotionListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;
import com.jiuyu.replay.common.utils.Query;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 代理商推广渠道
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
public class AgentPromotionProducerImpl implements AgentPromotionProducer {

    @Resource
    private AgentPromotionService agentPromotionService;
    @Resource
    private AgentProperties agentProperties;


    @Override
    public PageUtils<AgentPromotionListVo> queryPage(AgentPromotionListBo agentPromotionListBo) {
        QueryWrapper<AgentPromotionEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(agentPromotionListBo.getKeyword())){
            wrapper.like("name", agentPromotionListBo.getKeyword());
        }

        IPage<AgentPromotionEntity> iPage = agentPromotionService.page(new Query<AgentPromotionEntity>().getPage(agentPromotionListBo.getPage(), agentPromotionListBo.getLimit()), wrapper);

        PageUtils<AgentPromotionListVo> pageUtils = new PageUtils<>(agentPromotionListBo.getPage(), agentPromotionListBo.getLimit(), iPage);

        List<AgentPromotionEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AgentPromotionListVo> vos = records.stream().map(item -> {
                AgentPromotionListVo agentPromotionVo = new AgentPromotionListVo();
                BeanUtils.copyProperties(item, agentPromotionVo);
                return agentPromotionVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AgentPromotionInfoVo info(Long id) {

        AgentPromotionEntity agentPromotionEntity = agentPromotionService.getById(id);
        if(agentPromotionEntity != null) {
            AgentPromotionInfoVo agentPromotionInfoVo = new AgentPromotionInfoVo();
            BeanUtils.copyProperties(agentPromotionEntity, agentPromotionInfoVo);
            return agentPromotionInfoVo;
        }

        return null;
    }

    @Override
    public AgentPromotionInfoVo save(AgentPromotionBo agentPromotionBo) {

         AgentPromotionEntity agentPromotionEntity = new AgentPromotionEntity();
         BeanUtils.copyProperties(agentPromotionBo, agentPromotionEntity);
         agentPromotionEntity.setCreateDate(new Date());
         agentPromotionEntity.setUpdateDate(new Date());

         agentPromotionService.save(agentPromotionEntity);

         AgentPromotionInfoVo agentPromotionInfoVo = new AgentPromotionInfoVo();
         BeanUtils.copyProperties(agentPromotionEntity, agentPromotionInfoVo);

         return agentPromotionInfoVo;
     }

    @Override
    public void update(AgentPromotionBo agentPromotionBo) {

        AgentPromotionEntity agentPromotionEntity = new AgentPromotionEntity();
        BeanUtils.copyProperties(agentPromotionBo, agentPromotionEntity);
        agentPromotionEntity.setUpdateDate(new Date());

        agentPromotionService.updateById(agentPromotionEntity);
    }

    @Override
    public void deleteById(Long id) {

        agentPromotionService.removeById(id);
    }

    @Override
    public List<AgentPromotionInfoVo> listByAgentId(Long agentId) {

        QueryWrapper<AgentPromotionEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId);
        List<AgentPromotionEntity> agentPromotionEntities = this.agentPromotionService.list(wrapper);

        if(agentPromotionEntities != null && agentPromotionEntities.size() > 0) {
            List<AgentPromotionInfoVo> agentPromotionInfoVos = agentPromotionEntities.stream().map(item -> {
                AgentPromotionInfoVo agentPromotionInfoVo = new AgentPromotionInfoVo();
                BeanUtils.copyProperties(item, agentPromotionInfoVo);
                agentPromotionInfoVo.setUrl(agentProperties.getUrl() + agentPromotionInfoVo.getPromotionUrlCode());
                return agentPromotionInfoVo;
            }).collect(Collectors.toList());

            return agentPromotionInfoVos;
        }

        return null;
    }


    @Override
    public List<InviteUrlPromotionVo> getNameById(List<InviteUrlPromotionVo> urlPromotionVos) {
        if (urlPromotionVos!= null && !urlPromotionVos.isEmpty()){
            List<Long> ids = urlPromotionVos.stream().map(InviteUrlPromotionVo::getPromotionId).toList();
            List<AgentPromotionEntity> agentPromotionEntities = agentPromotionService.listByIds(ids);
            if (agentPromotionEntities != null && !agentPromotionEntities.isEmpty()){
                Map<Long, String> collect = agentPromotionEntities.stream().collect(Collectors.toMap(AgentPromotionEntity::getId, AgentPromotionEntity::getPromotionName));
                urlPromotionVos.forEach(item -> item.setPromotionName(collect.get(item.getPromotionId())));
                return urlPromotionVos;
            }
        }
        return urlPromotionVos;
    }



    @Override
    public List<Long> selectQuery(AgentPromotionBo agentPromotionBo) {
        return agentPromotionService.lambdaQuery()
                .like(StringUtil.isNotBlank(agentPromotionBo.getPromotionName())
                        ,AgentPromotionEntity::getPromotionName, agentPromotionBo.getPromotionName())
                .select(AgentPromotionEntity::getId)
                .list().stream().map(AgentPromotionEntity::getId).filter(Objects::nonNull).distinct().toList();
    }


}

