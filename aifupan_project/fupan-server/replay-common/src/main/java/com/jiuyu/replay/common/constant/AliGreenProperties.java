package com.jiuyu.replay.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/8 上午11:19
 */
@Component
@Data
@ConfigurationProperties(prefix = "third.ali.green")
public class AliGreenProperties {

    /**
     * secretId
     */
    private String secretId;
    /**
     * secretKey
     */
    private String secretKey;
    /**
     * 内网域名
     */
    private String endpointEms;
    /**
     * 外网域名
     */
    private String endpoint;
    /**
     * 区域
     */
    private String region;
    /**
     * 是否外网
     */
    private Boolean isOuterNet = false;
}
