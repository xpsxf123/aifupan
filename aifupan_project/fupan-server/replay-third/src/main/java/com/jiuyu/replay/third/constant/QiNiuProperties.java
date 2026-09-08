package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.qiniu.oss")
public class QiNiuProperties {

    /**
     * accessKey
     */
    private String accessKey;
    /**
     * secretKey
     */
    private String secretKey;
    /**
     * 存储空间
     */
    private String bucket;
    /**
     * 下载地址
     */
    private String downloadDomain;
}
