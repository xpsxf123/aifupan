package com.jiuyu.replay.agent.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.AgentBo;
import com.jiuyu.replay.agent.bo.AgentListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.entity.AgentEntity;
import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.agent.repository.service.AgentService;
import com.jiuyu.replay.agent.repository.service.ChannelService;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentListVo;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
public class AgentProducerImpl implements AgentProducer {

    @Resource
    private AgentService agentService;
    @Resource
    private AgentProperties agentProperties;
    @Resource
    private ChannelService channelService;


    @Override
    public PageUtils<AgentListVo> queryPage(AgentListBo agentListBo) {
        LambdaQueryWrapper<AgentEntity> wrapper = new LambdaQueryWrapper<AgentEntity>()
                .in(ObjectUtil.isNotEmpty(agentListBo.getChannelIds()), AgentEntity::getChannelId, agentListBo.getChannelIds())
                .eq(ObjectUtil.isNotEmpty(agentListBo.getOperationUserId()), AgentEntity::getOperationUserId, agentListBo.getOperationUserId())
                .eq(ObjectUtil.isNotEmpty(agentListBo.getEmployeeStatus()), AgentEntity::getEmployeeStatus, agentListBo.getEmployeeStatus())
                .eq(ObjectUtil.isNotEmpty(agentListBo.getAgentType()), AgentEntity::getAgentType, agentListBo.getAgentType())
                .like(ObjectUtil.isNotEmpty(agentListBo.getAgentName()), AgentEntity::getAgentName, agentListBo.getAgentName())
                .and(ObjectUtil.isNotEmpty(agentListBo.getContactKeyword()), w -> {
                    w.like(AgentEntity::getContactName, agentListBo.getContactKeyword()).or().like(AgentEntity::getContactPhone, agentListBo.getContactKeyword());
                });

        IPage<AgentEntity> iPage = agentService.page(new Query<AgentEntity>().getPage(agentListBo.getPage(), agentListBo.getLimit()), wrapper);

        PageUtils<AgentListVo> pageUtils = new PageUtils<>(agentListBo.getPage(), agentListBo.getLimit(), iPage);

        List<AgentEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AgentListVo> vos = records.stream().map(item -> {
                AgentListVo agentVo = new AgentListVo();
                BeanUtils.copyProperties(item, agentVo);
                agentVo.setUrl(agentProperties.getUrl() + agentVo.getAgentUrlCode());
                return agentVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AgentInfoVo info(Long id, Integer agentStatus) {

        QueryWrapper<AgentEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        if(agentStatus != null) {
            wrapper.eq("agent_status", agentStatus);
        }

        AgentEntity agentEntity = agentService.getOne(wrapper);
        if(agentEntity != null) {
            AgentInfoVo agentInfoVo = new AgentInfoVo();
            BeanUtils.copyProperties(agentEntity, agentInfoVo);
            return agentInfoVo;
        }

        return null;
    }

    @Override
    public AgentInfoVo save(AgentBo agentBo) {

         AgentEntity agentEntity = new AgentEntity();
         BeanUtils.copyProperties(agentBo, agentEntity);
         agentEntity.setCreateDate(new Date());
         agentEntity.setUpdateDate(new Date());

         agentService.save(agentEntity);

         AgentInfoVo agentInfoVo = new AgentInfoVo();
         BeanUtils.copyProperties(agentEntity, agentInfoVo);

         return agentInfoVo;
     }

    @Override
    public void update(AgentBo agentBo) {

        AgentEntity agentEntity = new AgentEntity();
        BeanUtils.copyProperties(agentBo, agentEntity);
        agentEntity.setUpdateDate(new Date());

        agentService.updateById(agentEntity);
    }

    @Override
    public void deleteById(Long id) {

        agentService.removeById(id);
    }

    @Override
    public AgentInfoVo infoByChannelId(Long channelId) {

        AgentEntity agentEntity = this.agentService.getOne(new QueryWrapper<AgentEntity>().eq("channel_id", channelId));
        if(agentEntity != null) {
            AgentInfoVo agentInfoVo = new AgentInfoVo();
            BeanUtils.copyProperties(agentEntity, agentInfoVo);
            return agentInfoVo;
        }

        return null;
    }

    @Override
    public AgentInfoVo infoByUrlCode(String inviteUrlCode) {

        AgentEntity agentEntity = this.agentService.getOne(new QueryWrapper<AgentEntity>().eq("agent_url_code", inviteUrlCode));
        if(agentEntity != null) {
            AgentInfoVo agentInfoVo = new AgentInfoVo();
            BeanUtils.copyProperties(agentEntity, agentInfoVo);
            return agentInfoVo;
        }
        return null;
    }

    @Override
    public AgentInfoVo getClientDefaultAgent() {

        List<AgentEntity> agentEntities = this.agentService.list(new QueryWrapper<AgentEntity>().eq("is_client_invite_agent", 1).orderByDesc("id"));
        if(agentEntities != null && agentEntities.size() > 0) {
            AgentEntity agentEntity = agentEntities.get(0);
            AgentInfoVo agentInfoVo = new AgentInfoVo();
            BeanUtils.copyProperties(agentEntity, agentInfoVo);
            return agentInfoVo;
        }

        return null;
    }

    @Override
    public AgentInfoVo getByChannelId(String channelId) {
        AgentEntity agent = agentService.getOne(new LambdaQueryWrapper<AgentEntity>()
                .eq(AgentEntity::getChannelId, channelId)
                .last("limit 1")
        );
        if (agent != null) {
            return BeanConvertUtils.convert(agent, AgentInfoVo.class);
        }
        return null;
    }

    @Override
    public long countAgentByPhoneType(String contactPhone, Integer type, Long noAgentId) {
        RRException.isNotEmpty(contactPhone, "手机号不能为空");
        RRException.isNotEmpty(type, "类型不能为空");
        return agentService.lambdaQuery()
                .eq(AgentEntity::getContactPhone, contactPhone)
                .eq(AgentEntity::getAgentType, type)
                .ne(ObjectUtil.isNotEmpty(noAgentId), AgentEntity::getId, noAgentId)
                .count();
    }

    @Override
    public AgentInfoVo infoByPhoneType(String phone, Integer type) {
        AgentEntity agent = agentService.lambdaQuery()
                .eq(AgentEntity::getContactPhone, phone)
                .eq(AgentEntity::getAgentType, type)
                .last("limit 1")
                .one();
        if (agent != null) {
            return BeanConvertUtils.convert(agent, AgentInfoVo.class);
        }
        return null;
    }

    @Override
    public List<AgentInfoVo> listByIds(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<AgentEntity> agents = agentService.lambdaQuery()
                .in(AgentEntity::getId, agentIds)
                .list();

        return agents.stream()
                .map(agent -> BeanConvertUtils.convert(agent, AgentInfoVo.class))
                .collect(Collectors.toList());
    }

    @Override
    public void updateAgentNameByChannelId(Long channelId, String channelName) {
        agentService.lambdaUpdate()
                .eq(AgentEntity::getChannelId, channelId)
                .set(AgentEntity::getAgentName, channelName)
                .update();
    }

    @Override
    public void updateNameAndPhoneByUserId(Long userId, String nickName, String phone) {
        if (userId != null && (ObjectUtil.isNotEmpty(nickName) || ObjectUtil.isNotEmpty(phone))) {
            agentService.lambdaUpdate()
                    .eq(AgentEntity::getUserId, userId)
                    .set(AgentEntity::getContactName, nickName)
                    .set(AgentEntity::getContactPhone, phone)
                    .update();
        }
    }

    @Override
    public AgentInfoVo getBySalesUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        AgentEntity one = agentService.lambdaQuery()
                .eq(AgentEntity::getUserId, userId)
                .last("limit 1")
                .one();
        if (one != null) {
            return BeanConvertUtils.convert(one, AgentInfoVo.class);
        }
        return null;
    }

    @Override
    public void updateEmployeeStatus(Long id, Integer employeeStatus) {
        Integer agentStatus = null;
        if (employeeStatus == 0) {
            agentStatus = 0;
        }
        agentService.lambdaUpdate()
                .eq(AgentEntity::getId, id)
                .set(AgentEntity::getEmployeeStatus, employeeStatus)
                .set(ObjectUtil.isNotEmpty(agentStatus), AgentEntity::getAgentStatus, agentStatus)
                .update();
    }
}

