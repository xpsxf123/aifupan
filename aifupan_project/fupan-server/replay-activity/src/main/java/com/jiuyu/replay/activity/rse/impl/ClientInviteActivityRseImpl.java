package com.jiuyu.replay.activity.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.activity.entity.ClientInviteActivityEntity;
import com.jiuyu.replay.activity.entity.ClientInviteProgressEntity;
import com.jiuyu.replay.activity.entity.ClientInviteProgressRewardEntity;
import com.jiuyu.replay.activity.repository.service.ClientInviteActivityService;
import com.jiuyu.replay.activity.repository.service.ClientInviteProgressRewardService;
import com.jiuyu.replay.activity.repository.service.ClientInviteProgressService;
import com.jiuyu.replay.activity.rse.ClientInviteActivityRse;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityListBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Service
public class ClientInviteActivityRseImpl implements ClientInviteActivityRse {

    @Resource
    private ClientInviteActivityService clientInviteActivityService;
    @Resource
    private ClientInviteProgressService clientInviteProgressService;
    @Resource
    private ClientInviteProgressRewardService clientInviteProgressRewardService;


    @Override
    public PageUtils<ClientInviteActivityListVo> queryPage(ClientInviteActivityListBo clientInviteActivityListBo) {
        QueryWrapper<ClientInviteActivityEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(clientInviteActivityListBo.getKeyword())) {
            wrapper.like("name", clientInviteActivityListBo.getKeyword());
        }

        IPage<ClientInviteActivityEntity> iPage = clientInviteActivityService.page(new Query<ClientInviteActivityEntity>().getPage(clientInviteActivityListBo.getPage(), clientInviteActivityListBo.getLimit()), wrapper);

        PageUtils<ClientInviteActivityListVo> pageUtils = new PageUtils<>(clientInviteActivityListBo.getPage(), clientInviteActivityListBo.getLimit(), iPage);

        List<ClientInviteActivityEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
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
        if (activityStatus != null) {
            wrapper.eq("activity_status", activityStatus);
        }

        ClientInviteActivityEntity clientInviteActivityEntity = clientInviteActivityService.getOne(wrapper);
        if (clientInviteActivityEntity != null) {
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

    @Override
    public ClientInviteActivityInfoVo infoByActivate(Long activityId) {

        QueryWrapper<ClientInviteActivityEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("id", activityId);
        wrapper.eq("activity_status", 1);
        LocalDateTime localDateTime = LocalDateTime.now();
        String currDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(localDateTime);
        wrapper.lt("activity_start_time", currDate);
        wrapper.gt("activity_end_time", currDate);

        ClientInviteActivityEntity clientInviteActivityEntity = clientInviteActivityService.getOne(wrapper);
        if (clientInviteActivityEntity != null) {
            ClientInviteActivityInfoVo clientInviteActivityInfoVo = new ClientInviteActivityInfoVo();
            BeanUtils.copyProperties(clientInviteActivityEntity, clientInviteActivityInfoVo);
            return clientInviteActivityInfoVo;
        }

        return null;
    }

    @Override
    public void saveBatch(List<ClientInviteProgressBo> clientInviteProgressBoList) {
        if (clientInviteProgressBoList != null && clientInviteProgressBoList.size() > 0) {

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
                if (clientInviteProgressRewardBos != null && clientInviteProgressRewardBos.size() > 0) {
                    for (ClientInviteProgressRewardBo clientInviteProgressRewardBo : clientInviteProgressRewardBos) {
                        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = new ClientInviteProgressRewardEntity();
                        BeanUtils.copyProperties(clientInviteProgressRewardBo, clientInviteProgressRewardEntity);
                        clientInviteProgressRewardEntity.setId(SnowflakeManager.nextValue());
                        clientInviteProgressRewardEntity.setProgressId(clientInviteProgressEntity.getId());
                        clientInviteProgressRewardEntity.setCreateDate(new Date());
                        clientInviteProgressRewardEntity.setUpdateDate(new Date());
                        if (clientInviteProgressRewardEntity.getRewardType() == 0) {
                            clientInviteProgressRewardEntity.setCommodityTypeId(null);
                            clientInviteProgressRewardEntity.setCommodityNumber(null);
                        } else {
                            clientInviteProgressRewardEntity.setPackageId(null);
                            clientInviteProgressRewardEntity.setPackagePriceId(null);
                        }
                        clientInviteProgressRewardEntities.add(clientInviteProgressRewardEntity);
                    }
                }
            }

            if (clientInviteProgressEntities.size() > 0) {
                this.clientInviteProgressService.saveBatch(clientInviteProgressEntities);
            }
            if (clientInviteProgressRewardEntities.size() > 0) {
                this.clientInviteProgressRewardService.saveBatch(clientInviteProgressRewardEntities);
            }

        }
    }

    @Override
    public void disableOld(Long inviteActivityId) {
        List<ClientInviteProgressEntity> list =  clientInviteProgressService.list(new LambdaQueryWrapper<>(ClientInviteProgressEntity.class).eq(ClientInviteProgressEntity::getInviteActivityId, inviteActivityId).eq(ClientInviteProgressEntity::getInviteProgressStatus,1));

        if (list != null && list.size() > 0) {
            for (ClientInviteProgressEntity clientInviteProgressEntity : list) {
                clientInviteProgressEntity.setInviteProgressStatus(0);
                clientInviteProgressEntity.setUpdateDate(new Date());
            }
            this.clientInviteProgressService.updateBatchById(list);
        }
    }


}

