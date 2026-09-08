package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.AiAnalysisRecordListVo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordListBo;


/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
public interface AiAnalysisRecordLogic {


    /**
     * AI分析出来的关键词总数和未匹配上词库的关键词个数列表
     * @param aiAnalysisRecordListBo AI分析出来的关键词总数和未匹配上词库的关键词个数列表查询参数
     * @return
     */
    R<PageUtils<AiAnalysisRecordListVo>> queryPage(AiAnalysisRecordListBo aiAnalysisRecordListBo);

    /**
    * AI分析出来的关键词总数和未匹配上词库的关键词个数信息
    * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
    * @return
    */
    R<AiAnalysisRecordInfoVo> info(Long id);

    /**
     * 新增AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    R<String> save(AiAnalysisRecordBo aiAnalysisRecordBo);

    /**
     * 修改AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    R<String> update(AiAnalysisRecordBo aiAnalysisRecordBo);

    /**
     * 删除AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
     * @return
     */
    R<String> delete(Long id);


}

