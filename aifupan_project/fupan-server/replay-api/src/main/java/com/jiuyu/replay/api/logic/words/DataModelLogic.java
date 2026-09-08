package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.DataModelBo;
import com.jiuyu.replay.words.bo.DataModelListBo;
import com.jiuyu.replay.words.bo.DataModelSaveBo;
import com.jiuyu.replay.words.vo.DataModelInfoVo;
import com.jiuyu.replay.words.vo.DataModelListVo;

import java.util.List;


/**
 * 罗盘数据模型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
public interface DataModelLogic {


    /**
     * 罗盘数据模型列表
     * @param dataModelListBo 罗盘数据模型列表查询参数
     * @return
     */
    R<PageUtils<DataModelListVo>> queryPage(DataModelListBo dataModelListBo);

    /**
    * 罗盘数据模型信息
    * @param id 罗盘数据模型id
    * @return
    */
    R<DataModelInfoVo> info(Long id);

    /**
     * 新增罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    R<String> save(DataModelBo dataModelBo);

    /**
     * 修改罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    R<String> update(DataModelBo dataModelBo);

    /**
     * 删除罗盘数据模型
     * @param id 罗盘数据模型id
     * @return
     */
    R<String> delete(Long id);


    R<String> saveDataModel(DataModelSaveBo dataModelBo);

    R<PageUtils<DataModelSaveBo>> modelCruxTypeList(DataModelListBo dataModelBo);

    R<String> updateDataModel(DataModelSaveBo dataModelBo);

    R<String> deleteDataModel(Long id);

    R<DataModelSaveBo> infoDataModel(Long id);

    /**
     * 根据视频id或对比id获取模型列表
     * @param uuid 视频id或对比id
     * @param type 类型 0：视频 1：对比 2：文件
     * @return
     */
    R<List<DataModelInfoVo>> listTradeModel(String uuid, Integer type);
}

