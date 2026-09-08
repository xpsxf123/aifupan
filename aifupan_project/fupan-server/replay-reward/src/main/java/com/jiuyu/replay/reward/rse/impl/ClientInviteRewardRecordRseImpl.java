package com.jiuyu.replay.reward.rse.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.reward.ClientInviteRewardRecordBo;
import com.jiuyu.replay.generic.bo.reward.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.reward.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.generic.vo.reward.ClientInviteRewardRecordListVo;
import com.jiuyu.replay.reward.bo.ClientInviteRewardRecordInfoBo;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordGroupEntity;
import com.jiuyu.replay.reward.repository.service.ClientInviteRewardRecordService;
import com.jiuyu.replay.reward.rse.ClientInviteRewardRecordRse;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@Service
public class ClientInviteRewardRecordRseImpl implements ClientInviteRewardRecordRse {

    @Resource
    private ClientInviteRewardRecordService clientInviteRewardRecordService;


    @Override
    public PageUtils<ClientInviteRewardRecordListVo> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo) {
        QueryWrapper<ClientInviteRewardRecordEntity> wrapper = new QueryWrapper<>();

        IPage<ClientInviteRewardRecordEntity> iPage = clientInviteRewardRecordService.page(new Query<ClientInviteRewardRecordEntity>().getPage(clientInviteRewardRecordListBo.getPage(), clientInviteRewardRecordListBo.getLimit()), wrapper);

        PageUtils<ClientInviteRewardRecordListVo> pageUtils = new PageUtils<>(clientInviteRewardRecordListBo.getPage(), clientInviteRewardRecordListBo.getLimit(), iPage);

        List<ClientInviteRewardRecordEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
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
        if (clientInviteRewardRecordEntity != null) {
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

    @Override
    public List<ClientInviteRewardRecordInfoVo> listByUserIdAndTenantId(Long userId, Long tenantId, Integer rewardStatus, Integer rewardTargetType) {

        QueryWrapper<ClientInviteRewardRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("reward_user_id", userId);
        wrapper.eq("reward_tenant_id", tenantId);
        if (rewardStatus != null) {
            wrapper.eq("reward_status", rewardStatus);
        }
        if (rewardTargetType != null) {
            wrapper.eq("reward_target_type", rewardTargetType);
        }
        List<ClientInviteRewardRecordEntity> clientInviteRewardRecordEntities = this.clientInviteRewardRecordService.list(wrapper);
        if (clientInviteRewardRecordEntities != null && clientInviteRewardRecordEntities.size() > 0) {
            List<ClientInviteRewardRecordInfoVo> recordInfoVoList = clientInviteRewardRecordEntities.stream().map(item -> {
                ClientInviteRewardRecordInfoVo clientInviteRewardRecordInfoVo = new ClientInviteRewardRecordInfoVo();
                BeanUtils.copyProperties(item, clientInviteRewardRecordInfoVo);
                return clientInviteRewardRecordInfoVo;
            }).collect(Collectors.toList());

            return recordInfoVoList;
        }

        return null;
    }

    /**
     * 获取用户奖励列表根据一些列条件
     *
     * @param rewardUserId     获得奖励用户id
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @param rewardStatus     奖励的状态 0：待发放 1：已发放
     * @return
     */
    @Override
    public List<ClientInviteRewardRecordInfoBo> listByRewardUserIdAndRewardTargetTypeAndRewardStatus(Long rewardUserId, Integer rewardTargetType, Integer rewardStatus) {
        List<ClientInviteRewardRecordEntity> clientInviteRewardRecordEntityList = clientInviteRewardRecordService.list(new LambdaQueryWrapper<ClientInviteRewardRecordEntity>().eq(ClientInviteRewardRecordEntity::getRewardUserId, rewardUserId).eq(ClientInviteRewardRecordEntity::getRewardTargetType, rewardTargetType).eq(ClientInviteRewardRecordEntity::getRewardStatus, rewardStatus));
        return BeanConvertUtils.convertList(clientInviteRewardRecordEntityList, ClientInviteRewardRecordInfoBo.class);
    }

    @Override
    public List<ClientInviteRewardRecordInfoBo> listByActivityIdAndRewardTargetTypeAndRewardUserIdAndRewardSourceUserId(Long activityId, Integer rewardTargetType, Long rewardUserId, Long sourceUserId) {
        List<ClientInviteRewardRecordEntity> clientInviteRewardRecordEntityList = clientInviteRewardRecordService.list(new LambdaQueryWrapper<>(ClientInviteRewardRecordEntity.class).eq(ClientInviteRewardRecordEntity::getActivityId, activityId).eq(ClientInviteRewardRecordEntity::getRewardTargetType, rewardTargetType).eq(ClientInviteRewardRecordEntity::getRewardUserId, rewardUserId).eq(ClientInviteRewardRecordEntity::getRewardSourceUserId, sourceUserId));
        return BeanConvertUtils.convertList(clientInviteRewardRecordEntityList, ClientInviteRewardRecordInfoBo.class);
    }

    @Override
    public boolean batchSave(List<ClientInviteRewardRecordInfoBo> finalRewardRecordList) {
        return clientInviteRewardRecordService.saveBatch(BeanConvertUtils.convertList(finalRewardRecordList, ClientInviteRewardRecordEntity.class));
    }

    @Override
    public boolean updateRewardStatusByIds(List<Long> ids) {
        List<ClientInviteRewardRecordEntity> updateList = new ArrayList<>();
        ids.forEach(item -> {
            ClientInviteRewardRecordEntity clientInviteRewardRecordEntity = new ClientInviteRewardRecordEntity();
            clientInviteRewardRecordEntity.setId(item);
            clientInviteRewardRecordEntity.setRewardStatus(1L);
            clientInviteRewardRecordEntity.setSendDate(new Date());
            clientInviteRewardRecordEntity.setUpdateDate(new Date());
            updateList.add(clientInviteRewardRecordEntity);
        });
        return clientInviteRewardRecordService.updateBatchById(updateList);
    }

    @Override
    public PageUtils<ClientInviteRewardRecordInfoVo> listByBack(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo) {

        QueryWrapper<ClientInviteRewardRecordEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(clientInviteRewardRecordListBo.getProgressCode())) {
            wrapper.eq("progress_code", clientInviteRewardRecordListBo.getProgressCode());
        }
        if (!StringUtils.isEmpty(clientInviteRewardRecordListBo.getRewardStatus())) {
            wrapper.eq("reward_status", clientInviteRewardRecordListBo.getRewardStatus());
        }
        if (CollectionUtil.isNotEmpty(clientInviteRewardRecordListBo.getInviteeUserIds())) {
            wrapper.in("reward_source_user_id", clientInviteRewardRecordListBo.getInviteeUserIds());
        }
        if (CollectionUtil.isNotEmpty(clientInviteRewardRecordListBo.getInviterUserIds())) {
            wrapper.in("reward_user_id", clientInviteRewardRecordListBo.getInviterUserIds());
        }
        if (!StringUtils.isEmpty(clientInviteRewardRecordListBo.getStartTime())) {
            wrapper.ge("send_date", clientInviteRewardRecordListBo.getStartTime() + " 00:00:00");
        }
        if (!StringUtils.isEmpty(clientInviteRewardRecordListBo.getEndTime())) {
            wrapper.le("send_date", clientInviteRewardRecordListBo.getStartTime() + " 23:59:59");
        }
        wrapper.eq("reward_target_type", 0);
        wrapper.eq("reward_status", 1);
        wrapper.orderByDesc("send_date");
        wrapper.groupBy("reward_user_id", "progress_id", "reward_source_user_id");

        // 查出分组后的分页数据
        IPage<ClientInviteRewardRecordEntity> iPage = this.clientInviteRewardRecordService.page(new Query<ClientInviteRewardRecordEntity>().getPageNoSort(clientInviteRewardRecordListBo.getPage(), clientInviteRewardRecordListBo.getLimit()), wrapper);
        List<ClientInviteRewardRecordEntity> records = iPage.getRecords();

        PageUtils<ClientInviteRewardRecordInfoVo> pageUtils = new PageUtils<>(clientInviteRewardRecordListBo.getPage(), clientInviteRewardRecordListBo.getLimit(), iPage);

        if (records != null && records.size() > 0) {
            List<Long> rewardUserIds = records.stream().map(ClientInviteRewardRecordEntity::getRewardUserId).collect(Collectors.toList());
            List<Long> progressIds = records.stream().map(ClientInviteRewardRecordEntity::getProgressId).collect(Collectors.toList());
            List<Long> rewardSourceUserIds = records.stream().map(ClientInviteRewardRecordEntity::getRewardSourceUserId).collect(Collectors.toList());
            QueryWrapper<ClientInviteRewardRecordEntity> recordWrapper = new QueryWrapper<>();
            recordWrapper.in("reward_user_id", rewardUserIds);
            recordWrapper.in("progress_id", progressIds);
            recordWrapper.in("reward_source_user_id", rewardSourceUserIds);
            recordWrapper.eq("reward_status", 1);
            List<ClientInviteRewardRecordEntity> rewardRecordEntities = this.clientInviteRewardRecordService.list(recordWrapper);

            List<ClientInviteRewardRecordInfoVo> vos = records.stream().map(item -> {
                ClientInviteRewardRecordInfoVo clientInviteRewardRecordInfoVo = new ClientInviteRewardRecordInfoVo();
                BeanUtils.copyProperties(item, clientInviteRewardRecordInfoVo);
                // 配置所有奖励
                List<Long> progressRewardIds = new LinkedList<>();
                if (rewardRecordEntities != null) {
                    for (ClientInviteRewardRecordEntity rewardRecordEntity : rewardRecordEntities) {
                        if (rewardRecordEntity.getRewardUserId().equals(item.getRewardUserId())
                                && rewardRecordEntity.getProgressId().equals(item.getProgressId())
                                && rewardRecordEntity.getRewardSourceUserId().equals(item.getRewardSourceUserId())) {
                            progressRewardIds.add(rewardRecordEntity.getProgressRewardId());
                        }
                    }
                }
                clientInviteRewardRecordInfoVo.setProgressRewardIds(progressRewardIds);

                return clientInviteRewardRecordInfoVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public IPage<ClientInviteRewardRecordEntity> pageUserRewardListByRewardUserIdAndRewardTargetType(Long userId, Integer rewardTargetType, Integer page, Integer limit) {
        return clientInviteRewardRecordService.page(new Page<>(page, limit), new LambdaQueryWrapper<>(ClientInviteRewardRecordEntity.class).eq(ClientInviteRewardRecordEntity::getRewardUserId, userId).eq(ClientInviteRewardRecordEntity::getRewardTargetType, rewardTargetType).eq(ClientInviteRewardRecordEntity::getRewardStatus, 1).orderByDesc(ClientInviteRewardRecordEntity::getSendDate));
    }

    @Override
    public IPage<ClientInviteRewardRecordGroupEntity> pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(Long userId, Integer rewardTargetType, Integer page, Integer limit) {
        return clientInviteRewardRecordService.pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(userId, rewardTargetType, page, limit);
    }
}

