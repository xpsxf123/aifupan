package com.jiuyu.replay.api.config;

import com.jiuyu.replay.common.properties.RocketMqProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author RayChou
 * @date 2025/6/7 9:58
 */
@Configuration
@EnableConfigurationProperties(RocketMqProperties.class)
public class RocketMqConfig {
}
