package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.agent.entity.ClientInviteProgressRewardEntity;
import com.jiuyu.replay.agent.producer.ClientInviteProgressRewardProducer;
import com.jiuyu.replay.agent.repository.service.ClientInviteProgressRewardService;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardListVo;
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
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteProgressRewardProducerImpl implements ClientInviteProgressRewardProducer {

    @Resource
    private ClientInviteProgressRewardService clientInviteProgressRewardService;


    @Override
    public PageUtils<ClientInviteProgressRewardListVo> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo) {
        QueryWrapper<ClientInviteProgressRewardEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientInviteProgressRewardListBo.getKeyword())){
            wrapper.like("name", clientInviteProgressRewardListBo.getKeyword());
        }

        IPage<ClientInviteProgressRewardEntity> iPage = clientInviteProgressRewardService.page(new Query<ClientInviteProgressRewardEntity>().getPage(clientInviteProgressRewardListBo.getPage(), clientInviteProgressRewardListBo.getLimit()), wrapper);

        PageUtils<ClientInviteProgressRewardListVo> pageUtils = new PageUtils<>(clientInviteProgressRewardListBo.getPage(), clientInviteProgressRewardListBo.getLimit(), iPage);

        List<ClientInviteProgressRewardEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ClientInviteProgressRewardListVo> vos = records.stream().map(item -> {
                ClientInviteProgressRewardListVo clientInviteProgressRewardVo = new ClientInviteProgressRewardListVo();
                BeanUtils.copyProperties(item, clientInviteProgressRewardVo);
                return clientInviteProgressRewardVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientInviteProgressRewardInfoVo info(Long id) {

        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = clientInviteProgressRewardService.getById(id);
        if(clientInviteProgressRewardEntity != null) {
            ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
            BeanUtils.copyProperties(clientInviteProgressRewardEntity, clientInviteProgressRewardInfoVo);
            return clientInviteProgressRewardInfoVo;
        }

        return null;
    }

    @Override
    public ClientInviteProgressRewardInfoVo save(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

         ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = new ClientInviteProgressRewardEntity();
         BeanUtils.copyProperties(clientInviteProgressRewardBo, clientInviteProgressRewardEntity);
         clientInviteProgressRewardEntity.setId(SnowflakeManager.nextValue());
         clientInviteProgressRewardEntity.setCreateDate(new Date());
         clientInviteProgressRewardEntity.setUpdateDate(new Date());

         clientInviteProgressRewardService.save(clientInviteProgressRewardEntity);

         ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
         BeanUtils.copyProperties(clientInviteProgressRewardEntity, clientInviteProgressRewardInfoVo);

         return clientInviteProgressRewardInfoVo;
     }

    @Override
    public void update(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = new ClientInviteProgressRewardEntity();
        BeanUtils.copyProperties(clientInviteProgressRewardBo, clientInviteProgressRewardEntity);
        clientInviteProgressRewardEntity.setUpdateDate(new Date());

        clientInviteProgressRewardService.updateById(clientInviteProgressRewardEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientInviteProgressRewardService.removeById(id);
    }


}

