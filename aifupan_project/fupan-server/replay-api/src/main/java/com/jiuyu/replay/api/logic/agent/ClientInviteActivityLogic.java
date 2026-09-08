package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

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
public interface ClientInviteActivityLogic {


    /**
     * 邀请活动列表
     * @param clientInviteActivityListBo 邀请活动列表查询参数
     * @return
     */
    R<PageUtils<ClientInviteActivityListVo>> queryPage(ClientInviteActivityListBo clientInviteActivityListBo);

    /**
    * 邀请活动信息
    * @param id 邀请活动id
    * @return
    */
    R<ClientInviteActivityInfoVo> info(Long id);

    /**
     * 新增邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    R<String> save(ClientInviteActivityBo clientInviteActivityBo);

    /**
     * 修改邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    R<String> update(ClientInviteActivityBo clientInviteActivityBo);

    /**
     * 删除邀请活动
     * @param id 邀请活动id
     * @return
     */
    R<String> delete(Long id);

}

