package com.jiuyu.replay.api.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CRM 向智能体出站调用配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "crm.agent")
public class CrmAgentIntegrationProperties {

    /**
     * 智能体服务基础地址
     */
    private String baseUrl;

    /**
     * 智能体共享鉴权密钥
     */
    private String apiKey;

    /**
     * 订单事件推送路径
     */
    private String orderEventPath;
}
