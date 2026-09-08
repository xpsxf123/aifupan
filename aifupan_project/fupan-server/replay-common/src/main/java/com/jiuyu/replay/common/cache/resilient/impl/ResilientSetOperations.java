package com.jiuyu.replay.common.cache.resilient.impl;

import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate.CacheEntry;
import com.jiuyu.replay.common.cache.resilient.api.SetOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Redis降级包装类的集合操作实现类
 *
 * @author RayChou
 * @date 2025/7/7
 */
@RequiredArgsConstructor
@Slf4j
public class ResilientSetOperations<K, V> implements SetOperations<K, V> {

    private final ResilientRedisTemplate<K, V> template;
    private final Random random = new Random();

    @Override
    @SuppressWarnings("unchecked")
    public Long add(K key, V... values) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForSet().add(key, values);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Set操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：写入本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Long[] result = {0L};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前Set
                Set<V> set = getLocalSet(key);
                if (set == null) {
                    set = new HashSet<>();
                }

                // 添加元素
                long addedCount = 0;
                for (V value : values) {
                    if (set.add(value)) {
                        addedCount++;
                    }
                }

                // 更新缓存
                template.updateLocalCache(cacheKey, (V) set);
                result[0] = addedCount;
            });

            return result[0];
        }

        return 0L;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<V> members(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Set<V> result = template.getRedisTemplate().opsForSet().members(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Set操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Set<V> set = getLocalSet(key);
            return set != null ? new HashSet<>(set) : new HashSet<>();
        }

        return new HashSet<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean isMember(K key, V value) {
        // 如果未降级，尝试从Redis查询
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForSet().isMember(key, value);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Set操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Set<V> set = getLocalSet(key);
            return set != null && set.contains(value);
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long remove(K key, Object... values) {
        // 如果未降级，尝试从Redis移除
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForSet().remove(key, values);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Set操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存移除
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Long[] result = {0L};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前Set
                Set<V> set = getLocalSet(key);
                if (set != null) {
                    // 移除元素
                    long removedCount = 0;
                    for (Object value : values) {
                        if (set.remove(value)) {
                            removedCount++;
                        }
                    }

                    // 更新缓存
                    template.updateLocalCache(cacheKey, (V) set);
                    result[0] = removedCount;
                }
            });

            return result[0];
        }

        return 0L;
    }

    @Override
    public Long size(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForSet().size(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Set操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Set<V> set = getLocalSet(key);
            return set != null ? (long) set.size() : 0L;
        }

        return 0L;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V randomMember(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                V result = template.getRedisTemplate().opsForSet().randomMember(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Set操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Set<V> set = getLocalSet(key);
            if (set != null && !set.isEmpty()) {
                int size = set.size();
                int randomIndex = random.nextInt(size);
                int i = 0;
                for (V value : set) {
                    if (i == randomIndex) {
                        return value;
                    }
                    i++;
                }
            }
        }

        return null;
    }

    /**
     * 从本地缓存获取Set
     *
     * @param key 键
     * @return Set
     */
    @SuppressWarnings("unchecked")
    private Set<V> getLocalSet(K key) {
        String cacheKey = key.toString();
        CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);

        if (entry != null && !entry.isExpired()) {
            if (entry.getValue() instanceof Set) {
                return (Set<V>) entry.getValue();
            }
        }

        return new HashSet<>();
    }
} 