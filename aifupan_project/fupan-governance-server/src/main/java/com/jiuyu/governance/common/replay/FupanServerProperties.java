package com.jiuyu.governance.common.replay;

import com.jiuyu.governance.plugins.http.ServiceInstance;
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
@ConfigurationProperties(prefix = "jiuyu.fupan-server")
public class FupanServerProperties {

    /**
     * 基础URL
     */
    private String baseUrl = "http://replay";

    /**
     *  客户端ID
     */
    private String clientId;

    /**
     * 密钥
     */
    private String clientSecret;


    private String appId;


    private String appSecret;


    /**
     * 负载均衡开关
     */
    private Boolean enableLoadBalance = false;

    /**
     * 服务实例列表
     */
    private List<ServiceInstance> serviceInstances = new ArrayList<>();
}
