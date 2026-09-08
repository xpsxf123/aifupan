package com.jiuyu.replay.reward.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@Mapper
public interface ClientInviteRewardRecordDao extends BaseMapper<ClientInviteRewardRecordEntity> {

    /**
     * 分页分组获取用户奖励记录
     *
     * @param userId           用户id
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @return
     */
    IPage<ClientInviteRewardRecordGroupEntity> pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(Page<ClientInviteRewardRecordGroupEntity> page, @Param("userId") Long userId, @Param("rewardTargetType") Integer rewardTargetType);
}
