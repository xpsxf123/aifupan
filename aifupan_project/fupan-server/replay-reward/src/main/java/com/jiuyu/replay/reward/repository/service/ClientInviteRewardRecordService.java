package com.jiuyu.replay.reward.repository.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordGroupEntity;

/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
public interface ClientInviteRewardRecordService extends IService<ClientInviteRewardRecordEntity> {

    /**
     * 分页分组获取用户奖励记录
     *
     * @param userId           用户id
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @param page             当前页
     * @param limit            每页大小
     * @return
     */
    IPage<ClientInviteRewardRecordGroupEntity> pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(Long userId, Integer rewardTargetType, Integer page, Integer limit);
}

