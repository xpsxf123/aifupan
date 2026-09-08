package com.jiuyu.replay.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 话术智能监控 RocketMQ 配置入口（注册 {@link ScriptMonitorMqProperties}）。
 *
 * @author beta
 * @date 2026-06-01
 */
@Configuration
@EnableConfigurationProperties(ScriptMonitorMqProperties.class)
public class ScriptMonitorMqConfig {
}
