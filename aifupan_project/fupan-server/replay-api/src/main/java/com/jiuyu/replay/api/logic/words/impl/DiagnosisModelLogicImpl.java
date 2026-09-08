package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.DiagnosisModelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.ai.bll.DiagnosisModelBll;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelListBo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelListVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Service
@AllArgsConstructor
public class DiagnosisModelLogicImpl implements DiagnosisModelLogic {

    private final DiagnosisModelBll diagnosisModelBll;
    private final AiModelBll aiModelBll;



    @Override
    public R<PageUtils<DiagnosisModelListVo>> queryPage(DiagnosisModelListBo diagnosisModelListBo) {

        return diagnosisModelBll.queryPage(diagnosisModelListBo);
    }

    @Override
    public R<DiagnosisModelInfoVo> info(Long id) {

        return diagnosisModelBll.info(id);
    }

    @Override
    public R<String> save(DiagnosisModelBo diagnosisModelBo) {

        return diagnosisModelBll.save(diagnosisModelBo);
    }

    @Override
    public R<String> update(DiagnosisModelBo diagnosisModelBo) {

        return diagnosisModelBll.update(diagnosisModelBo);
    }

    @Override
    public R<String> delete(Long id) {

        return diagnosisModelBll.delete(id);
    }

    @Override
    public R<String> saveOrUpdate(DiagnosisModelBo diagnosisModelBo) {
        return diagnosisModelBll.saveOrUpdate(diagnosisModelBo);
    }
}

