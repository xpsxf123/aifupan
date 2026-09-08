package com.jiuyu.replay.common.cache.resilient.api;

import java.util.List;

/**
 * Redis降级包装类的列表操作接口
 *
 * @author RayChou
 * @date 2025/7/7
 */
public interface ListOperations<K, V> {

    /**
     * 获取列表中指定索引位置的元素
     *
     * @param key   键
     * @param index 索引
     * @return 元素
     */
    V index(K key, long index);

    /**
     * 获取列表的指定范围的元素
     *
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素列表
     */
    List<V> range(K key, long start, long end);

    /**
     * 从列表左侧添加元素
     *
     * @param key   键
     * @param value 值
     * @return 添加后列表的长度
     */
    Long leftPush(K key, V value);

    /**
     * 从列表右侧添加元素
     *
     * @param key   键
     * @param value 值
     * @return 添加后列表的长度
     */
    Long rightPush(K key, V value);

    /**
     * 从列表左侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素
     */
    V leftPop(K key);

    /**
     * 从列表右侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素
     */
    V rightPop(K key);

    /**
     * 获取列表的长度
     *
     * @param key 键
     * @return 列表长度
     */
    Long size(K key);
} 