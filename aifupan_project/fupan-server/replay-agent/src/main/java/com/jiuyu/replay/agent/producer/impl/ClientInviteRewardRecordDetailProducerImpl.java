package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailListBo;
import com.jiuyu.replay.agent.entity.ClientInviteRewardRecordDetailEntity;
import com.jiuyu.replay.agent.producer.ClientInviteRewardRecordDetailProducer;
import com.jiuyu.replay.agent.repository.service.ClientInviteRewardRecordDetailService;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailListVo;
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
 * 邀请奖励明细记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteRewardRecordDetailProducerImpl implements ClientInviteRewardRecordDetailProducer {

    @Resource
    private ClientInviteRewardRecordDetailService clientInviteRewardRecordDetailService;


    @Override
    public PageUtils<ClientInviteRewardRecordDetailListVo> queryPage(ClientInviteRewardRecordDetailListBo clientInviteRewardRecordDetailListBo) {
        QueryWrapper<ClientInviteRewardRecordDetailEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientInviteRewardRecordDetailListBo.getKeyword())){
            wrapper.like("name", clientInviteRewardRecordDetailListBo.getKeyword());
        }

        IPage<ClientInviteRewardRecordDetailEntity> iPage = clientInviteRewardRecordDetailService.page(new Query<ClientInviteRewardRecordDetailEntity>().getPage(clientInviteRewardRecordDetailListBo.getPage(), clientInviteRewardRecordDetailListBo.getLimit()), wrapper);

        PageUtils<ClientInviteRewardRecordDetailListVo> pageUtils = new PageUtils<>(clientInviteRewardRecordDetailListBo.getPage(), clientInviteRewardRecordDetailListBo.getLimit(), iPage);

        List<ClientInviteRewardRecordDetailEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ClientInviteRewardRecordDetailListVo> vos = records.stream().map(item -> {
                ClientInviteRewardRecordDetailListVo clientInviteRewardRecordDetailVo = new ClientInviteRewardRecordDetailListVo();
                BeanUtils.copyProperties(item, clientInviteRewardRecordDetailVo);
                return clientInviteRewardRecordDetailVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientInviteRewardRecordDetailInfoVo info(Long id) {

        ClientInviteRewardRecordDetailEntity clientInviteRewardRecordDetailEntity = clientInviteRewardRecordDetailService.getById(id);
        if(clientInviteRewardRecordDetailEntity != null) {
            ClientInviteRewardRecordDetailInfoVo clientInviteRewardRecordDetailInfoVo = new ClientInviteRewardRecordDetailInfoVo();
            BeanUtils.copyProperties(clientInviteRewardRecordDetailEntity, clientInviteRewardRecordDetailInfoVo);
            return clientInviteRewardRecordDetailInfoVo;
        }

        return null;
    }

    @Override
    public ClientInviteRewardRecordDetailInfoVo save(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo) {

         ClientInviteRewardRecordDetailEntity clientInviteRewardRecordDetailEntity = new ClientInviteRewardRecordDetailEntity();
         BeanUtils.copyProperties(clientInviteRewardRecordDetailBo, clientInviteRewardRecordDetailEntity);
         clientInviteRewardRecordDetailEntity.setId(SnowflakeManager.nextValue());
         clientInviteRewardRecordDetailEntity.setCreateDate(new Date());
         clientInviteRewardRecordDetailEntity.setUpdateDate(new Date());

         clientInviteRewardRecordDetailService.save(clientInviteRewardRecordDetailEntity);

         ClientInviteRewardRecordDetailInfoVo clientInviteRewardRecordDetailInfoVo = new ClientInviteRewardRecordDetailInfoVo();
         BeanUtils.copyProperties(clientInviteRewardRecordDetailEntity, clientInviteRewardRecordDetailInfoVo);

         return clientInviteRewardRecordDetailInfoVo;
     }

    @Override
    public void update(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo) {

        ClientInviteRewardRecordDetailEntity clientInviteRewardRecordDetailEntity = new ClientInviteRewardRecordDetailEntity();
        BeanUtils.copyProperties(clientInviteRewardRecordDetailBo, clientInviteRewardRecordDetailEntity);
        clientInviteRewardRecordDetailEntity.setUpdateDate(new Date());

        clientInviteRewardRecordDetailService.updateById(clientInviteRewardRecordDetailEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientInviteRewardRecordDetailService.removeById(id);
    }


}

