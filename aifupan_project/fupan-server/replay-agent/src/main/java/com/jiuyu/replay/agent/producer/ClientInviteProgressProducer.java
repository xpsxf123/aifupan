package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.agent.vo.ClientInviteProgressListVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressListBo;

import java.util.List;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
public interface ClientInviteProgressProducer {


    /**
     * 邀请进度列表
     * @param clientInviteProgressListBo 邀请进度列表查询参数
     * @return
     */
    PageUtils<ClientInviteProgressListVo> queryPage(ClientInviteProgressListBo clientInviteProgressListBo);

    /**
    * 邀请进度信息
    * @param id 邀请进度id
    * @return
    */
    ClientInviteProgressInfoVo info(Long id);

    /**
     * 新增邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
     ClientInviteProgressInfoVo save(ClientInviteProgressBo clientInviteProgressBo);

    /**
     * 修改邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    void update(ClientInviteProgressBo clientInviteProgressBo);

    /**
     * 删除邀请进度
     * @param id 邀请进度id
     * @return
     */
    void deleteById(Long id);


    /**
     * 将旧的所有奖励状态改为停用
     * @param inviteActivityId 邀请活动id
     */
    void disableOld(Long inviteActivityId);

    /**
     * 批量保存进度和进度奖励
     * @param clientInviteProgressBoList 进度和奖励信息集合
     */
    void saveBatch(List<ClientInviteProgressBo> clientInviteProgressBoList);

    /**
     * 获取启用中的进度列表
     * @param inviteActivityId 活动id
     * @param inviteProgressType 进度类型 0：被邀请人进度 1：邀请人进度
     * @return
     */
    List<ClientInviteProgressInfoVo> listAllActivity(Long inviteActivityId, Integer inviteProgressType);
}

