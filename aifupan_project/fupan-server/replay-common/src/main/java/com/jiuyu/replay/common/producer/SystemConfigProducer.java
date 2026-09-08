package com.jiuyu.replay.common.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.common.vo.SystemConfigListVo;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.bo.SystemConfigBo;
import com.jiuyu.replay.common.bo.SystemConfigListBo;


/**
 * 系统配置
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
public interface SystemConfigProducer {


    /**
     * 系统配置列表
     * @param systemConfigListBo 系统配置列表查询参数
     * @return
     */
    PageUtils<SystemConfigListVo> queryPage(SystemConfigListBo systemConfigListBo);

    /**
    * 系统配置信息
    * @param id 系统配置id
    * @return
    */
    SystemConfigInfoVo info(Long id);

    /**
     * 新增系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
     SystemConfigInfoVo save(SystemConfigBo systemConfigBo);

    /**
     * 修改系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    void update(SystemConfigBo systemConfigBo);

    /**
     * 删除系统配置
     * @param id 系统配置id
     * @return
     */
    void deleteById(Long id);


}

