package com.jiuyu.replay.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "third.tencent.audio")
public class TencentAudioProperties {

    /**
     * 语音识别接口的Qps余量 redis-key
     */
    private String redisKeyPrefix;
    /**
     * 语音识别接口的Qps余量timeout，用于监听过期加回余量
     */
    private String redisTimeoutKeyPrefix;
    /**
     * 语音识别接口日志 redis-key
     */
    private String logKeyPrefix;
    /**
     * 语音识别临时凭证 redis-key
     */
    private String redisTempTokenKeyPrefix;

    /**
     * 语音识别secret
     */
    private List<AudioSecretProperties> secret;

    /**
     * 语音识别接口qps记录 redis-key
     */
    private String qpsLogKeyPrefix;

}
