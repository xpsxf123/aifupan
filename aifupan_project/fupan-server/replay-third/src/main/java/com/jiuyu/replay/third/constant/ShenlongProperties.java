package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.shenlong")
public class ShenlongProperties {

    /**
     * 代理ip redis缓存key前缀
     */
    private String proxyIpUserRedisKeyPrefix;
}
