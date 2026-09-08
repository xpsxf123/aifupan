package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.AiAnalysisRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.AiAnalysisRecordListVo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordListBo;
import com.jiuyu.replay.words.bll.AiAnalysisRecordBll;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;


/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Service
public class AiAnalysisRecordLogicImpl implements AiAnalysisRecordLogic {

    @Resource
    private AiAnalysisRecordBll aiAnalysisRecordBll;


    @Override
    public R<PageUtils<AiAnalysisRecordListVo>> queryPage(AiAnalysisRecordListBo aiAnalysisRecordListBo) {

        return aiAnalysisRecordBll.queryPage(aiAnalysisRecordListBo);
    }

    @Override
    public R<AiAnalysisRecordInfoVo> info(Long id) {

        return aiAnalysisRecordBll.info(id);
    }

    @Override
    public R<String> save(AiAnalysisRecordBo aiAnalysisRecordBo) {

        return aiAnalysisRecordBll.save(aiAnalysisRecordBo);
    }

    @Override
    public R<String> update(AiAnalysisRecordBo aiAnalysisRecordBo) {

        return aiAnalysisRecordBll.update(aiAnalysisRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiAnalysisRecordBll.delete(id);
    }


}

