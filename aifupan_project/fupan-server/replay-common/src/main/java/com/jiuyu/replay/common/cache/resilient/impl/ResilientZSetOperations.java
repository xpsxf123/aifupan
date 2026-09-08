package com.jiuyu.replay.common.cache.resilient.impl;

import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate.CacheEntry;
import com.jiuyu.replay.common.cache.resilient.api.ZSetOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Redis降级包装类的有序集合操作实现类
 *
 * @author RayChou
 * @date 2025/7/7
 */
@RequiredArgsConstructor
@Slf4j
public class ResilientZSetOperations<K, V> implements ZSetOperations<K, V> {

    private final ResilientRedisTemplate<K, V> template;

    @Override
    @SuppressWarnings("unchecked")
    public Boolean add(K key, V value, double score) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForZSet().add(key, value, score);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis ZSet操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：写入本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Boolean[] result = {false};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前ZSet
                Map<V, Double> zset = getLocalZSet(key);
                if (zset == null) {
                    zset = new HashMap<>();
                }

                // 添加元素
                boolean isNew = !zset.containsKey(value);
                zset.put(value, score);

                // 更新缓存
                template.updateLocalCache(cacheKey, (V) zset);
                result[0] = isNew;
            });

            return result[0];
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Double score(K key, V value) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Double result = template.getRedisTemplate().opsForZSet().score(key, value);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis ZSet操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Map<V, Double> zset = getLocalZSet(key);
            return zset != null ? zset.get(value) : null;
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Double incrementScore(K key, V value, double delta) {
        // 如果未降级，尝试在Redis中递增
        if (!template.isDegraded()) {
            try {
                Double result = template.getRedisTemplate().opsForZSet().incrementScore(key, value, delta);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis ZSet操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Double[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                Map<V, Double> zset = getLocalZSet(key);
                if (zset == null) {
                    zset = new HashMap<>();
                }

                // 获取当前分数
                Double currentScore = zset.getOrDefault(value, 0.0);

                // 增加分数
                double newScore = currentScore + delta;

                // 更新分数
                zset.put(value, newScore);

                // 更新缓存
                template.updateLocalCache(cacheKey, (V) zset);

                result[0] = newScore;
            });

            return result[0];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<V> range(K key, long start, long end) {
        // 如果未降级，尝试从Redis查询
        if (!template.isDegraded()) {
            try {
                Set<V> values = template.getRedisTemplate().opsForZSet().range(key, start, end);
                template.resetFailures();
                return values;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis ZSet操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存中的ZSet查询范围
        if (template.isDegraded()) {
            Map<V, Double> zset = getLocalZSet(key);

            // 将元素按分数排序
            List<Map.Entry<V, Double>> sortedEntries = new ArrayList<>(zset.entrySet());
            sortedEntries.sort(Map.Entry.comparingByValue());

            // 返回指定范围的元素
            Set<V> result = new LinkedHashSet<>();
            int s = (int) Math.max(start, 0);
            int e;

            // 处理end为-1的情况，表示获取到集合末尾
            if (end == -1) {
                e = sortedEntries.size() - 1;
            } else {
                e = (int) Math.min(end, sortedEntries.size() - 1);
            }

            if (s <= e && s < sortedEntries.size()) {
                for (int i = s; i <= e; i++) {
                    result.add(sortedEntries.get(i).getKey());
                }
            }

            return result;
        }

        return new HashSet<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long remove(K key, Object... values) {
        // 如果未降级，尝试从Redis删除
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForZSet().remove(key, values);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis ZSet操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存移除元素
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Long[] result = {0L};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                Map<V, Double> zset = getLocalZSet(key);
                long count = 0;

                // 删除指定的元素
                for (Object value : values) {
                    if (zset.remove(value) != null) {
                        count++;
                    }
                }

                template.updateLocalCache(cacheKey, (V) zset);
                result[0] = count;
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
                Long result = template.getRedisTemplate().opsForZSet().size(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis ZSet操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Map<V, Double> zset = getLocalZSet(key);
            return zset != null ? (long) zset.size() : 0L;
        }

        return 0L;
    }

    /**
     * 从本地缓存获取ZSet
     *
     * @param key 键
     * @return ZSet
     */
    @SuppressWarnings("unchecked")
    private Map<V, Double> getLocalZSet(K key) {
        String cacheKey = key.toString();
        CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);

        if (entry != null && !entry.isExpired()) {
            if (entry.getValue() instanceof Map) {
                return (Map<V, Double>) entry.getValue();
            }
        }

        return new HashMap<>();
    }
} 