package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.common.vo.SystemConfigListVo;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.bo.SystemConfigBo;
import com.jiuyu.replay.common.bo.SystemConfigListBo;
import com.jiuyu.replay.common.producer.SystemConfigProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 系统配置
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
@Component
public class SystemConfigBll {

    @Resource
    private SystemConfigProducer systemConfigProducer;


    /**
     * 系统配置列表
     * @param systemConfigListBo 系统配置列表查询参数
     * @return
     */
    public R<PageUtils<SystemConfigListVo>> queryPage(SystemConfigListBo systemConfigListBo) {

        return R.ok("获取成功", systemConfigProducer.queryPage(systemConfigListBo));
    }

    /**
    * 系统配置信息
    * @param id 系统配置id
    * @return
    */
    public R<SystemConfigInfoVo> info(Long id) {

        SystemConfigInfoVo systemConfigInfoVo = systemConfigProducer.info(id);
        return R.ok("获取成功", systemConfigInfoVo);
    }

    /**
     * 新增系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    public R<String> save(SystemConfigBo systemConfigBo) {

        SystemConfigInfoVo systemConfigInfoVo = systemConfigProducer.save(systemConfigBo);
        return R.ok("添加成功");
    }

    /**
     * 修改系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    public R<String> update(SystemConfigBo systemConfigBo) {

        systemConfigProducer.update(systemConfigBo);
        return R.ok("修改成功");
    }

    /**
     * 删除系统配置
     * @param id 系统配置id
     * @return
     */
    public R<String> delete(Long id) {

        systemConfigProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

