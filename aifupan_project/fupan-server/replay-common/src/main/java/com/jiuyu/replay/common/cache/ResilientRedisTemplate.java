package com.jiuyu.replay.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jiuyu.replay.common.cache.resilient.api.*;
import com.jiuyu.replay.common.cache.resilient.impl.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis降级包装类，在Redis不可用时自动切换到本地缓存
 *
 * @author RayChou
 * @date 2025/7/2
 */
@Slf4j
@Component
public class ResilientRedisTemplate<K, V> {

    @Resource
    @Getter
    private RedisTemplate<K, V> redisTemplate;

    @Resource
    @Getter
    private HybridLockService hybridLockService;

    // 降级状态控制
    private final AtomicBoolean degraded = new AtomicBoolean(false);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    // 本地缓存
    @Getter
    private Cache<Object, CacheEntry<V>> localCache;

    // 配置参数
    @Value("${redis.resilient.failure-threshold:3}")
    private int failureThreshold;

    @Value("${redis.resilient.retry-interval-ms:30000}")
    private long retryIntervalMs;

    @Value("${redis.resilient.local-cache-size:10000}")
    private int localCacheSize;

    @Value("${redis.resilient.local-cache-ttl-seconds:86400}")
    @Getter
    private int localCacheTtlSeconds;

    // 操作对象，预先创建
    private final ValueOperations<K, V> valueOps = new ResilientValueOperations<>(this);
    private final ListOperations<K, V> listOps = new ResilientListOperations<>(this);
    private final SetOperations<K, V> setOps = new ResilientSetOperations<>(this);
    private final ZSetOperations<K, V> zSetOps = new ResilientZSetOperations<>(this);

    // 缓存条目，包含值和过期时间
    @Getter
    public static class CacheEntry<T> {
        private final T value;
        private final long expireAt;

        public CacheEntry(T value, long ttlMillis) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + ttlMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /**
     * 初始化方法，在属性注入后调用
     */
    @PostConstruct
    public void init() {
        // 使用配置参数构建缓存
        this.localCache = Caffeine.newBuilder()
                .maximumSize(localCacheSize)
                // 设置较长的全局过期时间作为安全网
                .expireAfterWrite(localCacheTtlSeconds, TimeUnit.SECONDS)
                .build();
        log.info("[Redis降级] 初始化本地缓存，大小限制: {}, 默认TTL: {}秒",
                localCacheSize, localCacheTtlSeconds);
    }

    /**
     * 获取值操作对象
     *
     * @return 值操作对象
     */
    public ValueOperations<K, V> opsForValue() {
        return this.valueOps;
    }
    
    /**
     * 获取列表操作对象
     *
     * @return 列表操作对象
     */
    public ListOperations<K, V> opsForList() {
        return this.listOps;
    }

    /**
     * 获取哈希操作对象
     *
     * @return 哈希操作对象
     */
    @SuppressWarnings("unchecked")
    public <HK, HV> HashOperations<K, HK, HV> opsForHash() {
        return new ResilientHashOperations<>(this);
    }

    /**
     * 获取集合操作对象
     *
     * @return 集合操作对象
     */
    public SetOperations<K, V> opsForSet() {
        return this.setOps;
    }

    /**
     * 获取有序集合操作对象
     *
     * @return 有序集合操作对象
     */
    public ZSetOperations<K, V> opsForZSet() {
        return this.zSetOps;
    }

    /**
     * 删除键
     *
     * @param key 键
     * @return 是否成功
     */
    public Boolean delete(K key) {
        return opsForValue().delete(key);
    }

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(K key) {
        return opsForValue().hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    public Boolean expire(K key, long timeout, TimeUnit unit) {
        return opsForValue().expire(key, timeout, unit);
    }

    /**
     * 判断是否处于降级状态
     *
     * @return 是否降级
     */
    public boolean isDegraded() {
        if (degraded.get()) {
            // 检查是否应该尝试恢复
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFailureTime.get() > retryIntervalMs) {
                // 超过重试间隔，但不在这里修改状态，而是让健康检查来处理
                // 这里只返回当前状态
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * 更新本地缓存
     *
     * @param key   键
     * @param value 值
     */
    public <T> void updateLocalCache(String key, T value) {
        updateLocalCache(key, value, TimeUnit.SECONDS.toMillis(localCacheTtlSeconds));
    }

    /**
     * 更新本地缓存，支持自定义过期时间
     *
     * @param key       键
     * @param value     值
     * @param ttlMillis 过期时间（毫秒）
     */
    @SuppressWarnings("unchecked")
    public <T> void updateLocalCache(String key, T value, long ttlMillis) {
        localCache.put(key, new CacheEntry<V>((V) value, ttlMillis));
    }

    /**
     * 记录失败并可能触发降级
     */
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int currentFailures = consecutiveFailures.incrementAndGet();
        if (currentFailures >= failureThreshold && !degraded.get()) {
            // 只有在之前不是降级状态时，才设置为降级并记录日志
            degraded.set(true);
            log.error("[Redis降级] Redis连续{}次操作失败，启用降级模式", currentFailures);
        }
    }

    /**
     * 重置失败计数
     */
    public void resetFailures() {
        // 只重置失败计数，不改变降级状态
        // 降级状态的改变由健康检查或手动设置负责
        consecutiveFailures.set(0);
    }

    /**
     * 生成缓存锁的键名
     *
     * @param key 原始键
     * @return 锁的键名
     */
    public String getLockKey(String key) {
        return "replay:localCache:lock:" + key;
    }

    /**
     * 手动设置降级状态
     *
     * @param degraded 是否降级
     */
    public void setDegraded(boolean degraded) {
        boolean oldState = this.degraded.get();
        this.degraded.set(degraded);

        if (degraded) {
            // 进入降级状态
            lastFailureTime.set(System.currentTimeMillis());
            if (!oldState) {
                log.info("[Redis降级] 手动启用降级模式");
            }
        } else {
            // 退出降级状态
            consecutiveFailures.set(0);
            if (oldState) {
                log.info("[Redis降级] 手动关闭降级模式");
            }
        }
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCache.invalidateAll();
    }

    /**
     * 获取本地缓存的大小
     *
     * @return 缓存中的条目数
     */
    public long getLocalCacheSize() {
        return localCache.estimatedSize();
    }

    /**
     * 在Redis恢复后清空本地缓存
     * 此方法应由健康检查服务调用，而不是每次操作后调用
     */
    public void clearCacheAfterRecovery() {
        if (!isDegraded()) {
            log.info("[Redis降级] Redis已恢复，清空本地缓存");
            clearLocalCache();
        }
    }

    /**
     * 检查Redis连接并尝试退出降级模式
     * 此方法由健康检查服务定期调用
     * 注意：此方法只负责状态检查和切换，清理工作统一由handleServiceRecovery处理
     *
     * @return 是否成功退出降级模式
     */
    public boolean checkAndExitDegradedMode() {
        // 只有在降级状态才需要检查
        if (degraded.get()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFailureTime.get() > retryIntervalMs) {
                // 超过重试间隔，尝试退出降级模式
                // Redis连接已在RedisHealthChecker中验证，这里可以直接退出降级模式
                consecutiveFailures.set(0);
                degraded.set(false);
                log.info("[Redis降级] Redis恢复正常，退出降级模式");

                // 清理工作统一由handleServiceRecovery处理，这里不再执行
                return true;
            }
        }
        return false;
    }
}