package com.jiuyu.replay.common.cache;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis降级配置类，启用定时任务和注册监控指标
 *
 * @author RayChou
 * @date 2025/7/2
 */
@Configuration
public class RedisResilientConfig {

    /**
     * 注册Redis监控指标
     *
     * @param resilientRedisTemplate Redis降级模板
     * @param hybridLockService      分布式锁服务
     * @return MeterBinder对象
     */
    @Bean
    public MeterBinder redisMetricsBinder(
            ResilientRedisTemplate<?, ?> resilientRedisTemplate,
            HybridLockService hybridLockService) {

        return registry -> {
            // Redis缓存降级状态指标
            Gauge.builder("redis.cache.degraded",
                            () -> resilientRedisTemplate.isDegraded() ? 1 : 0)
                    .description("Redis缓存是否处于降级状态")
                    .register(registry);

            // Redis锁降级状态指标
            Gauge.builder("redis.lock.degraded",
                            () -> hybridLockService.isDegraded() ? 1 : 0)
                    .description("Redis锁是否处于降级状态")
                    .register(registry);

            // 本地缓存大小指标
            Gauge.builder("redis.local.cache.size",
                            resilientRedisTemplate::getLocalCacheSize)
                    .description("本地缓存条目数量")
                    .register(registry);
        };
    }
} 