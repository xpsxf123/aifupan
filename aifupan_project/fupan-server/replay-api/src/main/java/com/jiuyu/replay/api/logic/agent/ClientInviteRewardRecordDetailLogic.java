package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailListVo;
import com.jiuyu.replay.agent.vo.ClientInviteRewardRecordDetailInfoVo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailBo;
import com.jiuyu.replay.agent.bo.ClientInviteRewardRecordDetailListBo;


/**
 * 邀请奖励明细记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
public interface ClientInviteRewardRecordDetailLogic {


    /**
     * 邀请奖励明细记录列表
     * @param clientInviteRewardRecordDetailListBo 邀请奖励明细记录列表查询参数
     * @return
     */
    R<PageUtils<ClientInviteRewardRecordDetailListVo>> queryPage(ClientInviteRewardRecordDetailListBo clientInviteRewardRecordDetailListBo);

    /**
    * 邀请奖励明细记录信息
    * @param id 邀请奖励明细记录id
    * @return
    */
    R<ClientInviteRewardRecordDetailInfoVo> info(Long id);

    /**
     * 新增邀请奖励明细记录
     * @param clientInviteRewardRecordDetailBo 邀请奖励明细记录对象
     * @return
     */
    R<String> save(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo);

    /**
     * 修改邀请奖励明细记录
     * @param clientInviteRewardRecordDetailBo 邀请奖励明细记录对象
     * @return
     */
    R<String> update(ClientInviteRewardRecordDetailBo clientInviteRewardRecordDetailBo);

    /**
     * 删除邀请奖励明细记录
     * @param id 邀请奖励明细记录id
     * @return
     */
    R<String> delete(Long id);


}

