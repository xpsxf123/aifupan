package com.jiuyu.replay.activity.rse;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressListBo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressListVo;

import java.util.List;

/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
public interface ClientInviteProgressRse {


    /**
     * 邀请进度列表
     *
     * @param clientInviteProgressListBo 邀请进度列表查询参数
     * @return
     */
    PageUtils<ClientInviteProgressListVo> queryPage(ClientInviteProgressListBo clientInviteProgressListBo);

    /**
     * 邀请进度信息
     *
     * @param id 邀请进度id
     * @return
     */
    ClientInviteProgressInfoVo info(Long id);

    /**
     * 新增邀请进度
     *
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    ClientInviteProgressInfoVo save(ClientInviteProgressBo clientInviteProgressBo);

    /**
     * 修改邀请进度
     *
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    void update(ClientInviteProgressBo clientInviteProgressBo);

    /**
     * 删除邀请进度
     *
     * @param id 邀请进度id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取邀请进度和进度奖励
     *
     * @param inviteActivityId   活动id
     * @param inviteProgressType 进度类型 0：被邀请人进度 1：邀请人进度
     * @return
     */
    List<ClientInviteProgressInfoVo> getInviteProgressAndReward(Long inviteActivityId, Integer inviteProgressType);

    /**
     * 获取活动进度根据活动id和进度类型
     *
     * @param activityId         活动id
     * @param inviteProgressType 进度类型 0：被邀请人进度 1：邀请人进度
     * @return
     */
    List<ClientInviteProgressBo> listClientInviteProgressByActivityIdAndInviteProgressType(Long activityId,Integer inviteProgressType);

    /**
     * 根据活动id和进度状态获取进度列表
     * @param activityId 活动id
     * @param progressType 进度启用状态 0：停用 1：启用中
     * @return
     */
    List<ClientInviteProgressInfoVo> listByActivityIdAndStatus(Long activityId, Integer progressType);
}

