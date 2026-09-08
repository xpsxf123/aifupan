package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.AiAnalysisSensitiveRelaLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AiAnalysisSensitiveRelaBll;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaListBo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Service
public class AiAnalysisSensitiveRelaLogicImpl implements AiAnalysisSensitiveRelaLogic {

    @Resource
    private AiAnalysisSensitiveRelaBll aiAnalysisSensitiveRelaBll;


    @Override
    public R<PageUtils<AiAnalysisSensitiveRelaListVo>> queryPage(AiAnalysisSensitiveRelaListBo aiAnalysisSensitiveRelaListBo) {

        return aiAnalysisSensitiveRelaBll.queryPage(aiAnalysisSensitiveRelaListBo);
    }

    @Override
    public R<AiAnalysisSensitiveRelaInfoVo> info(Long id) {

        return aiAnalysisSensitiveRelaBll.info(id);
    }

    @Override
    public R<String> save(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo) {

        return aiAnalysisSensitiveRelaBll.save(aiAnalysisSensitiveRelaBo);
    }

    @Override
    public R<String> update(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo) {

        return aiAnalysisSensitiveRelaBll.update(aiAnalysisSensitiveRelaBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiAnalysisSensitiveRelaBll.delete(id);
    }


}

