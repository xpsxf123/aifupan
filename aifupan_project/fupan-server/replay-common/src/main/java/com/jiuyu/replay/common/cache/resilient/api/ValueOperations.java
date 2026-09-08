package com.jiuyu.replay.common.cache.resilient.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis降级包装类的值操作接口
 *
 * @author RayChou
 * @date 2025/7/7
 */
public interface ValueOperations<K, V> {

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    V get(Object key);

    /**
     * 获取值，支持回调函数
     *
     * @param key      键
     * @param fallback 回调函数，在缓存中不存在时调用
     * @return 值
     */
    V get(K key, Supplier<V> fallback);

    /**
     * 设置值
     *
     * @param key   键
     * @param value 值
     */
    void set(Object key, Object value);

    /**
     * 设置值，支持过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void set(Object key, Object value, long timeout, TimeUnit unit);

    /**
     * 递增整数值
     *
     * @param key   键
     * @param delta 增量
     * @return 增加后的值
     */
    Long increment(Object key, long delta);

    /**
     * 递增浮点值
     *
     * @param key   键
     * @param delta 增量
     * @return 增加后的值
     */
    Double increment(Object key, double delta);

    /**
     * 递减操作
     *
     * @param key   键
     * @param delta 减量
     * @return 减少后的值
     */
    Long decrement(Object key, long delta);

    /**
     * 批量设置值
     *
     * @param map 键值对映射
     */
    void multiSet(Map<? extends K, ? extends V> map);

    /**
     * 批量获取键值对
     *
     * @param keys 键集合
     * @return 值列表
     */
    List<V> multiGet(Collection<K> keys);

    /**
     * 设置键的值，仅当键不存在时
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    Boolean setIfAbsent(K key, V value);

    /**
     * 设置键的值，仅当键不存在时，支持过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    Boolean setIfAbsent(K key, V value, long timeout, TimeUnit unit);

    /**
     * 设置键的值，仅当键已存在时
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    Boolean setIfPresent(K key, V value);

    /**
     * 设置键的值，仅当键已存在时，支持过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    Boolean setIfPresent(K key, V value, long timeout, TimeUnit unit);

    /**
     * 获取位图中指定位置的位值
     *
     * @param key    键
     * @param offset 偏移量
     * @return 位值
     */
    Boolean getBit(K key, long offset);

    /**
     * 设置键的位图的位值
     *
     * @param key    键
     * @param offset 偏移量
     * @param value  值
     * @return 原来的位值
     */
    Boolean setBit(K key, long offset, boolean value);

    /**
     * 删除值
     *
     * @param key 键
     * @return 是否成功
     */
    Boolean delete(K key);

    /**
     * 判断是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    Boolean hasKey(K key);

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    Boolean expire(K key, long timeout, TimeUnit unit);
} 