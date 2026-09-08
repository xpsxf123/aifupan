package com.jiuyu.replay.api.logic.agent;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.AgentListVo;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.bo.AgentBo;
import com.jiuyu.replay.agent.bo.AgentListBo;


/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface AgentLogic {


    /**
     * 代理商列表
     * @param agentListBo 代理商列表查询参数
     * @return
     */
    R<PageUtils<AgentListVo>> queryPage(AgentListBo agentListBo);

    /**
    * 代理商信息
    * @param id 代理商id
    * @return
    */
    R<AgentInfoVo> info(Long id);

    /**
     * 新增代理商
     * @param agentBo 代理商对象
     * @return
     */
    R<String> save(AgentBo agentBo);

    /**
     * 修改代理商
     * @param agentBo 代理商对象
     * @return
     */
    R<String> update(AgentBo agentBo);

    /**
     * 删除代理商
     * @param id 代理商id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 代理商用户获取对应的代理商信息
     *
     * @return 代理商信息
     */
    R<AgentInfoVo> currentUserInfo();

    /**
     * 修改代理商状态
     *
     * @param id          代理商id
     * @param agentStatus 代理商状态 0：未启用 1：启用中
     */
    R<String> updateAgentStatus(Long id, Integer agentStatus);
}

