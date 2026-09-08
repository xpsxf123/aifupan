package com.jiuyu.replay.api.service.agentusage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 「用户 × 租户」对，作为接口5(user-tenant/usage)的请求单位。
 * userId/tenantId 走 JSON number（Long），双方均为 Java 服务，无 JS 中间层。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTenantPair {

    private Long userId;

    private Long tenantId;

    /**
     * 结果 map 的键：userId_tenantId
     */
    public static String key(Long userId, Long tenantId) {
        return userId + "_" + tenantId;
    }

    public String key() {
        return key(userId, tenantId);
    }
}
