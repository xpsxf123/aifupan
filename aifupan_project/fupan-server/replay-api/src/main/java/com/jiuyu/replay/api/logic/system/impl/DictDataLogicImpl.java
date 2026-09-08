package com.jiuyu.replay.api.logic.system.impl;

import com.jiuyu.replay.api.logic.system.DictDataLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.system.bo.DictDataBo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.system.vo.DictDataInfoVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Service
public class DictDataLogicImpl implements DictDataLogic {

    @Resource
    private DictDataBll dictDataBll;


    @Override
    public R<PageUtils<DictDataListVo>> queryPage(DictDataListBo dictDataListBo) {

        R<PageUtils<DictDataListVo>> pageUtilsR = dictDataBll.queryPage(dictDataListBo);

        return pageUtilsR;
    }

    @Override
    public R<DictDataInfoVo> info(Long id) {

        return dictDataBll.info(id);
    }

    @Override
    public R<String> save(DictDataBo dictDataBo) {

        return dictDataBll.save(dictDataBo);
    }

    @Override
    public R<String> update(DictDataBo dictDataBo) {

        return dictDataBll.update(dictDataBo);
    }

    @Override
    public R<String> delete(Long id) {

        return dictDataBll.delete(id);
    }


}

