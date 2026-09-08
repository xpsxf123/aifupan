package com.jiuyu.replay.agent.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressListBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardBo;
import com.jiuyu.replay.agent.entity.ClientInviteProgressEntity;
import com.jiuyu.replay.agent.entity.ClientInviteProgressRewardEntity;
import com.jiuyu.replay.agent.producer.ClientInviteProgressProducer;
import com.jiuyu.replay.agent.repository.service.ClientInviteProgressRewardService;
import com.jiuyu.replay.agent.repository.service.ClientInviteProgressService;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressListVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteProgressProducerImpl implements ClientInviteProgressProducer {

    @Resource
    private ClientInviteProgressService clientInviteProgressService;
    @Resource
    private ClientInviteProgressRewardService clientInviteProgressRewardService;


    @Override
    public PageUtils<ClientInviteProgressListVo> queryPage(ClientInviteProgressListBo clientInviteProgressListBo) {
        QueryWrapper<ClientInviteProgressEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientInviteProgressListBo.getKeyword())){
            wrapper.like("name", clientInviteProgressListBo.getKeyword());
        }

        IPage<ClientInviteProgressEntity> iPage = clientInviteProgressService.page(new Query<ClientInviteProgressEntity>().getPage(clientInviteProgressListBo.getPage(), clientInviteProgressListBo.getLimit()), wrapper);

        PageUtils<ClientInviteProgressListVo> pageUtils = new PageUtils<>(clientInviteProgressListBo.getPage(), clientInviteProgressListBo.getLimit(), iPage);

        List<ClientInviteProgressEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ClientInviteProgressListVo> vos = records.stream().map(item -> {
                ClientInviteProgressListVo clientInviteProgressVo = new ClientInviteProgressListVo();
                BeanUtils.copyProperties(item, clientInviteProgressVo);
                return clientInviteProgressVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientInviteProgressInfoVo info(Long id) {

        ClientInviteProgressEntity clientInviteProgressEntity = clientInviteProgressService.getById(id);
        if(clientInviteProgressEntity != null) {
            ClientInviteProgressInfoVo clientInviteProgressInfoVo = new ClientInviteProgressInfoVo();
            BeanUtils.copyProperties(clientInviteProgressEntity, clientInviteProgressInfoVo);
            return clientInviteProgressInfoVo;
        }

        return null;
    }

    @Override
    public ClientInviteProgressInfoVo save(ClientInviteProgressBo clientInviteProgressBo) {

         ClientInviteProgressEntity clientInviteProgressEntity = new ClientInviteProgressEntity();
         BeanUtils.copyProperties(clientInviteProgressBo, clientInviteProgressEntity);
         clientInviteProgressEntity.setId(SnowflakeManager.nextValue());
         clientInviteProgressEntity.setCreateDate(new Date());
         clientInviteProgressEntity.setUpdateDate(new Date());

         clientInviteProgressService.save(clientInviteProgressEntity);

         ClientInviteProgressInfoVo clientInviteProgressInfoVo = new ClientInviteProgressInfoVo();
         BeanUtils.copyProperties(clientInviteProgressEntity, clientInviteProgressInfoVo);

         return clientInviteProgressInfoVo;
     }

    @Override
    public void update(ClientInviteProgressBo clientInviteProgressBo) {

        ClientInviteProgressEntity clientInviteProgressEntity = new ClientInviteProgressEntity();
        BeanUtils.copyProperties(clientInviteProgressBo, clientInviteProgressEntity);
        clientInviteProgressEntity.setUpdateDate(new Date());

        clientInviteProgressService.updateById(clientInviteProgressEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientInviteProgressService.removeById(id);
    }

    @Override
    public void disableOld(Long inviteActivityId) {

        List<ClientInviteProgressEntity> list = this.clientInviteProgressService.list(new QueryWrapper<ClientInviteProgressEntity>().eq("invite_activity_id", inviteActivityId));
        if(list != null && list.size() > 0) {
            for (ClientInviteProgressEntity clientInviteProgressEntity : list) {
                clientInviteProgressEntity.setInviteProgressStatus(0);
                clientInviteProgressEntity.setUpdateDate(new Date());
                this.clientInviteProgressService.updateBatchById(list);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<ClientInviteProgressBo> clientInviteProgressBoList) {

        if(clientInviteProgressBoList != null && clientInviteProgressBoList.size() > 0) {

            List<ClientInviteProgressEntity> clientInviteProgressEntities = new LinkedList<>();
            List<ClientInviteProgressRewardEntity> clientInviteProgressRewardEntities = new LinkedList<>();

            for (ClientInviteProgressBo clientInviteProgressBo : clientInviteProgressBoList) {
                ClientInviteProgressEntity clientInviteProgressEntity = new ClientInviteProgressEntity();
                BeanUtils.copyProperties(clientInviteProgressBo, clientInviteProgressEntity);
                clientInviteProgressEntity.setId(SnowflakeManager.nextValue());
                clientInviteProgressEntity.setInviteProgressStatus(1);
                clientInviteProgressEntity.setCreateDate(new Date());
                clientInviteProgressEntity.setUpdateDate(new Date());
                clientInviteProgressEntities.add(clientInviteProgressEntity);

                List<ClientInviteProgressRewardBo> clientInviteProgressRewardBos = clientInviteProgressBo.getRewardList();
                if(clientInviteProgressRewardBos != null && clientInviteProgressRewardBos.size() > 0) {
                    for (ClientInviteProgressRewardBo clientInviteProgressRewardBo : clientInviteProgressRewardBos) {
                        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = new ClientInviteProgressRewardEntity();
                        BeanUtils.copyProperties(clientInviteProgressRewardBo, clientInviteProgressRewardEntity);
                        clientInviteProgressRewardEntity.setId(SnowflakeManager.nextValue());
                        clientInviteProgressRewardEntity.setProgressId(clientInviteProgressEntity.getId());
                        clientInviteProgressRewardEntity.setCreateDate(new Date());
                        clientInviteProgressRewardEntity.setUpdateDate(new Date());
                        clientInviteProgressRewardEntities.add(clientInviteProgressRewardEntity);
                    }
                }
            }

            if(clientInviteProgressEntities.size() > 0) {
                this.clientInviteProgressService.saveBatch(clientInviteProgressEntities);
            }
            if(clientInviteProgressRewardEntities.size() > 0) {
                this.clientInviteProgressRewardService.saveBatch(clientInviteProgressRewardEntities);
            }

        }

    }

    @Override
    public List<ClientInviteProgressInfoVo> listAllActivity(Long inviteActivityId, Integer inviteProgressType) {

        QueryWrapper<ClientInviteProgressEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("invite_progress_status", 1);
        if(inviteActivityId != null) {
            wrapper.eq("invite_activity_id", inviteActivityId);
        }
        if(inviteProgressType != null) {
            wrapper.eq("invite_progress_type", inviteProgressType);
        }
        wrapper.orderByDesc("invite_progress_type");
        wrapper.orderByAsc("invite_progress_value");
        List<ClientInviteProgressEntity> inviteProgressEntities = this.clientInviteProgressService.list(wrapper);

        if(inviteProgressEntities != null && inviteProgressEntities.size() > 0) {


            List<Long> progressIds = inviteProgressEntities.stream().map(ClientInviteProgressEntity::getId).collect(Collectors.toList());
            List<ClientInviteProgressRewardEntity> clientInviteProgressRewardEntities = this.clientInviteProgressRewardService.list(new QueryWrapper<ClientInviteProgressRewardEntity>().in("progress_id", progressIds));

            List<ClientInviteProgressInfoVo> clientInviteProgressInfoVos = inviteProgressEntities.stream().map(item -> {
                ClientInviteProgressInfoVo clientInviteProgressInfoVo = new ClientInviteProgressInfoVo();
                BeanUtils.copyProperties(item, clientInviteProgressInfoVo);
                // 封装进度的奖励列表
                List<ClientInviteProgressRewardInfoVo> rewardList = new LinkedList<>();
                if(clientInviteProgressRewardEntities != null && clientInviteProgressRewardEntities.size() > 0) {
                    for (ClientInviteProgressRewardEntity clientInviteProgressRewardEntity : clientInviteProgressRewardEntities) {
                        if(clientInviteProgressRewardEntity.getProgressId().equals(item.getId())) {
                            ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
                            BeanUtils.copyProperties(clientInviteProgressRewardEntity, clientInviteProgressRewardInfoVo);
                            rewardList.add(clientInviteProgressRewardInfoVo);
                        }
                    }
                }
                clientInviteProgressInfoVo.setRewardList(rewardList);

                return clientInviteProgressInfoVo;
            }).collect(Collectors.toList());

            return clientInviteProgressInfoVos;
        }

        return null;
    }


}

