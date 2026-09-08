package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordListVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordListBo;


/**
 * 邀请奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
public interface ClientInviteRewardRecordLogic {


    /**
     * 邀请奖励记录列表
     * @param clientInviteRewardRecordListBo 邀请奖励记录列表查询参数
     * @return
     */
    R<PageUtils<ClientInviteRewardRecordListVo>> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo);

    /**
    * 邀请奖励记录信息
    * @param id 邀请奖励记录id
    * @return
    */
    R<ClientInviteRewardRecordInfoVo> info(Long id);

    /**
     * 新增邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    R<String> save(ClientInviteRewardRecordBo clientInviteRewardRecordBo);

    /**
     * 修改邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    R<String> update(ClientInviteRewardRecordBo clientInviteRewardRecordBo);

    /**
     * 删除邀请奖励记录
     * @param id 邀请奖励记录id
     * @return
     */
    R<String> delete(Long id);


}

