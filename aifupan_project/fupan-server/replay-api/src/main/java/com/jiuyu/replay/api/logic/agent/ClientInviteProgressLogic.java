package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.ClientInviteProgressListVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressListBo;

import java.util.List;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
public interface ClientInviteProgressLogic {


    /**
     * 邀请进度列表
     * @param clientInviteProgressListBo 邀请进度列表查询参数
     * @return
     */
    R<PageUtils<ClientInviteProgressListVo>> queryPage(ClientInviteProgressListBo clientInviteProgressListBo);

    /**
    * 邀请进度信息
    * @param id 邀请进度id
    * @return
    */
    R<ClientInviteProgressInfoVo> info(Long id);

    /**
     * 新增邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    R<String> save(ClientInviteProgressBo clientInviteProgressBo);

    /**
     * 修改邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    R<String> update(ClientInviteProgressBo clientInviteProgressBo);

    /**
     * 删除邀请进度
     * @param id 邀请进度id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 设置邀请进度和进度奖励
     * @param clientInviteProgressBoList 进度集合
     * @return
     */
    R<String> setInviteProgressAndReward(List<ClientInviteProgressBo> clientInviteProgressBoList);

    /**
     * 获取邀请进度和进度奖励
     * @return
     */
    R<List<ClientInviteProgressInfoVo>> getInviteProgressAndReward();

    /**
     * 客户端获取邀请进度和进度奖励
     * @return
     */
    R<List<ClientInviteProgressInfoVo>> clientGetInviteProgressAndReward();

}

