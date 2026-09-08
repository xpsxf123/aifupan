package com.jiuyu.replay.api.service.agentusage.dto;

import lombok.Data;

import java.util.List;

/**
 * 外部智能体统计接口的响应信封（爱复盘 ApiResponse 口径）。
 * 成败以 code == 0 判定（HTTP 状态码恒 200）。用 fastjson2 反序列化，未知字段默认忽略。
 */
@Data
public class AgentUsageApiResp {

    /**
     * 0 表示成功（不是 200）；非 0 即失败
     */
    private Integer code;

    private String msg;

    private UsageData data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    @Data
    public static class UsageData {
        /**
         * 实际生效窗口，仅回显用
         */
        private String startTime;
        private String endTime;
        /**
         * 逐 id 结果，顺序与入参一致，无数据补 0
         */
        private List<UsageItem> items;
    }

    @Data
    public static class UsageItem {
        /**
         * 接口1-4：租户 id 或用户 id（字符串回显）
         */
        private String id;
        /**
         * 接口5（user-tenant/usage）：用户 id（number 回显）
         */
        private Long userId;
        /**
         * 接口5（user-tenant/usage）：租户 id（number 回显）
         */
        private Long tenantId;
        private Long billedTokens;
        private Long userMessageCount;
    }
}
