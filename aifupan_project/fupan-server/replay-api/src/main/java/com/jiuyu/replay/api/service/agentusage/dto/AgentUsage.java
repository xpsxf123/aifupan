package com.jiuyu.replay.api.service.agentusage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体用量内部聚合值对象（单个 user/tenant 的用量），也用作本地缓存的 value。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentUsage {

    /**
     * 窗口内计费 token 合计
     */
    private long billedTokens;

    /**
     * 窗口内用户提问条数（会话数）
     */
    private long userMessageCount;

    /**
     * 降级/无数据时的零值
     */
    public static AgentUsage zero() {
        return new AgentUsage(0L, 0L);
    }
}
