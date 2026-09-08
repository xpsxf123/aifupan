package com.jiuyu.replay.common.http.governance;

import com.jiuyu.replay.common.http.ServiceInstance;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 爱复盘服务端配置
 *
 * @author HeHui
 * @date 2025-11-11 15:37
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jiuyu.governance-server")
public class FupanServerProperties {

    /**
     * 基础URL
     */
    private String baseUrl = "http://governance";

    private String clientAppHandler = "x-jiuyu-client_id";

    private String clientSecretHandler = "api-key";

    /**
     * 客户端应用ID
     */
    private String clientAppId;

    /**
     * 客户端应用密钥
     */
    private String clientAppSecret;


    /**
     * 认证Token名称
     */
    private String tokenName = "Token";


    /**
     * 负载均衡开关
     */
    private Boolean enableLoadBalance = false;

    /**
     * 服务实例列表
     */
    private List<ServiceInstance> serviceInstances = new ArrayList<>();
}
