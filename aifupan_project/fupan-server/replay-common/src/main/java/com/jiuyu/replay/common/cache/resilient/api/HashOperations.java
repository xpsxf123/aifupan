package com.jiuyu.replay.common.cache.resilient.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Redis降级包装类的哈希操作接口
 *
 * @author RayChou
 * @date 2025/7/7
 */
public interface HashOperations<K, HK, HV> {

    /**
     * 获取Hash中的字段值
     *
     * @param key   键
     * @param field 字段
     * @return 值
     */
    HV get(K key, Object field);

    /**
     * 获取Hash中的字段值，支持回调函数
     *
     * @param key      键
     * @param field    字段
     * @param fallback 回调函数，在缓存中不存在时调用
     * @return 值
     */
    HV get(K key, Object field, Supplier<HV> fallback);

    /**
     * 设置Hash中的字段值
     *
     * @param key   键
     * @param field 字段
     * @param value 值
     */
    void put(K key, HK field, HV value);

    /**
     * 删除Hash中的字段
     *
     * @param key    键
     * @param fields 字段
     * @return 删除的字段数量
     */
    Long delete(K key, Object... fields);

    /**
     * 判断Hash中是否存在字段
     *
     * @param key   键
     * @param field 字段
     * @return 是否存在
     */
    Boolean hasKey(K key, Object field);

    /**
     * 获取Hash的所有字段和值
     *
     * @param key 键
     * @return 字段和值的映射
     */
    Map<HK, HV> entries(K key);

    /**
     * 获取Hash的所有键
     *
     * @param key 键
     * @return 字段集合
     */
    Set<HK> keys(K key);

    /**
     * 获取Hash的大小
     *
     * @param key 键
     * @return 字段数量
     */
    Long size(K key);

    /**
     * 将Hash中字段的整数值增加指定的增量
     *
     * @param key   键
     * @param field 字段
     * @param delta 增量
     * @return 增加后的值
     */
    Long increment(K key, HK field, long delta);

    /**
     * 将Hash中字段的浮点值增加指定的增量
     *
     * @param key   键
     * @param field 字段
     * @param delta 增量
     * @return 增加后的值
     */
    Double increment(K key, HK field, double delta);

    /**
     * 批量获取Hash中多个字段的值
     *
     * @param key    键
     * @param fields 字段集合
     * @return 值列表
     */
    List<HV> multiGet(K key, Collection<HK> fields);

    /**
     * 批量设置Hash中多个字段的值
     *
     * @param key 键
     * @param m   字段值映射
     */
    void putAll(K key, Map<? extends HK, ? extends HV> m);

    /**
     * 当字段不存在时设置值
     *
     * @param key   键
     * @param field 字段
     * @param value 值
     * @return 是否设置成功
     */
    Boolean putIfAbsent(K key, HK field, HV value);

    /**
     * 获取Hash的所有值
     *
     * @param key 键
     * @return 值集合
     */
    List<HV> values(K key);
} 