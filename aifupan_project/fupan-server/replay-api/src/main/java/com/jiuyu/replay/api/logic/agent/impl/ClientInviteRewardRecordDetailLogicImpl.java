package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.ClientInviteRewardRecordDetailBll;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailListBo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteRewardRecordDetailLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;


/**
 * 邀请奖励明细记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteRewardRecordDetailLogicImpl implements ClientInviteRewardRecordDetailLogic {

    @Resource
    private ClientInviteRewardRecordDetailBll clientInviteRewardRecordDetailBll;


    @Override
    public R<PageUtils<ClientInviteRewardRecordDetailListVo>> queryPage(ClientInviteRewardRecordDetailListBo clientInviteRewardRecordDetailListBo) {

        return clientInviteRewardRecordDetailBll.queryPage(clientInviteRewardRecordDetailListBo);
    }

    @Override
    public R<ClientInviteRewardRecordDetailInfoVo> info(Long id) {

        return clientInviteRewardRecordDetailBll.info(id);
    }

    @Override
    public R<String> save(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo) {

        return clientInviteRewardRecordDetailBll.save(clientInviteRewardRecordDetailBo);
    }

    @Override
    public R<String> update(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo) {

        return clientInviteRewardRecordDetailBll.update(clientInviteRewardRecordDetailBo);
    }

    @Override
    public R<String> delete(Long id) {

        return clientInviteRewardRecordDetailBll.delete(id);
    }


}

