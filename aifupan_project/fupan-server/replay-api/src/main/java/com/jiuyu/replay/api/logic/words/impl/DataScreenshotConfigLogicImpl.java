package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.DataScreenshotConfigLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.DataScreenshotConfigListVo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigBo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigListBo;
import com.jiuyu.replay.words.bll.DataScreenshotConfigBll;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;


/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Service
public class DataScreenshotConfigLogicImpl implements DataScreenshotConfigLogic {

    @Resource
    private DataScreenshotConfigBll dataScreenshotConfigBll;


    @Override
    public R<PageUtils<DataScreenshotConfigListVo>> queryPage(DataScreenshotConfigListBo dataScreenshotConfigListBo) {

        return dataScreenshotConfigBll.queryPage(dataScreenshotConfigListBo);
    }

    @Override
    public R<DataScreenshotConfigInfoVo> info(Long id) {

        return dataScreenshotConfigBll.info(id);
    }

    @Override
    public R<String> save(DataScreenshotConfigBo dataScreenshotConfigBo) {

        return dataScreenshotConfigBll.save(dataScreenshotConfigBo);
    }

    @Override
    public R<String> update(DataScreenshotConfigBo dataScreenshotConfigBo) {

        return dataScreenshotConfigBll.update(dataScreenshotConfigBo);
    }

    @Override
    public R<String> delete(Long id) {

        return dataScreenshotConfigBll.delete(id);
    }


}

