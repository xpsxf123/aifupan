package com.jiuyu.replay.activity.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.activity.entity.ClientInviteProgressEntity;
import com.jiuyu.replay.activity.entity.ClientInviteProgressRewardEntity;
import com.jiuyu.replay.activity.repository.service.ClientInviteProgressRewardService;
import com.jiuyu.replay.activity.repository.service.ClientInviteProgressService;
import com.jiuyu.replay.activity.rse.ClientInviteProgressRse;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressListVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
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
 * @date 2025-05-24 16:10:28
 */
@Service
public class ClientInviteProgressRseImpl implements ClientInviteProgressRse {

    @Resource
    private ClientInviteProgressService clientInviteProgressService;
    @Resource
    private ClientInviteProgressRewardService clientInviteProgressRewardService;


    @Override
    public PageUtils<ClientInviteProgressListVo> queryPage(ClientInviteProgressListBo clientInviteProgressListBo) {
        QueryWrapper<ClientInviteProgressEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(clientInviteProgressListBo.getKeyword())) {
            wrapper.like("name", clientInviteProgressListBo.getKeyword());
        }

        IPage<ClientInviteProgressEntity> iPage = clientInviteProgressService.page(new Query<ClientInviteProgressEntity>().getPage(clientInviteProgressListBo.getPage(), clientInviteProgressListBo.getLimit()), wrapper);

        PageUtils<ClientInviteProgressListVo> pageUtils = new PageUtils<>(clientInviteProgressListBo.getPage(), clientInviteProgressListBo.getLimit(), iPage);

        List<ClientInviteProgressEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
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
        if (clientInviteProgressEntity != null) {
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
    public List<ClientInviteProgressInfoVo> getInviteProgressAndReward(Long inviteActivityId, Integer inviteProgressType) {

        QueryWrapper<ClientInviteProgressEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("invite_progress_status", 1);
        if (inviteActivityId != null) {
            wrapper.eq("invite_activity_id", inviteActivityId);
        }
        if (inviteProgressType != null) {
            wrapper.eq("invite_progress_type", inviteProgressType);
        }
        wrapper.orderByDesc("invite_progress_type");
        wrapper.orderByAsc("invite_progress_value");
        List<ClientInviteProgressEntity> inviteProgressEntities = this.clientInviteProgressService.list(wrapper);

        if (inviteProgressEntities != null && inviteProgressEntities.size() > 0) {


            List<Long> progressIds = inviteProgressEntities.stream().map(ClientInviteProgressEntity::getId).collect(Collectors.toList());
            List<ClientInviteProgressRewardEntity> clientInviteProgressRewardEntities = this.clientInviteProgressRewardService.list(new QueryWrapper<ClientInviteProgressRewardEntity>().in("progress_id", progressIds));

            List<ClientInviteProgressInfoVo> clientInviteProgressInfoVos = inviteProgressEntities.stream().map(item -> {
                ClientInviteProgressInfoVo clientInviteProgressInfoVo = new ClientInviteProgressInfoVo();
                BeanUtils.copyProperties(item, clientInviteProgressInfoVo);
                // 封装进度的奖励列表
                List<ClientInviteProgressRewardInfoVo> rewardList = new LinkedList<>();
                if (clientInviteProgressRewardEntities != null && clientInviteProgressRewardEntities.size() > 0) {
                    for (ClientInviteProgressRewardEntity clientInviteProgressRewardEntity : clientInviteProgressRewardEntities) {
                        if (clientInviteProgressRewardEntity.getProgressId().equals(item.getId())) {
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

    @Override
    public List<ClientInviteProgressInfoVo> listByActivityIdAndStatus(Long activityId, Integer progressType) {

        QueryWrapper<ClientInviteProgressEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("invite_activity_id", activityId);
        if(progressType != null) {
            wrapper.eq("invite_progress_status", progressType);
        }
        List<ClientInviteProgressEntity> clientInviteProgressEntities = this.clientInviteProgressService.list(wrapper);
        if(clientInviteProgressEntities != null && clientInviteProgressEntities.size() > 0) {
            List<ClientInviteProgressInfoVo> progressInfoVos = clientInviteProgressEntities.stream().map(item -> {
                ClientInviteProgressInfoVo clientInviteProgressInfoVo = new ClientInviteProgressInfoVo();
                BeanUtils.copyProperties(item, clientInviteProgressInfoVo);
                return clientInviteProgressInfoVo;
            }).collect(Collectors.toList());

            return progressInfoVos;
        }

        return null;
    }


    @Override
    public List<ClientInviteProgressBo> listClientInviteProgressByActivityIdAndInviteProgressType(Long activityId, Integer inviteProgressType) {
        List<ClientInviteProgressEntity> inviteProgressEntityList = clientInviteProgressService.list(new LambdaQueryWrapper<>(ClientInviteProgressEntity.class).eq(ClientInviteProgressEntity::getInviteActivityId, activityId).eq(ClientInviteProgressEntity::getInviteProgressType, inviteProgressType).eq(ClientInviteProgressEntity::getInviteProgressStatus, 1));
        return BeanConvertUtils.convertList(inviteProgressEntityList, ClientInviteProgressBo.class);
    }
}

