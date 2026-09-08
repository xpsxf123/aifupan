package com.jiuyu.replay.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能体用量统计外部接口配置（对接 ifupan-anchor-agent 的开放统计接口）
 * 密钥放各 profile yml（配置中心），不硬编码、不下发前端；仅服务端调用。
 */
@Component
@Data
@ConfigurationProperties(prefix = "agent-stats")
public class AgentUsageProperties {

    /**
     * 接口基础地址。dev 带 /api，prod 不带；务必配置化，不在代码里拼死前缀。
     * 完整地址 = baseUrl + /open/stats/{user|tenant}/usage
     */
    private String baseUrl;

    /**
     * 调用方 appId，放请求头 x-jiuyu-client-id
     */
    private String appId = "fupan-server";

    /**
     * 该 appId 对应环境的密钥，放请求头 api-key
     */
    private String apiKey;

    /**
     * 统计窗口天数，startTime = now - windowDays，endTime 不传
     */
    private int windowDays = 60;

    /**
     * 连接超时(毫秒)
     */
    private int connectTimeoutMs = 3000;

    /**
     * 读取超时(毫秒)
     */
    private int readTimeoutMs = 10000;

    /**
     * 本地缓存 TTL(秒)
     */
    private int cacheTtlSeconds = 300;

    /**
     * 单批 id 上限，超出分批
     */
    private int maxIdsPerBatch = 200;
}
