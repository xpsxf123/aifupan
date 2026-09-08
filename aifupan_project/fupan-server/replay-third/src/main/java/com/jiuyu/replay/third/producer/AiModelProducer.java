package com.jiuyu.replay.third.producer;

import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import com.jiuyu.replay.third.bo.AiModelListBo;

import java.util.List;


/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
public interface AiModelProducer {


    /**
     * AI模型配置表列表
     * @param aiModelListBo AI模型配置表列表查询参数
     * @return
     */
    PageUtils<AiModelListVo> queryPage(AiModelListBo aiModelListBo);

    /**
    * AI模型配置表信息
    * @param id AI模型配置表id
    * @return
    */
    AiModelInfoVo info(Long id);

    /**
     * 新增AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
     AiModelInfoVo save(AiModelBo aiModelBo);

    /**
     * 修改AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    void update(AiModelBo aiModelBo);

    /**
     * 删除AI模型配置表
     * @param id AI模型配置表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据模型名称查询模型信息
     * @param modelName
     * @return
     */
    AiModelInfoVo getByModelName(String modelName);

    /**
     * 根据模型编码查询模型信息
     * @param code
     * @return
     */
    AiModelInfoVo getByCode(String code);

    /**
     * 根据模型编码列表查询模型信息
     * @param codes
     * @return
     */
    List<AiModelInfoVo> listByCodes(List<String> codes);
}

