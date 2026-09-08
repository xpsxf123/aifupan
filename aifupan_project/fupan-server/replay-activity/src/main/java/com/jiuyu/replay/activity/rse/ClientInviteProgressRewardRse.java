package com.jiuyu.replay.activity.rse;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardListVo;

import java.util.Collection;
import java.util.List;

/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
public interface ClientInviteProgressRewardRse {


    /**
     * 邀请进度奖励列表
     * @param clientInviteProgressRewardListBo 邀请进度奖励列表查询参数
     * @return
     */
    PageUtils<ClientInviteProgressRewardListVo> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo);

    /**
    * 邀请进度奖励信息
    * @param id 邀请进度奖励id
    * @return
    */
    ClientInviteProgressRewardInfoVo info(Long id);

    /**
     * 新增邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
     ClientInviteProgressRewardInfoVo save(ClientInviteProgressRewardBo clientInviteProgressRewardBo);

    /**
     * 修改邀请进度奖励
     * @param clientInviteProgressRewardBo 邀请进度奖励对象
     * @return
     */
    void update(ClientInviteProgressRewardBo clientInviteProgressRewardBo);

    /**
     * 删除邀请进度奖励
     * @param id 邀请进度奖励id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据进度奖励id集合获取进度奖励列表
     * @param progressRewardIds 进度奖励id集合
     * @return
     */
    List<ClientInviteProgressRewardInfoVo> listByProgressRewardIds(Collection<Long> progressRewardIds);

    /**
     * 根据进度id集合获取奖励列表
     * @param progressIds 进度id集合
     * @return
     */
    List<ClientInviteProgressRewardInfoVo> listByProgressIdsAndStatus(List<Long> progressIds);

    /**
     * 获取奖励规则根据进度ids
     * @param progressIds 进度ids
     * @return
     */
    List<ClientInviteProgressRewardBo> listByProgressIds(List<Long> progressIds);
}

