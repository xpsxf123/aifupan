package com.jiuyu.replay.api.logic.system.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.system.vo.DictTypeListVo;
import com.jiuyu.replay.system.vo.DictTypeInfoVo;
import com.jiuyu.replay.system.bo.DictTypeBo;
import com.jiuyu.replay.system.bo.DictTypeListBo;
import com.jiuyu.replay.api.logic.system.DictTypeLogic;
import com.jiuyu.replay.system.bll.DictTypeBll;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;


/**
 * 字典类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Service
public class DictTypeLogicImpl implements DictTypeLogic {

    @Resource
    private DictTypeBll dictTypeBll;


    @Override
    public R<PageUtils<DictTypeListVo>> queryPage(DictTypeListBo dictTypeListBo) {

        return dictTypeBll.queryPage(dictTypeListBo);
    }

    @Override
    public R<DictTypeInfoVo> info(Long id) {

        return dictTypeBll.info(id);
    }

    @Override
    public R<String> save(DictTypeBo dictTypeBo) {

        return dictTypeBll.save(dictTypeBo);
    }

    @Override
    public R<String> update(DictTypeBo dictTypeBo) {

        return dictTypeBll.update(dictTypeBo);
    }

    @Override
    public R<String> delete(Long id) {

        return dictTypeBll.delete(id);
    }


}

