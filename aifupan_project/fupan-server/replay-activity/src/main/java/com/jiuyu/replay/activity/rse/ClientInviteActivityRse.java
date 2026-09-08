package com.jiuyu.replay.activity.rse;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityListBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityListVo;

import java.util.List;

/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
public interface ClientInviteActivityRse {


    /**
     * 邀请活动列表
     * @param clientInviteActivityListBo 邀请活动列表查询参数
     * @return
     */
    PageUtils<ClientInviteActivityListVo> queryPage(ClientInviteActivityListBo clientInviteActivityListBo);

    /**
     * 邀请活动信息
     * @param id 邀请活动id
     * @param activityStatus 活动状态 0：未启用 1：启用中
     * @return
     */
    ClientInviteActivityInfoVo info(Long id, Integer activityStatus);

    /**
     * 新增邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
     ClientInviteActivityInfoVo save(ClientInviteActivityBo clientInviteActivityBo);

    /**
     * 修改邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    void update(ClientInviteActivityBo clientInviteActivityBo);

    /**
     * 删除邀请活动
     * @param id 邀请活动id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据活动id获取启用中的活动
     * @param activityId 活动id
     * @return
     */
    ClientInviteActivityInfoVo infoByActivate(Long activityId);

    /**
     * 批量保存进度和进度奖励
     * @param clientInviteProgressBoList 进度和奖励信息集合
     */
    void saveBatch(List<ClientInviteProgressBo> clientInviteProgressBoList);

    /**
     * 将旧的所有奖励状态改为停用
     * @param inviteActivityId 邀请活动id
     */
    void disableOld(Long inviteActivityId);
}

