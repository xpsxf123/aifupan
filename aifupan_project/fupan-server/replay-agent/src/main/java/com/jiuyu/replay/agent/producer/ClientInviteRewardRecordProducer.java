package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

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
public interface ClientInviteRewardRecordProducer {


    /**
     * 邀请奖励记录列表
     * @param clientInviteRewardRecordListBo 邀请奖励记录列表查询参数
     * @return
     */
    PageUtils<ClientInviteRewardRecordListVo> queryPage(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo);

    /**
    * 邀请奖励记录信息
    * @param id 邀请奖励记录id
    * @return
    */
    ClientInviteRewardRecordInfoVo info(Long id);

    /**
     * 新增邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
     ClientInviteRewardRecordInfoVo save(ClientInviteRewardRecordBo clientInviteRewardRecordBo);

    /**
     * 修改邀请奖励记录
     * @param clientInviteRewardRecordBo 邀请奖励记录对象
     * @return
     */
    void update(ClientInviteRewardRecordBo clientInviteRewardRecordBo);

    /**
     * 删除邀请奖励记录
     * @param id 邀请奖励记录id
     * @return
     */
    void deleteById(Long id);


}

