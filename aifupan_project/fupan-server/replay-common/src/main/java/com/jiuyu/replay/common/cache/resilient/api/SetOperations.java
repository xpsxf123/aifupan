package com.jiuyu.replay.common.cache.resilient.api;

import java.util.Set;

/**
 * Redis降级包装类的集合操作接口
 *
 * @author RayChou
 * @date 2025/7/7
 */
public interface SetOperations<K, V> {

    /**
     * 将元素添加到Set
     *
     * @param key    键
     * @param values 值
     * @return 添加的元素数量
     */
    Long add(K key, V... values);

    /**
     * 获取Set中的所有元素
     *
     * @param key 键
     * @return 元素集合
     */
    Set<V> members(K key);

    /**
     * 判断元素是否在Set中
     *
     * @param key   键
     * @param value 值
     * @return 是否存在
     */
    Boolean isMember(K key, V value);

    /**
     * 从Set中移除元素
     *
     * @param key    键
     * @param values 值
     * @return 移除的元素数量
     */
    Long remove(K key, Object... values);

    /**
     * 获取Set的大小
     *
     * @param key 键
     * @return 元素数量
     */
    Long size(K key);

    /**
     * 随机获取Set中的一个元素
     *
     * @param key 键
     * @return 随机元素
     */
    V randomMember(K key);
}
