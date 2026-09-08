package com.jiuyu.replay.api.logic.common.impl;

import com.jiuyu.replay.api.logic.common.SystemKvLogic;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.bo.ImgConfigBo;
import com.jiuyu.replay.common.bo.SystemKvBo;
import com.jiuyu.replay.common.bo.SystemKvListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.ImgConfigVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.common.vo.SystemKvListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Service
public class SystemKvLogicImpl implements SystemKvLogic {

    @Resource
    private SystemKvBll systemKvBll;


    @Override
    public R<PageUtils<SystemKvListVo>> queryPage(SystemKvListBo systemKvListBo) {

        return systemKvBll.queryPage(systemKvListBo);
    }

    @Override
    public R<SystemKvInfoVo> info(Long id) {

        return systemKvBll.info(id);
    }

    @Override
    public R<String> save(SystemKvBo systemKvBo) {

        return systemKvBll.save(systemKvBo);
    }

    @Override
    public R<String> update(SystemKvBo systemKvBo) {

        return systemKvBll.update(systemKvBo);
    }

    @Override
    public R<String> delete(Long id) {

        return systemKvBll.delete(id);
    }

    @Override
    public R<String> updateImgConfig(ImgConfigBo imgConfigBo) {

        return systemKvBll.updateImgConfig(imgConfigBo);
    }

    @Override
    public R<ImgConfigVo> getImgConfig() {

        return systemKvBll.getImgConfig();
    }

    @Override
    public R<SystemKvInfoVo> getByKey(String key) {
        return systemKvBll.getByKey(key);
    }
}

