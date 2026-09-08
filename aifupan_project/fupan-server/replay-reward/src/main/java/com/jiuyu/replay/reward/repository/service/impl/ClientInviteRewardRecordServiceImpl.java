package com.jiuyu.replay.reward.repository.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordGroupEntity;
import com.jiuyu.replay.reward.repository.dao.ClientInviteRewardRecordDao;
import com.jiuyu.replay.reward.repository.service.ClientInviteRewardRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


@Service("clientInviteRewardRecordService")
public class ClientInviteRewardRecordServiceImpl extends ServiceImpl<ClientInviteRewardRecordDao, ClientInviteRewardRecordEntity> implements ClientInviteRewardRecordService {

    @Resource
    private ClientInviteRewardRecordDao clientInviteRewardRecordDao;

    @Override
    public IPage<ClientInviteRewardRecordGroupEntity> pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(Long userId, Integer rewardTargetType, Integer page, Integer limit) {
        return clientInviteRewardRecordDao.pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(new Page<>(page, limit), userId, rewardTargetType);
    }
}