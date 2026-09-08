package com.jiuyu.replay.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "third.tencent.vod")
public class TencentVodProperties {

    /**
     * vod临时凭证 redis-key
     */
    private String redisVodTempTokenKeyPrefix;
    /**
     * 语音识别secretId
     */
    private String secretId;
    /**
     * 语音识别secretKey
     */
    private String secretKey;
    /**
     * 应用id
     */
    private String subAppId;

}
