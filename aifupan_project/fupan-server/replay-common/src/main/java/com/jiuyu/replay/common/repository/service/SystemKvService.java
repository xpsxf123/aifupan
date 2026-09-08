package com.jiuyu.replay.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.common.entity.SystemKvEntity;

/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
public interface SystemKvService extends IService<SystemKvEntity> {

    /**
     * 根据key查询
     *
     * @param key key
     * @return 值
     */
    SystemKvEntity getByKey(String key);

    /**
     * 根据key查询
     *
     * @param key          key
     * @param defaultValue 默认值
     * @return 值
     */
    String getValueByKey(String key, String defaultValue);

    /**
     * 根据key查询
     *
     * @param key          key
     * @param defaultValue 默认值
     * @return 值
     */
    Integer getValueByKey(String key, Integer defaultValue);

    /**
     * 根据key查询
     *
     * @param key          key
     * @param defaultValue 默认值
     * @return 值
     */
    Long getValueByKey(String key, Long defaultValue);
}

