package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.DataModelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.DataModelBll;
import com.jiuyu.replay.words.bo.DataModelBo;
import com.jiuyu.replay.words.bo.DataModelListBo;
import com.jiuyu.replay.words.bo.DataModelSaveBo;
import com.jiuyu.replay.words.vo.DataModelInfoVo;
import com.jiuyu.replay.words.vo.DataModelListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 罗盘数据模型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Service
public class DataModelLogicImpl implements DataModelLogic {

    @Resource
    private DataModelBll dataModelBll;


    @Override
    public R<PageUtils<DataModelListVo>> queryPage(DataModelListBo dataModelListBo) {

        return dataModelBll.queryPage(dataModelListBo);
    }

    @Override
    public R<DataModelInfoVo> info(Long id) {

        return dataModelBll.info(id);
    }

    @Override
    public R<String> save(DataModelBo dataModelBo) {

        return dataModelBll.save(dataModelBo);
    }

    @Override
    public R<String> update(DataModelBo dataModelBo) {

        return dataModelBll.update(dataModelBo);
    }

    @Override
    public R<String> delete(Long id) {

        return dataModelBll.delete(id);
    }


    @Override
    public R<String> saveDataModel(DataModelSaveBo dataModelBo) {

        return dataModelBll.saveDataModel(dataModelBo);
    }

    @Override
    public R<PageUtils<DataModelSaveBo>> modelCruxTypeList(DataModelListBo dataModelBo) {
        return dataModelBll.modelCruxTypeList(dataModelBo);
    }

    @Override
    public R<String> updateDataModel(DataModelSaveBo dataModelBo) {
        return dataModelBll.updateDataModel(dataModelBo);
    }

    @Override
    public R<String> deleteDataModel(Long id) {
        return dataModelBll.deleteDataModel(id);
    }

    @Override
    public R<DataModelSaveBo> infoDataModel(Long id) {
        return dataModelBll.infoDataModel(id);
    }

    @Override
    public R<List<DataModelInfoVo>> listTradeModel(String uuid, Integer type) {

        return dataModelBll.listTradeModel(uuid, type);
    }

}

