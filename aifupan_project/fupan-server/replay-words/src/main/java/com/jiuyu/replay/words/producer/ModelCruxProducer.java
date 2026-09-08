package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.bo.words.CruxTypeScaleBo;
import com.jiuyu.replay.words.vo.ModelCruxListVo;
import com.jiuyu.replay.words.vo.ModelCruxInfoVo;
import com.jiuyu.replay.words.bo.ModelCruxBo;
import com.jiuyu.replay.words.bo.ModelCruxListBo;

import java.util.List;


/**
 * 模型-关键词类型-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
public interface ModelCruxProducer {


    /**
     * 模型-关键词类型-关联表列表
     * @param modelCruxListBo 模型-关键词类型-关联表列表查询参数
     * @return
     */
    PageUtils<ModelCruxListVo> queryPage(ModelCruxListBo modelCruxListBo);

    /**
    * 模型-关键词类型-关联表信息
    * @param id 模型-关键词类型-关联表id
    * @return
    */
    ModelCruxInfoVo info(Long id);

    /**
     * 新增模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
     ModelCruxInfoVo save(ModelCruxBo modelCruxBo);

    /**
     * 修改模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
    void update(ModelCruxBo modelCruxBo);

    /**
     * 删除模型-关键词类型-关联表
     * @param id 模型-关键词类型-关联表id
     * @return
     */
    void deleteById(Long id);


    void saveBatchModel(List<CruxTypeScaleBo> cruxTypeScaleBos, Long dataModelId);
}

