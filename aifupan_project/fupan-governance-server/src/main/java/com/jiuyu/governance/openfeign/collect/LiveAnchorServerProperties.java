package com.jiuyu.governance.openfeign.collect;

import com.jiuyu.governance.plugins.http.ServiceInstance;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Python对接的各直播平台 服务端配置
 *
 * @author HeHui
 * @date 2025-11-11 15:37
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jiuyu.live-anchor")
public class LiveAnchorServerProperties {

    /**
     * 基础URL
     */
    private String baseUrl = "http://live-anchor";

    /**
     *  客户端ID
     */
    private String clientId;

    /**
     * 密钥
     */
    private String clientSecret;


    /**
     * 负载均衡开关
     */
    private Boolean enableLoadBalance = false;

    /**
     * 服务实例列表
     */
    private List<ServiceInstance> serviceInstances = new ArrayList<>();
}
