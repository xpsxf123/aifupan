package com.jiuyu.replay.common.cache.resilient.api;

import java.util.Set;

/**
 * Redis降级包装类的有序集合操作接口
 *
 * @author RayChou
 * @date 2025/7/7
 */
public interface ZSetOperations<K, V> {

    /**
     * 向有序集合添加元素，可指定分数
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return 是否成功添加（添加新元素，而不是更新分数）
     */
    Boolean add(K key, V value, double score);

    /**
     * 获取有序集合中元素的分数
     *
     * @param key   键
     * @param value 值
     * @return 分数，如果元素不存在则返回null
     */
    Double score(K key, V value);

    /**
     * 增加有序集合中元素的分数
     *
     * @param key   键
     * @param value 值
     * @param delta 增量
     * @return 增加后的分数
     */
    Double incrementScore(K key, V value, double delta);

    /**
     * 获取有序集合中指定范围的元素（按分数排序）
     *
     * @param key   键
     * @param start 起始索引
     * @param end   结束索引
     * @return 元素集合
     */
    Set<V> range(K key, long start, long end);

    /**
     * 从有序集合中移除元素
     *
     * @param key    键
     * @param values 要移除的元素
     * @return 成功移除的元素数量
     */
    Long remove(K key, Object... values);

    /**
     * 获取有序集合的大小
     *
     * @param key 键
     * @return 元素数量
     */
    Long size(K key);
} 