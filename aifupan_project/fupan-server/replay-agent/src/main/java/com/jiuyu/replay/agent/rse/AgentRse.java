package com.jiuyu.replay.agent.rse;


import com.jiuyu.replay.generic.vo.agent.AgentInfoVo;

public interface AgentRse {

    /**
     * 代理商信息
     * @param id 代理商id
     * @param agentStatus 代理商状态 0：未启用 1：启用中
     * @return
     */
    AgentInfoVo info(Long id, Integer agentStatus);

}
