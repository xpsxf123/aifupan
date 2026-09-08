package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.ClientInviteActivityBll;
import com.jiuyu.replay.agent.bll.ClientInviteProgressBll;
import com.jiuyu.replay.agent.bo.ClientInviteActivityBo;
import com.jiuyu.replay.agent.bo.ClientInviteActivityListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.vo.ClientInviteActivityInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteActivityListVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteActivityLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.CommodityTypeBll;
import com.jiuyu.replay.order.bll.PackageBll;
import jakarta.annotation.Resource;

import java.util.List;


/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-23 16:45:49
 */
//@Service
public class ClientInviteActivityLogicImpl implements ClientInviteActivityLogic {

    @Resource
    private ClientInviteActivityBll clientInviteActivityBll;
    @Resource
    private ClientInviteProgressBll clientInviteProgressBll;
    @Resource
    private CommodityTypeBll commodityTypeBll;
    @Resource
    private PackageBll packageBll;
    @Resource
    private AgentProperties agentProperties;


    @Override
    public R<PageUtils<ClientInviteActivityListVo>> queryPage(ClientInviteActivityListBo clientInviteActivityListBo) {

        return clientInviteActivityBll.queryPage(clientInviteActivityListBo);
    }

    @Override
    public R<ClientInviteActivityInfoVo> info(Long id) {

        R<ClientInviteActivityInfoVo> clientInviteActivityInfoVoR = clientInviteActivityBll.info(id, null);
        if(clientInviteActivityInfoVoR.getData() != null) {
            ClientInviteActivityInfoVo inviteActivityInfoVo = clientInviteActivityInfoVoR.getData();
            R<List<ClientInviteProgressInfoVo>> clientInviteProgressInfoVosR = clientInviteProgressBll.getInviteProgressAndReward(inviteActivityInfoVo.getId(), null);
            List<ClientInviteProgressInfoVo> progressInfoVos = clientInviteProgressInfoVosR.getData();
            inviteActivityInfoVo.setProgressList(progressInfoVos);
        }

        return clientInviteActivityInfoVoR;
    }

    @Override
    public R<String> save(ClientInviteActivityBo clientInviteActivityBo) {

        return clientInviteActivityBll.save(clientInviteActivityBo);
    }

    @Override
    public R<String> update(ClientInviteActivityBo clientInviteActivityBo) {

        return clientInviteActivityBll.update(clientInviteActivityBo);
    }

    @Override
    public R<String> delete(Long id) {

        return clientInviteActivityBll.delete(id);
    }


}

