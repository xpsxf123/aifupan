package com.jiuyu.replay.common.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.common.vo.SystemKvListVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.common.bo.SystemKvBo;
import com.jiuyu.replay.common.bo.SystemKvListBo;


/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
public interface SystemKvProducer {


    /**
     * 系统配置的键值对列表
     * @param systemKvListBo 系统配置的键值对列表查询参数
     * @return
     */
    PageUtils<SystemKvListVo> queryPage(SystemKvListBo systemKvListBo);

    /**
    * 系统配置的键值对信息
    * @param id 系统配置的键值对id
    * @return
    */
    SystemKvInfoVo info(Long id);

    /**
     * 新增系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
     SystemKvInfoVo save(SystemKvBo systemKvBo);

    /**
     * 修改系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    void update(SystemKvBo systemKvBo);

    /**
     * 删除系统配置的键值对
     * @param id 系统配置的键值对id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据key查询
     * @param key
     * @return
     */
    SystemKvInfoVo getByKey(String key);

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

