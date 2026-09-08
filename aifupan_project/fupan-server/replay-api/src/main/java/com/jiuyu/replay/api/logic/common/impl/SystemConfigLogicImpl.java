package com.jiuyu.replay.api.logic.common.impl;

import com.jiuyu.replay.api.logic.common.SystemConfigLogic;
import com.jiuyu.replay.common.bll.SystemConfigBll;
import com.jiuyu.replay.common.bo.SystemConfigBo;
import com.jiuyu.replay.common.bo.SystemConfigListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.vo.SystemConfigListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 系统配置
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
@Service
public class SystemConfigLogicImpl implements SystemConfigLogic {

    @Resource
    private SystemConfigBll systemConfigBll;


    @Override
    public R<PageUtils<SystemConfigListVo>> queryPage(SystemConfigListBo systemConfigListBo) {

        return systemConfigBll.queryPage(systemConfigListBo);
    }

    @Override
    public R<SystemConfigInfoVo> info(Long id) {

        return systemConfigBll.info(id);
    }

    @Override
    public R<String> save(SystemConfigBo systemConfigBo) {

        return systemConfigBll.save(systemConfigBo);
    }

    @Override
    public R<String> update(SystemConfigBo systemConfigBo) {

        return systemConfigBll.update(systemConfigBo);
    }

    @Override
    public R<String> delete(Long id) {

        return systemConfigBll.delete(id);
    }


}

