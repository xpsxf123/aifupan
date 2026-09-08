package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.ClientInviteActivityBo;
import com.jiuyu.replay.agent.bo.ClientInviteActivityListBo;
import com.jiuyu.replay.agent.entity.ClientInviteActivityEntity;
import com.jiuyu.replay.agent.producer.ClientInviteActivityProducer;
import com.jiuyu.replay.agent.repository.service.ClientInviteActivityService;
import com.jiuyu.replay.agent.vo.ClientInviteActivityInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteActivityListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
//@Service
public class ClientInviteActivityProducerImpl implements ClientInviteActivityProducer {

    @Resource
    private ClientInviteActivityService clientInviteActivityService;


    @Override
    public PageUtils<ClientInviteActivityListVo> queryPage(ClientInviteActivityListBo clientInviteActivityListBo) {
        QueryWrapper<ClientInviteActivityEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientInviteActivityListBo.getKeyword())){
            wrapper.like("name", clientInviteActivityListBo.getKeyword());
        }

        IPage<ClientInviteActivityEntity> iPage = clientInviteActivityService.page(new Query<ClientInviteActivityEntity>().getPage(clientInviteActivityListBo.getPage(), clientInviteActivityListBo.getLimit()), wrapper);

        PageUtils<ClientInviteActivityListVo> pageUtils = new PageUtils<>(clientInviteActivityListBo.getPage(), clientInviteActivityListBo.getLimit(), iPage);

        List<ClientInviteActivityEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ClientInviteActivityListVo> vos = records.stream().map(item -> {
                ClientInviteActivityListVo clientInviteActivityVo = new ClientInviteActivityListVo();
                BeanUtils.copyProperties(item, clientInviteActivityVo);
                return clientInviteActivityVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientInviteActivityInfoVo info(Long id, Integer activityStatus) {

        QueryWrapper<ClientInviteActivityEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        if(activityStatus != null) {
            wrapper.eq("activity_status", activityStatus);
        }

        ClientInviteActivityEntity clientInviteActivityEntity = clientInviteActivityService.getOne(wrapper);
        if(clientInviteActivityEntity != null) {
            ClientInviteActivityInfoVo clientInviteActivityInfoVo = new ClientInviteActivityInfoVo();
            BeanUtils.copyProperties(clientInviteActivityEntity, clientInviteActivityInfoVo);
            return clientInviteActivityInfoVo;
        }

        return null;
    }

    @Override
    public ClientInviteActivityInfoVo save(ClientInviteActivityBo clientInviteActivityBo) {

         ClientInviteActivityEntity clientInviteActivityEntity = new ClientInviteActivityEntity();
         BeanUtils.copyProperties(clientInviteActivityBo, clientInviteActivityEntity);
         clientInviteActivityEntity.setId(SnowflakeManager.nextValue());
         clientInviteActivityEntity.setCreateDate(new Date());
         clientInviteActivityEntity.setUpdateDate(new Date());

         clientInviteActivityService.save(clientInviteActivityEntity);

         ClientInviteActivityInfoVo clientInviteActivityInfoVo = new ClientInviteActivityInfoVo();
         BeanUtils.copyProperties(clientInviteActivityEntity, clientInviteActivityInfoVo);

         return clientInviteActivityInfoVo;
     }

    @Override
    public void update(ClientInviteActivityBo clientInviteActivityBo) {

        ClientInviteActivityEntity clientInviteActivityEntity = new ClientInviteActivityEntity();
        BeanUtils.copyProperties(clientInviteActivityBo, clientInviteActivityEntity);
        clientInviteActivityEntity.setUpdateDate(new Date());

        clientInviteActivityService.updateById(clientInviteActivityEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientInviteActivityService.removeById(id);
    }


}

