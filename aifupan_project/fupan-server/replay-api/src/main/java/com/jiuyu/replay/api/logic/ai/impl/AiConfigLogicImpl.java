package com.jiuyu.replay.api.logic.ai.impl;

import com.jiuyu.replay.api.logic.ai.AiConfigLogic;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * AI配置
 *
 * @author lujie
 * @date 2026-06-01
 */
@Service
public class AiConfigLogicImpl implements AiConfigLogic {

    @Resource
    private DictDataBll dictDataBll;

    @Override
    public AiContentCorrectionConfigVo getContentCorrectionConfig(Integer sceneType) {
        return dictDataBll.getContentCorrectionConfig(sceneType);
    }
}
