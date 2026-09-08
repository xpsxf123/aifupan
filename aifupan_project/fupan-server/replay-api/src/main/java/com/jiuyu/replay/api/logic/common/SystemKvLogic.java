package com.jiuyu.replay.api.logic.common;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.bo.ImgConfigBo;

import com.jiuyu.replay.common.vo.ImgConfigVo;
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
public interface SystemKvLogic {


    /**
     * 系统配置的键值对列表
     * @param systemKvListBo 系统配置的键值对列表查询参数
     * @return
     */
    R<PageUtils<SystemKvListVo>> queryPage(SystemKvListBo systemKvListBo);

    /**
    * 系统配置的键值对信息
    * @param id 系统配置的键值对id
    * @return
    */
    R<SystemKvInfoVo> info(Long id);

    /**
     * 新增系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    R<String> save(SystemKvBo systemKvBo);

    /**
     * 修改系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    R<String> update(SystemKvBo systemKvBo);

    /**
     * 删除系统配置的键值对
     * @param id 系统配置的键值对id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 修改图片配置
     * @param imgConfigBo 图片配置信息
     * @return
     */
    R<String> updateImgConfig(ImgConfigBo imgConfigBo);

    /**
     * 获取图片配置
     * @return
     */
    R<ImgConfigVo> getImgConfig();

    /**
     * 根据key获取系统配置的键值对信息
     *
     * @param key 系统配置的键值对key
     * @return
     */
    R<SystemKvInfoVo> getByKey(String key);
}

