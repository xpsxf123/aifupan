package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.chanmama")
public class ChanmamaProperties {

    /**
     * token保存的redis-key前缀
     */
    private String tokenRedisKey;
    /**
     * 请求url
     */
    private String requestBaseUrl;
    /**
     * 回调地址
     */
    private String callbackUrl;
    /**
     * 主播发送记录保存的redis-key前缀
     */
    private String anchorRedisKey;
}
