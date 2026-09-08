package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.agent.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.agent.producer.ClientInviteRewardRecordProducer;
import com.jiuyu.replay.agent.repository.service.ClientInviteRewardRecordService;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordListVo;
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
 * 邀请奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteRewardRecordProducerImpl implements ClientInviteRewardRecordProducer {

    @Resource
    private ClientInviteRewardRecordService clientInviteRewardRecordService;


    @Override
    public PageUtils<ClientInviteRewardRecordListVo> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo) {
        QueryWrapper<ClientInviteRewardRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientInviteRewardRecordListBo.getKeyword())){
            wrapper.like("name", clientInviteRewardRecordListBo.getKeyword());
        }

        IPage<ClientInviteRewardRecordEntity> iPage = clientInviteRewardRecordService.page(new Query<ClientInviteRewardRecordEntity>().getPage(clientInviteRewardRecordListBo.getPage(), clientInviteRewardRecordListBo.getLimit()), wrapper);

        PageUtils<ClientInviteRewardRecordListVo> pageUtils = new PageUtils<>(clientInviteRewardRecordListBo.getPage(), clientInviteRewardRecordListBo.getLimit(), iPage);

        List<ClientInviteRewardRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ClientInviteRewardRecordListVo> vos = records.stream().map(item -> {
                ClientInviteRewardRecordListVo clientInviteRewardRecordVo = new ClientInviteRewardRecordListVo();
                BeanUtils.copyProperties(item, clientInviteRewardRecordVo);
                return clientInviteRewardRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientInviteRewardRecordInfoVo info(Long id) {

        ClientInviteRewardRecordEntity clientInviteRewardRecordEntity = clientInviteRewardRecordService.getById(id);
        if(clientInviteRewardRecordEntity != null) {
            ClientInviteRewardRecordInfoVo clientInviteRewardRecordInfoVo = new ClientInviteRewardRecordInfoVo();
            BeanUtils.copyProperties(clientInviteRewardRecordEntity, clientInviteRewardRecordInfoVo);
            return clientInviteRewardRecordInfoVo;
        }

        return null;
    }

    @Override
    public ClientInviteRewardRecordInfoVo save(ClientInviteRewardRecordBo clientInviteRewardRecordBo) {

         ClientInviteRewardRecordEntity clientInviteRewardRecordEntity = new ClientInviteRewardRecordEntity();
         BeanUtils.copyProperties(clientInviteRewardRecordBo, clientInviteRewardRecordEntity);
         clientInviteRewardRecordEntity.setId(SnowflakeManager.nextValue());
         clientInviteRewardRecordEntity.setCreateDate(new Date());
         clientInviteRewardRecordEntity.setUpdateDate(new Date());

         clientInviteRewardRecordService.save(clientInviteRewardRecordEntity);

         ClientInviteRewardRecordInfoVo clientInviteRewardRecordInfoVo = new ClientInviteRewardRecordInfoVo();
         BeanUtils.copyProperties(clientInviteRewardRecordEntity, clientInviteRewardRecordInfoVo);

         return clientInviteRewardRecordInfoVo;
     }

    @Override
    public void update(ClientInviteRewardRecordBo clientInviteRewardRecordBo) {

        ClientInviteRewardRecordEntity clientInviteRewardRecordEntity = new ClientInviteRewardRecordEntity();
        BeanUtils.copyProperties(clientInviteRewardRecordBo, clientInviteRewardRecordEntity);
        clientInviteRewardRecordEntity.setUpdateDate(new Date());

        clientInviteRewardRecordService.updateById(clientInviteRewardRecordEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientInviteRewardRecordService.removeById(id);
    }


}

