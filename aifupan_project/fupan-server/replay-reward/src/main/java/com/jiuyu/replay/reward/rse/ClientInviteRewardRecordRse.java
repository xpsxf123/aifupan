package com.jiuyu.replay.reward.rse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.bo.reward.ClientInviteRewardRecordBo;
import com.jiuyu.replay.generic.bo.reward.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.reward.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.generic.vo.reward.ClientInviteRewardRecordListVo;
import com.jiuyu.replay.reward.bo.ClientInviteRewardRecordInfoBo;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordGroupEntity;

import java.util.List;


/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
public interface ClientInviteRewardRecordRse {


    /**
     * 进度奖励记录列表
     *
     * @param clientInviteRewardRecordListBo 进度奖励记录列表查询参数
     * @return
     */
    PageUtils<ClientInviteRewardRecordListVo> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo);

    /**
     * 进度奖励记录信息
     *
     * @param id 进度奖励记录id
     * @return
     */
    ClientInviteRewardRecordInfoVo info(Long id);

    /**
     * 新增进度奖励记录
     *
     * @param clientInviteRewardRecordBo 进度奖励记录对象
     * @return
     */
    ClientInviteRewardRecordInfoVo save(ClientInviteRewardRecordBo clientInviteRewardRecordBo);

    /**
     * 修改进度奖励记录
     *
     * @param clientInviteRewardRecordBo 进度奖励记录对象
     * @return
     */
    void update(ClientInviteRewardRecordBo clientInviteRewardRecordBo);

    /**
     * 删除进度奖励记录
     *
     * @param id 进度奖励记录id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取用户的奖励列表
     *
     * @param userId           用户id
     * @param tenantId         租户id
     * @param rewardStatus     奖励的状态 0：待发放 1：已发放
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @return
     */
    List<ClientInviteRewardRecordInfoVo> listByUserIdAndTenantId(Long userId, Long tenantId, Integer rewardStatus, Integer rewardTargetType);

    /**
     * 获取用户奖励列表根据一些列条件
     *
     * @param rewardUserId     获得奖励用户id
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @param rewardStatus     奖励的状态 0：待发放 1：已发放
     * @return
     */
    List<ClientInviteRewardRecordInfoBo> listByRewardUserIdAndRewardTargetTypeAndRewardStatus(Long rewardUserId, Integer rewardTargetType, Integer rewardStatus);

    /**
     * 获取某种奖励对象类型 用户的奖励记录
     *
     * @param activityId       活动id
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @param rewardUserId     奖励用户id
     * @param sourceUserId     奖励来源用户id
     * @return
     */
    List<ClientInviteRewardRecordInfoBo> listByActivityIdAndRewardTargetTypeAndRewardUserIdAndRewardSourceUserId(Long activityId, Integer rewardTargetType, Long rewardUserId, Long sourceUserId);

    /**
     * 批量保存用户奖励记录
     *
     * @param finalRewardRecordList
     * @return
     */
    boolean batchSave(List<ClientInviteRewardRecordInfoBo> finalRewardRecordList);

    /**
     * 修改发放奖励状态
     *
     * @param ids 主键id集合
     * @return
     */
    boolean updateRewardStatusByIds(List<Long> ids);

    /**
     * 后台获取奖励列表
     *
     * @param clientInviteRewardRecordListBo 查询参数
     * @return
     */
    PageUtils<ClientInviteRewardRecordInfoVo> listByBack(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo);


    /**
     * 分页获取用户奖励记录
     *
     * @param userId           用户id
     * @param rewardTargetType 奖励对象类型 0：邀请人 1：被邀请人
     * @param page             当前页
     * @param limit            每页大小
     * @return
     */
    IPage<ClientInviteRewardRecordEntity> pageUserRewardListByRewardUserIdAndRewardTargetType(Long userId, Integer rewardTargetType, Integer page, Integer limit);

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

