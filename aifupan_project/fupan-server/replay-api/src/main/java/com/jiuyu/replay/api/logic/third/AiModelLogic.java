package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusV2Bo;
import com.jiuyu.replay.third.bo.AiModelListBo;


/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
public interface AiModelLogic {


    /**
     * AI模型配置表列表
     * @param aiModelListBo AI模型配置表列表查询参数
     * @return
     */
    R<PageUtils<AiModelListVo>> queryPage(AiModelListBo aiModelListBo);

    /**
    * AI模型配置表信息
    * @param id AI模型配置表id
    * @return
    */
    R<AiModelInfoVo> info(Long id);

    /**
     * 新增AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    R<String> save(AiModelBo aiModelBo);

    /**
     * 修改AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    R<String> update(AiModelBo aiModelBo);

    /**
     * 删除AI模型配置表
     * @param id AI模型配置表id
     * @return
     */
    R<String> delete(Long id);

    /**
     * AI模型配置表消耗令牌
     *
     * @param assets AI模型配置表消耗令牌参数
     * @return AI模型配置表消耗令牌结果
     */
    AssetsMinusOrPlusV2Bo AiTokenConsumeMultiple(AssetsMinusOrPlusV2Bo assets);
}

