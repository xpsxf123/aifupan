package com.jiuyu.replay.common.cache.resilient.impl;

import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate.CacheEntry;
import com.jiuyu.replay.common.cache.resilient.api.HashOperations;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis降级包装类的哈希操作实现类
 *
 * @author RayChou
 * @date 2025/7/7
 */
@Slf4j
public class ResilientHashOperations<K, HK, HV> implements HashOperations<K, HK, HV> {

    private final ResilientRedisTemplate<K, ?> template;

    public ResilientHashOperations(ResilientRedisTemplate<K, ?> template) {
        this.template = template;
    }

    @Override
    @SuppressWarnings("unchecked")
    public HV get(K key, Object field) {
        return get(key, field, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public HV get(K key, Object field, Supplier<HV> fallback) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                HV value = (HV) template.getRedisTemplate().opsForHash().get(key, field);
                template.resetFailures();

                // 如果Redis中没有值，使用回调获取
                if (value == null && fallback != null) {
                    value = fallback.get();
                    if (value != null) {
                        template.getRedisTemplate().opsForHash().put(key, field, value);
                    }
                }

                return value;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString() + ":" + field.toString();
            // 读操作不需要加锁，因为CaffeinCache是线程安全的
            CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
            if (entry != null && !entry.isExpired()) {
                return (HV) entry.getValue();
            }

            // 本地缓存也没有，使用回调函数
            if (fallback != null) {
                HV value = fallback.get();
                if (value != null) {
                    updateHashField(key, field, value);
                }
                return value;
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(K key, HK field, HV value) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                template.getRedisTemplate().opsForHash().put(key, field, value);
                template.resetFailures();
                return;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：写入本地缓存
        if (template.isDegraded() && value != null) {
            updateHashField(key, field, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long delete(K key, Object... fields) {
        // 如果未降级，尝试从Redis删除
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForHash().delete(key, fields);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash删除字段异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：从本地缓存删除
        if (template.isDegraded()) {
            String fieldsKey = "_fields:" + key.toString();
            final long[] deletedCount = {0};

            template.getHybridLockService().executeWithLock(template.getLockKey(fieldsKey), 5, () -> {
                // 删除字段值
                for (Object field : fields) {
                    String cacheKey = key.toString() + ":" + field.toString();

                    template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                        if (template.getLocalCache().getIfPresent(cacheKey) != null) {
                            template.getLocalCache().invalidate(cacheKey);
                            deletedCount[0]++;
                        }
                    });

                    // 从字段索引中移除
                    CacheEntry<?> fieldsEntry = (CacheEntry<?>) template.getLocalCache().getIfPresent(fieldsKey);
                    if (fieldsEntry != null && !fieldsEntry.isExpired()) {
                        Set<Object> fieldSet = (Set<Object>) fieldsEntry.getValue();
                        if (fieldSet != null) {
                            fieldSet.remove(field);
                            // 更新字段索引
                            template.updateLocalCache(fieldsKey, fieldSet);
                        }
                    }
                }
            });

            return deletedCount[0];
        }

        return 0L;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean hasKey(K key, Object field) {
        // 如果未降级，尝试从Redis查询
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForHash().hasKey(key, field);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            String cacheKey = key.toString() + ":" + field.toString();
            // 读操作不需要加锁，因为CaffeinCache是线程安全的
            CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
            return entry != null && !entry.isExpired();
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<HK, HV> entries(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Map<Object, Object> result = template.getRedisTemplate().opsForHash().entries(key);
                Map<HK, HV> typedResult = new HashMap<>();
                result.forEach((k, v) -> typedResult.put((HK) k, (HV) v));
                template.resetFailures();
                return typedResult;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            Map<HK, HV> result = new HashMap<>();
            String fieldsKey = "_fields:" + key.toString();

            // 获取字段集合
            CacheEntry<?> fieldsEntry = (CacheEntry<?>) template.getLocalCache().getIfPresent(fieldsKey);
            if (fieldsEntry != null && !fieldsEntry.isExpired()) {
                Set<Object> fields = (Set<Object>) fieldsEntry.getValue();

                // 获取每个字段的值
                for (Object field : fields) {
                    String cacheKey = key.toString() + ":" + field.toString();
                    CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
                    if (entry != null && !entry.isExpired()) {
                        result.put((HK) field, (HV) entry.getValue());
                    }
                }
            }

            return result;
        }

        return new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long increment(K key, HK field, long delta) {
        // 如果未降级，尝试在Redis中增加
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForHash().increment(key, field, delta);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash增加值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString() + ":" + field.toString();
            final Long[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前值
                CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
                long currentValue = 0;

                if (entry != null && !entry.isExpired()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        currentValue = ((Number) value).longValue();
                    }
                }

                // 增加值
                long newValue = currentValue + delta;
                Object typedValue = Long.valueOf(newValue);

                // 更新缓存
                updateHashField(key, field, (HV) typedValue);
                result[0] = newValue;
            });

            return result[0];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Double increment(K key, HK field, double delta) {
        // 如果未降级，尝试在Redis中增加
        if (!template.isDegraded()) {
            try {
                Double result = template.getRedisTemplate().opsForHash().increment(key, field, delta);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash增加值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString() + ":" + field.toString();
            final Double[] result = {null};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 获取当前值
                CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
                double currentValue = 0;

                if (entry != null && !entry.isExpired()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        currentValue = ((Number) value).doubleValue();
                    }
                }

                // 增加值
                double newValue = currentValue + delta;
                Object typedValue = Double.valueOf(newValue);

                // 更新缓存
                updateHashField(key, field, (HV) typedValue);
                result[0] = newValue;
            });

            return result[0];
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<HK> keys(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Set<Object> result = template.getRedisTemplate().opsForHash().keys(key);
                Set<HK> typedResult = new HashSet<>();
                result.forEach(k -> typedResult.add((HK) k));
                template.resetFailures();
                return typedResult;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            String fieldsKey = "_fields:" + key.toString();
            Set<HK> result = new HashSet<>();

            // 获取字段集合
            CacheEntry<?> fieldsEntry = (CacheEntry<?>) template.getLocalCache().getIfPresent(fieldsKey);
            if (fieldsEntry != null && !fieldsEntry.isExpired()) {
                Set<Object> fields = (Set<Object>) fieldsEntry.getValue();
                fields.forEach(field -> result.add((HK) field));
            }

            return result;
        }

        return new HashSet<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long size(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                Long result = template.getRedisTemplate().opsForHash().size(key);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            String fieldsKey = "_fields:" + key.toString();

            // 获取字段集合
            CacheEntry<?> fieldsEntry = (CacheEntry<?>) template.getLocalCache().getIfPresent(fieldsKey);
            if (fieldsEntry != null && !fieldsEntry.isExpired()) {
                Set<Object> fields = (Set<Object>) fieldsEntry.getValue();
                return (long) fields.size();
            }
        }

        return 0L;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HV> multiGet(K key, Collection<HK> fields) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                List<Object> result = template.getRedisTemplate().opsForHash().multiGet(key, (Collection<Object>) fields);
                List<HV> typedResult = new ArrayList<>();
                result.forEach(v -> typedResult.add((HV) v));
                template.resetFailures();
                return typedResult;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            List<HV> result = new ArrayList<>();

            for (HK field : fields) {
                String cacheKey = key.toString() + ":" + field.toString();
                CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
                if (entry != null && !entry.isExpired()) {
                    result.add((HV) entry.getValue());
                } else {
                    result.add(null);
                }
            }

            return result;
        }

        return new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(K key, Map<? extends HK, ? extends HV> m) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                template.getRedisTemplate().opsForHash().putAll(key, m);
                template.resetFailures();
                return;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：写入本地缓存
        if (template.isDegraded()) {
            m.forEach((field, value) -> {
                if (value != null) {
                    updateHashField(key, field, value);
                }
            });
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean putIfAbsent(K key, HK field, HV value) {
        // 如果未降级，尝试写入Redis
        if (!template.isDegraded()) {
            try {
                Boolean result = template.getRedisTemplate().opsForHash().putIfAbsent(key, field, value);
                template.resetFailures();
                return result;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash设置值异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式下使用HybridLockService保证JVM级别的原子性
        if (template.isDegraded()) {
            String cacheKey = key.toString() + ":" + field.toString();
            final boolean[] success = {false};

            template.getHybridLockService().executeWithLock(template.getLockKey(cacheKey), 5, () -> {
                // 检查键是否存在
                CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
                if (entry != null && !entry.isExpired()) {
                    // 键已存在，返回失败
                    success[0] = false;
                    return;
                }

                // 键不存在，设置值
                updateHashField(key, field, value);
                success[0] = true;
            });

            return success[0];
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HV> values(K key) {
        // 如果未降级，尝试从Redis获取
        if (!template.isDegraded()) {
            try {
                List<Object> result = template.getRedisTemplate().opsForHash().values(key);
                List<HV> typedResult = new ArrayList<>();
                result.forEach(v -> typedResult.add((HV) v));
                template.resetFailures();
                return typedResult;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis Hash操作异常: {}", e.getMessage());
                template.recordFailure();
            }
        }

        // 降级模式：使用本地缓存
        if (template.isDegraded()) {
            List<HV> result = new ArrayList<>();
            String fieldsKey = "_fields:" + key.toString();

            // 获取字段集合
            CacheEntry<?> fieldsEntry = (CacheEntry<?>) template.getLocalCache().getIfPresent(fieldsKey);
            if (fieldsEntry != null && !fieldsEntry.isExpired()) {
                Set<Object> fields = (Set<Object>) fieldsEntry.getValue();

                // 获取每个字段的值
                for (Object field : fields) {
                    String cacheKey = key.toString() + ":" + field.toString();
                    CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(cacheKey);
                    if (entry != null && !entry.isExpired()) {
                        result.add((HV) entry.getValue());
                    }
                }
            }

            return result;
        }

        return new ArrayList<>();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void updateHashField(K key, Object field, HV value) {
        String cacheKey = key.toString() + ":" + field.toString();
        ((ResilientRedisTemplate) template).updateLocalCache(cacheKey, value);

        // 更新字段索引
        updateHashFieldIndex(key.toString(), field);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void updateHashFieldIndex(String key, Object field) {
        String fieldsKey = "_fields:" + key;

        template.getHybridLockService().executeWithLock(template.getLockKey(fieldsKey), 5, () -> {
            Set<Object> fields = new HashSet<>();

            // 获取现有的字段集合
            CacheEntry<?> entry = (CacheEntry<?>) template.getLocalCache().getIfPresent(fieldsKey);
            if (entry != null && !entry.isExpired()) {
                fields = (Set<Object>) entry.getValue();
            }

            // 添加新字段
            fields.add(field);

            // 更新字段集合，使用Object类型参数
            ((ResilientRedisTemplate) template).updateLocalCache(fieldsKey, fields);
        });
    }
} 