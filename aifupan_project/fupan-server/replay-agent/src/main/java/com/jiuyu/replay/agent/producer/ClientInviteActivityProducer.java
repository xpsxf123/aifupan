package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.agent.vo.ClientInviteActivityListVo;
import com.jiuyu.replay.agent.vo.ClientInviteActivityInfoVo;
import com.jiuyu.replay.agent.bo.ClientInviteActivityBo;
import com.jiuyu.replay.agent.bo.ClientInviteActivityListBo;


/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
public interface ClientInviteActivityProducer {


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


}

