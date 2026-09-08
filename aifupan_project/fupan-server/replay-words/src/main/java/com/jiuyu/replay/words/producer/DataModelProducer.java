package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.DataModelBo;
import com.jiuyu.replay.words.bo.DataModelListBo;
import com.jiuyu.replay.words.bo.DataModelSaveBo;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.entity.DataModelEntity;
import com.jiuyu.replay.words.vo.DataModelInfoVo;
import com.jiuyu.replay.words.vo.DataModelListVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;

import java.util.Collection;
import java.util.List;


/**
 * 罗盘数据模型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
public interface DataModelProducer {


    /**
     * 罗盘数据模型列表
     * @param dataModelListBo 罗盘数据模型列表查询参数
     * @return
     */
    PageUtils<DataModelListVo> queryPage(DataModelListBo dataModelListBo);

    /**
    * 罗盘数据模型信息
    * @param id 罗盘数据模型id
    * @return
    */
    DataModelInfoVo info(Long id);

    /**
     * 新增罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
     DataModelInfoVo save(DataModelBo dataModelBo);

    /**
     * 修改罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    void update(DataModelBo dataModelBo);

    /**
     * 删除罗盘数据模型
     * @param id 罗盘数据模型id
     * @return
     */
    void deleteById(Long id);


    R<String> saveDataModel(DataModelSaveBo dataModelBo);

    R<PageUtils<DataModelSaveBo>> modelCruxTypeList(DataModelListBo dataModelBo);

    R<String> updateDataModel(DataModelSaveBo dataModelBo);

    R<String> deleteDataModel(Long id);

    R<DataModelSaveBo> infoDataModel(Long id);

    DataModelEntity saveModel(TradeInfoVo tradeBo);

    void updateModel(TradeBo tradeBo);

    DataModelSaveBo infoModel(Long id);

    void deleteTradeModel(Long modelId);

    /**
     * 根据行业id列表获取模型(包含通用模型)
     * @param tradeIds 行业id列表
     * @return
     */
    List<DataModelInfoVo> listByTradeIds(List<Long> tradeIds);

    /**
     * 根据模型id列表获取模型信息(包含通用模型)
     * @param modelIds 模型id列表
     * @return
     */
    List<DataModelInfoVo> listByIdsAndGeneral(Collection<Long> modelIds);
}

