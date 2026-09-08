package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.ClientInviteProgressRewardBll;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressRewardListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteProgressRewardLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;


/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteProgressRewardLogicImpl implements ClientInviteProgressRewardLogic {

    @Resource
    private ClientInviteProgressRewardBll clientInviteProgressRewardBll;


    @Override
    public R<PageUtils<ClientInviteProgressRewardListVo>> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo) {

        return clientInviteProgressRewardBll.queryPage(clientInviteProgressRewardListBo);
    }

    @Override
    public R<ClientInviteProgressRewardInfoVo> info(Long id) {

        return clientInviteProgressRewardBll.info(id);
    }

    @Override
    public R<String> save(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        return clientInviteProgressRewardBll.save(clientInviteProgressRewardBo);
    }

    @Override
    public R<String> update(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        return clientInviteProgressRewardBll.update(clientInviteProgressRewardBo);
    }

    @Override
    public R<String> delete(Long id) {

        return clientInviteProgressRewardBll.delete(id);
    }


}

