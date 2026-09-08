package com.jiuyu.replay.third.config;

import com.jiuyu.replay.common.constant.AudioSecretProperties;
import com.jiuyu.replay.common.constant.TencentAudioProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Configuration
@Lazy(value = false)
@Slf4j
public class ThirdStartInit {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TencentAudioProperties tencentAudioProperties;

    /**
     * springboot启动后初始化配置
     */
    @PostConstruct
    public void init() {
        log.info("初始化ThirdStartInit");
        // 初始化每个腾讯语言识别Qps余量
        List<AudioSecretProperties> secret = tencentAudioProperties.getSecret();
        for (AudioSecretProperties audioSecretProperties : secret) {
            redisTemplate.opsForValue().set(tencentAudioProperties.getRedisKeyPrefix() + audioSecretProperties.getSecretId(), audioSecretProperties.getQpsNum());
        }

    }
}
