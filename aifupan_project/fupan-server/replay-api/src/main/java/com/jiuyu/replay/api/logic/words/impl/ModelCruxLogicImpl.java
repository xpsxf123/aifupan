package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.ModelCruxLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.ModelCruxBll;
import com.jiuyu.replay.words.bo.ModelCruxBo;
import com.jiuyu.replay.words.bo.ModelCruxListBo;
import com.jiuyu.replay.words.vo.ModelCruxInfoVo;
import com.jiuyu.replay.words.vo.ModelCruxListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 模型-关键词类型-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Service
public class ModelCruxLogicImpl implements ModelCruxLogic {

    @Resource
    private ModelCruxBll modelCruxBll;


    @Override
    public R<PageUtils<ModelCruxListVo>> queryPage(ModelCruxListBo modelCruxListBo) {

        return modelCruxBll.queryPage(modelCruxListBo);
    }

    @Override
    public R<ModelCruxInfoVo> info(Long id) {

        return modelCruxBll.info(id);
    }

    @Override
    public R<String> save(ModelCruxBo modelCruxBo) {

        return modelCruxBll.save(modelCruxBo);
    }

    @Override
    public R<String> update(ModelCruxBo modelCruxBo) {

        return modelCruxBll.update(modelCruxBo);
    }

    @Override
    public R<String> delete(Long id) {

        return modelCruxBll.delete(id);
    }


}

