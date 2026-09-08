package com.jiuyu.replay.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(new ToStringRedisSerializer());

        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存管理器配置
     * 1. 使用JSON序列化，与RedisTemplate保持一致
     * 2. 实现过期时间打散，防止缓存雪崩
     * 3. 区分不同业务数据的缓存策略
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory, @Value("${spring.cache.redis.time-to-live:600000}") long timeToLive, @Value("${spring.cache.redis.cache-null-values:false}") boolean cacheNullValues) {
        // 创建ObjectMapper
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);

        // 使用GenericJackson2JsonRedisSerializer替代Jackson2JsonRedisSerializer
        RedisSerializer<Object> serializer = new GenericJackson2JsonRedisSerializer(om);

        // 创建RedisCacheConfiguration并显式设置序列化器
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .computePrefixWith(name -> "replay:systemGlobal:" + name + ":");

        if (!cacheNullValues) {
            // 不缓存null值，避免占用缓存空间，但是会有缓存穿透风险
            defaultConfig = defaultConfig.disableCachingNullValues();
        }
        // 不同业务数据配置不同过期时间
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        // 用户数据 - 过期时间较长且打散
        // configMap.put("users", defaultConfig.entryTtl(randomTtl(60, 10))); // 60±10分钟
        // 构建缓存管理器 默认10分钟
        return RedisCacheManager.builder(factory).cacheDefaults(defaultConfig.entryTtl(Duration.ofMillis(timeToLive))).withInitialCacheConfigurations(configMap).build();
    }

    /**
     * 生成随机过期时间，防止缓存雪崩
     */
    private Duration randomTtl(int baseMinutes, int randomRange) {
        return Duration.ofMinutes(baseMinutes + new Random().nextInt(randomRange * 2) - randomRange);
    }

    /**
     * 自定义缓存键生成器
     * 支持更灵活的键生成策略
     */
    @Bean
    public KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName()).append(":");
            for (Object param : params) {
                if (param != null) {
                    sb.append(param.toString()).append("_");
                } else {
                    sb.append("null_");
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        };
    }

    /**
     * 配置过期监听
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
