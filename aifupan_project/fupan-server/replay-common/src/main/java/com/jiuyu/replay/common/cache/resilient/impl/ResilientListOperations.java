package com.jiuyu.replay.common.cache.resilient.impl;

import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate.CacheEntry;
import com.jiuyu.replay.common.cache.resilient.api.ListOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis降级包装类的列表操作实现类
 *
 * @author RayChou
 * @date 2025/7/7
 */
@RequiredArgsConstructor
@Slf4j
public class ResilientListOperations<K, V> implements ListOperations<K, V> {

    private final ResilientRedisTemplate<K, V> template;

    @Override
    @SuppressWarnings("unchecked")
    public V index(K key, long index) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                V result = template.getRedisTemplate().opsForList().index(key, index);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            List<V> list = getLocalList(key);
            if (list != null && index >= 0 && index < list.size()) {
                return list.get((int) index);
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<V> range(K key, long start, long end) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                List<V> result = template.getRedisTemplate().opsForList().range(key, start, end);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            List<V> list = getLocalList(key);
            if (list != null) {
                int s = (int) Math.max(start, 0);
                int e;

                // 处理end为-1的情况，表示获取到列表末尾
                if (end == -1) {
                    e = list.size() - 1;
                } else {
                    e = (int) Math.min(end, list.size() - 1);
                }

                if (s <= e && s < list.size()) {
                    return new ArrayList<>(list.subList(s, e + 1));
                }
            }
        }

        return new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long leftPush(K key, V value) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForList().leftPush(key, value);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：写入本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Long[] result = {0L};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                List<V> list = getLocalList(key);
                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(0, value);
                template.updateLocalCache(cacheKey, (V) list);
                result[0] = (long) list.size();
            });

            return result[0];
        }

        return 0L;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long rightPush(K key, V value) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForList().rightPush(key, value);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：写入本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Long[] result = {0L};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                List<V> list = getLocalList(key);
                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(value);
                template.updateLocalCache(cacheKey, (V) list);
                result[0] = (long) list.size();
            });

            return result[0];
        }

        return 0L;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V leftPop(K key) {
        // 如果未降级，尝试从Redis弹出
        if (!template.isDegraded()) {
            try {
                V result = template.getRedisTemplate().opsForList().leftPop(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存弹出
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final V[] result = (V[]) new Object[1];

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                List<V> list = getLocalList(key);
                if (list != null && !list.isEmpty()) {
                    result[0] = list.remove(0);
                    template.updateLocalCache(cacheKey, (V) list);
                }
            });

            return result[0];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V rightPop(K key) {
        // 如果未降级，尝试从Redis弹出
        if (!template.isDegraded()) {
            try {
                V result = template.getRedisTemplate().opsForList().rightPop(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存弹出
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final V[] result = (V[]) new Object[1];

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                List<V> list = getLocalList(key);
                if (list != null && !list.isEmpty()) {
                    result[0] = list.remove(list.size() - 1);
                    template.updateLocalCache(cacheKey, (V) list);
                }
            });

            return result[0];
        }

        return null;
    }

    @Override
    public Long size(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForList().size(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis List操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            List<V> list = getLocalList(key);
            return list != null ? (long) list.size() : 0L;
        }

        return 0L;
    }

    /**
     * 从本地缓存获取List
     *
     * @param key 键
     * @return List
     */
    @SuppressWarnings("unchecked")
    private List<V> getLocalList(K key) {
        String cacheKey = key.toString();
        CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);

        if (entry != null && !entry.isExpired()) {
            if (entry.getValue() instanceof List) {
                return (List<V>) entry.getValue();
            }
        }

        return new ArrayList<>();
    }
} 