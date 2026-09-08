package com.jiuyu.replay.api.logic.common;

import com.jiuyu.replay.common.bo.SystemConfigBo;
import com.jiuyu.replay.common.bo.SystemConfigListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.vo.SystemConfigListVo;


/**
 * 系统配置
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
public interface SystemConfigLogic {


    /**
     * 系统配置列表
     * @param systemConfigListBo 系统配置列表查询参数
     * @return
     */
    R<PageUtils<SystemConfigListVo>> queryPage(SystemConfigListBo systemConfigListBo);

    /**
    * 系统配置信息
    * @param id 系统配置id
    * @return
    */
    R<SystemConfigInfoVo> info(Long id);

    /**
     * 新增系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    R<String> save(SystemConfigBo systemConfigBo);

    /**
     * 修改系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    R<String> update(SystemConfigBo systemConfigBo);

    /**
     * 删除系统配置
     * @param id 系统配置id
     * @return
     */
    R<String> delete(Long id);


}

