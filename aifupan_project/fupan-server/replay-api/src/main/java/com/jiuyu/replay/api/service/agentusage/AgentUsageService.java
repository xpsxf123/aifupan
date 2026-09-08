package com.jiuyu.replay.api.service.agentusage;

import com.jiuyu.replay.api.service.agentusage.dto.AgentUsage;
import com.jiuyu.replay.api.service.agentusage.dto.UserTenantPair;

import java.util.Collection;
import java.util.Map;

/**
 * 智能体用量统计（对接外部 ifupan-anchor-agent 开放接口）。
 * 实时调用 + 本地缓存 5 分钟；任何失败/超时直接降级为 0，不重试、不抛异常、不阻断调用方。
 */
public interface AgentUsageService {

    /**
     * 批量查「用户 × 租户」维度用量（接口5 user-tenant/usage）。
     * 只统计用户在指定租户下的用量——用户当前租户取 active_tenant_id，避免跨租户糊成一笔。
     * @param pairs (userId, tenantId) 对，tenantId 传用户当前 active_tenant_id
     * @return key=UserTenantPair.key(userId,tenantId) -> 用量；入参每对都有值（无数据/降级为 0）
     */
    Map<String, AgentUsage> batchUserTenantUsage(Collection<UserTenantPair> pairs);

    /**
     * 批量查租户维度用量。
     * @param tenantIds 租户 id（tb_user.active_tenant_id）
     * @return tenantId -> 用量；入参每个 id 都有值（无数据/降级为 0）
     */
    Map<Long, AgentUsage> batchTenantUsage(Collection<Long> tenantIds);
}
