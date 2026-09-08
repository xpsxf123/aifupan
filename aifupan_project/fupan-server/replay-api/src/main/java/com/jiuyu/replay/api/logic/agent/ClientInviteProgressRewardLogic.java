package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardListVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardListBo;


/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
public interface ClientInviteProgressRewardLogic {


    /**
     * 邀请进度奖励列表
     * @param clientInviteProgressRewardListBo 邀请进度奖励列表查询参数
     * @return
     */
    R<PageUtils<ClientInviteProgressRewardListVo>> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo);

    /**
    * 邀请进度奖励信息
    * @param id 邀请进度奖励id
    * @return
    */
    R<ClientInviteProgressRewardInfoVo> info(Long id);

    /**
     * 新增邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    R<String> save(ClientInviteProgressRewardBo clientInviteProgressRewardBo);

    /**
     * 修改邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    R<String> update(ClientInviteProgressRewardBo clientInviteProgressRewardBo);

    /**
     * 删除邀请进度奖励
     * @param id 邀请进度奖励id
     * @return
     */
    R<String> delete(Long id);


}

