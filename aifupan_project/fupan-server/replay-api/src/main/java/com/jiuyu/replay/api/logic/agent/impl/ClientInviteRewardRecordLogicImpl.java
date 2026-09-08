package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.ClientInviteRewardRecordBll;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteRewardRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;


/**
 * 邀请奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteRewardRecordLogicImpl implements ClientInviteRewardRecordLogic {

    @Resource
    private ClientInviteRewardRecordBll clientInviteRewardRecordBll;


    @Override
    public R<PageUtils<ClientInviteRewardRecordListVo>> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo) {

        return clientInviteRewardRecordBll.queryPage(clientInviteRewardRecordListBo);
    }

    @Override
    public R<ClientInviteRewardRecordInfoVo> info(Long id) {

        return clientInviteRewardRecordBll.info(id);
    }

    @Override
    public R<String> save(ClientInviteRewardRecordBo clientInviteRewardRecordBo) {

        return clientInviteRewardRecordBll.save(clientInviteRewardRecordBo);
    }

    @Override
    public R<String> update(ClientInviteRewardRecordBo clientInviteRewardRecordBo) {

        return clientInviteRewardRecordBll.update(clientInviteRewardRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return clientInviteRewardRecordBll.delete(id);
    }


}

