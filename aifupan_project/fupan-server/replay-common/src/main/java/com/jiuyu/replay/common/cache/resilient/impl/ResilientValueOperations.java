package com.jiuyu.replay.common.cache.resilient.impl;

import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate.CacheEntry;
import com.jiuyu.replay.common.cache.resilient.api.ValueOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis降级包装类的值操作实现类
 *
 * @author RayChou
 * @date 2025/7/7
 */
@RequiredArgsConstructor
@Slf4j
public class ResilientValueOperations<K, V> implements ValueOperations<K, V> {

    private final ResilientRedisTemplate<K, V> template;

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return get((K) key, null);
    }

    @Override
    public V get(K key, Supplier<V> fallback) {
        // 如果未降级，直接从Redis获取
        if (!template.isDegraded()) {
            try {
                V value = template.getRedisTemplate().opsForValue().get(key);
                template.resetFailures();

                // 如果Redis中没有值，使用回调获取
                if (value == null && fallback != null) {
                    value = fallback.get();
                    if (value != null) {
                        template.getRedisTemplate().opsForValue().set(key, value);
                    }
                }

                return value;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis获取值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：先查本地缓存
        String cacheKey = key.toString();
        CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }

        // 本地缓存也没有，使用回调函数
        if (fallback != null) {
            V value = fallback.get();
            if (value != null) {
                template.updateLocalCache(cacheKey, value);
            }
            return value;
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(Object key, Object value) {
        set(key, value, template.getLocalCacheTtlSeconds(), TimeUnit.SECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(Object key, Object value, long timeout, TimeUnit unit) {
        K typedKey = (K) key;
        V typedValue = (V) value;

        // 如果未降级，只写入Redis
        if (!template.isDegraded()) {
            try {
                template.getRedisTemplate().opsForValue().set(typedKey, typedValue, timeout, unit);
                template.resetFailures();
                return;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 只有在降级模式下才写入本地缓存
        if (template.isDegraded() && typedValue != null) {
            String cacheKey = typedKey.toString();
            // 使用与Redis相同的过期时间
            template.updateLocalCache(cacheKey, typedValue, unit.toMillis(timeout));
        }
    }

    @Override
    public List<V> multiGet(Collection<K> keys) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                List<V> values = template.getRedisTemplate().opsForValue().multiGet(keys);
                template.resetFailures();
                return values;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis批量获取值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下，从本地缓存获取每个键的值
        if (template.isDegraded() && keys != null) {
            List<V> result = new ArrayList<>(keys.size());
            for (K key : keys) {
                String cacheKey = key.toString();
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                if (entry != null && !entry.isExpired()) {
                    result.add(entry.getValue());
                } else {
                    result.add(null);
                }
            }
            return result;
        }

        return new ArrayList<>();
    }

    @Override
    public void multiSet(Map<? extends K, ? extends V> map) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                template.getRedisTemplate().opsForValue().multiSet(map);
                template.resetFailures();
                return;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis批量设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下，写入本地缓存
        if (template.isDegraded() && map != null) {
            // 获取所有键的锁，按键的自然顺序排序以避免死锁
            List<String> keys = new ArrayList<>();
            for (K key : map.keySet()) {
                keys.add(key.toString());
            }
            java.util.Collections.sort(keys);

            // 依次获取所有锁并执行操作
            executeMultiLock(0, keys, map);
        }
    }

    /**
     * 递归获取多个锁并执行批量操作
     */
    private void executeMultiLock(int index, List<String> keys, Map<? extends K, ? extends V> map) {
        if (index >= keys.size()) {
            // 所有锁都已获取，执行批量更新
            map.forEach((key, value) -> {
                if (value != null) {
                    String cacheKey = key.toString();
                    // 使用默认过期时间
                    long ttl = TimeUnit.SECONDS.toMillis(template.getLocalCacheTtlSeconds());
                    template.updateLocalCache(cacheKey, value, ttl);
                }
            });
            return;
        }

        String lockKey = keys.get(index);
        template.getHybridLockService().executeWithLock(template.getLockKey(lockKey), 5, () -> {
            executeMultiLock(index + 1, keys, map);
        });
    }

    @Override
    public Boolean setIfAbsent(K key, V value) {
        return setIfAbsent(key, value, template.getLocalCacheTtlSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public Boolean setIfAbsent(K key, V value, long timeout, TimeUnit unit) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForValue().setIfAbsent(key, value, timeout, unit);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis原子设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final boolean[] success = {false};

            boolean executed = template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 检查键是否存在
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                if (entry != null && !entry.isExpired()) {
                    // 键已存在，返回失败
                    success[0] = false;
                    return;
                }

                // 键不存在，设置值
                long ttlMillis = unit.toMillis(timeout);
                template.updateLocalCache(cacheKey, value, ttlMillis);
                success[0] = true;
            });

            return executed && success[0];
        }

        return false;
    }

    @Override
    public Boolean setIfPresent(K key, V value) {
        return setIfPresent(key, value, template.getLocalCacheTtlSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public Boolean setIfPresent(K key, V value, long timeout, TimeUnit unit) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForValue().setIfPresent(key, value, timeout, unit);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis原子设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final boolean[] success = {false};

            boolean executed = template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 检查键是否存在
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                if (entry == null || entry.isExpired()) {
                    // 键不存在，返回失败
                    success[0] = false;
                    return;
                }

                // 键存在，设置新值
                long ttlMillis = unit.toMillis(timeout);
                template.updateLocalCache(cacheKey, value, ttlMillis);
                success[0] = true;
            });

            return executed && success[0];
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long increment(Object key, long delta) {
        K typedKey = (K) key;

        // 如果未降级，尝试在Redis中增加
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForValue().increment(typedKey, delta);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis增加值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = typedKey.toString();
            final Long[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前值
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                long currentValue = 0;

                if (entry != null && !entry.isExpired()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        currentValue = ((Number) value).longValue();
                    }
                }

                // 增加值
                long newValue = currentValue + delta;
                template.updateLocalCache(cacheKey, (V) Long.valueOf(newValue));
                result[0] = newValue;
            });

            return result[0];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Double increment(Object key, double delta) {
        K typedKey = (K) key;

        // 如果未降级，尝试在Redis中递增
        if (!template.isDegraded()) {
            try {
                Double result = template.getRedisTemplate().opsForValue().increment(typedKey, delta);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis递增操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = typedKey.toString();
            final Double[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前值
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                double currentValue = 0.0;

                if (entry != null && !entry.isExpired()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        currentValue = ((Number) value).doubleValue();
                    }
                }

                // 增加值
                double newValue = currentValue + delta;
                template.updateLocalCache(cacheKey, (V) Double.valueOf(newValue));
                result[0] = newValue;
            });

            return result[0];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long decrement(Object key, long delta) {
        K typedKey = (K) key;

        // 如果未降级，尝试在Redis中递减
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForValue().decrement(typedKey, delta);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis递减操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = typedKey.toString();
            final Long[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前值
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                long currentValue = 0;

                if (entry != null && !entry.isExpired()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        currentValue = ((Number) value).longValue();
                    }
                }

                // 减少值
                long newValue = currentValue - delta;
                template.updateLocalCache(cacheKey, (V) Long.valueOf(newValue));
                result[0] = newValue;
            });

            return result[0];
        }

        return null;
    }

    @Override
    public Boolean getBit(K key, long offset) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForValue().getBit(key, offset);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis位操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存获取位图位值
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
            if (entry != null && !entry.isExpired() && entry.getValue() instanceof byte[]) {
                byte[] bitmap = (byte[]) entry.getValue();
                int byteIndex = (int) (offset / 8);
                int bitIndex = (int) (offset % 8);

                if (byteIndex < bitmap.length) {
                    return (bitmap[byteIndex] & (1 << bitIndex)) != 0;
                }
            }
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean setBit(K key, long offset, boolean value) {
        // 如果未降级，尝试在Redis中设置
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForValue().setBit(key, offset, value);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis位图操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            final Boolean[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前位图
                CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
                byte[] bitmap = null;

                if (entry != null && !entry.isExpired()) {
                    Object val = entry.getValue();
                    if (val instanceof byte[]) {
                        bitmap = (byte[]) val;
                    }
                }

                // 如果位图不存在，创建一个新的
                if (bitmap == null) {
                    int byteIndex = (int) (offset / 8);
                    bitmap = new byte[byteIndex + 1];
                }

                // 计算字节索引和位索引
                int byteIndex = (int) (offset / 8);
                int bitIndex = (int) (offset % 8);

                // 确保数组足够大
                if (byteIndex >= bitmap.length) {
                    byte[] newBitmap = new byte[byteIndex + 1];
                    System.arraycopy(bitmap, 0, newBitmap, 0, bitmap.length);
                    bitmap = newBitmap;
                }

                // 获取原来的位值
                boolean oldValue = (bitmap[byteIndex] & (1 << bitIndex)) != 0;

                // 设置新的位值
                if (value) {
                    bitmap[byteIndex] |= (1 << bitIndex);
                } else {
                    bitmap[byteIndex] &= ~(1 << bitIndex);
                }

                // 更新缓存
                template.updateLocalCache(cacheKey, (V) bitmap);

                result[0] = oldValue;
            });

            return result[0];
        }

        return null;
    }

    @Override
    public Boolean delete(K key) {
        // 如果未降级，尝试从Redis删除
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().delete(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis删除值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存删除
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            template.getLocalCache().invalidate(cacheKey);
        }

        return true; // 降级模式下默认返回成功
    }

    @Override
    public Boolean hasKey(K key) {
        // 如果未降级，尝试从Redis查询
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().hasKey(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis查询键异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：查询本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
            return entry != null && !entry.isExpired();
        }

        return false;
    }

    @Override
    public Boolean expire(K key, long timeout, TimeUnit unit) {
        // 如果未降级，尝试在Redis中设置过期时间
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().expire(key, timeout, unit);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis设置过期时间异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：更新本地缓存的过期时间
        if (template.isDegraded()) {
            String cacheKey = key.toString();
            CacheEntry<V> entry = template.getLocalCache().getIfPresent(cacheKey);
            if (entry != null && !entry.isExpired()) {
                // 创建一个新的缓存条目，使用新的过期时间
                template.updateLocalCache(cacheKey, entry.getValue(), unit.toMillis(timeout));
                return true;
            }
        }

        return false;
    }
} 