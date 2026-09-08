package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.ai.bll.DiagnosisModelBll;
import com.jiuyu.replay.api.logic.third.AiModelLogic;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusV2Bo;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.third.bo.AiModelListBo;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Service
public class AiModelLogicImpl implements AiModelLogic {

    @Resource
    private AiModelBll aiModelBll;
    @Resource
    private VolcengineProperties volcengineProperties;
    @Resource
    private DiagnosisModelBll diagnosisModelBll;
    @Resource
    private SystemKvProducer systemKvProducer;


    @Override
    public R<PageUtils<AiModelListVo>> queryPage(AiModelListBo aiModelListBo) {

        return aiModelBll.queryPage(aiModelListBo);
    }

    @Override
    public R<AiModelInfoVo> info(Long id) {

        return aiModelBll.info(id);
    }

    @Override
    public R<String> save(AiModelBo aiModelBo) {

        return aiModelBll.save(aiModelBo);
    }

    @Override
    public R<String> update(AiModelBo aiModelBo) {

        return aiModelBll.update(aiModelBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiModelBll.delete(id);
    }

    @Override
    public AssetsMinusOrPlusV2Bo AiTokenConsumeMultiple(AssetsMinusOrPlusV2Bo assets) {
        AiModelInfoVo aiModelBo = aiModelBll.getAiModelByCode(assets.getModelCode());
        Double cacheRate = Double.parseDouble(systemKvProducer.getValueByKey("ai_cache_token_rate", "1"));

        assets.setNum(AiUtils.aiTokenConsumeMultiple(Math.abs(assets.getNum()),
                assets.getCachedTokens(),
                aiModelBo != null ? aiModelBo.getConsumeMultiple() : null,
                cacheRate));
        return assets;
    }
}

