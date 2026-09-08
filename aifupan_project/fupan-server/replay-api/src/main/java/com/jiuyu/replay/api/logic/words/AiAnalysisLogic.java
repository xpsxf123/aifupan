package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisListVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisListBo;
import com.volcengine.ApiException;

import java.util.List;
import java.util.Set;


/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
public interface AiAnalysisLogic {


    /**
     * AI分析表列表
     * @param aiAnalysisListBo AI分析表列表查询参数
     * @return
     */
    R<PageUtils<AiAnalysisListVo>> queryPage(AiAnalysisListBo aiAnalysisListBo);

    /**
    * AI分析表信息
    * @param id AI分析表id
    * @return
    */
    R<AiAnalysisInfoVo> info(Long id);

    /**
     * 新增AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    R<String> save(AiAnalysisBo aiAnalysisBo);

    /**
     * 修改AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    R<String> update(AiAnalysisBo aiAnalysisBo);

    /**
     * 删除AI分析表
     * @param id AI分析表id
     * @return
     */
    R<String> delete(Long id);

    /**
     * AI分析内容
     * @param analysisBo Ai分析类
     * @return
     */
    R<List<String>> aiAnalysis(AiAnalysisBo analysisBo);

    /**
     * 根据唯一标识查询所有AI分析记录
     * @param uuid 唯一标识
     * @return
     */
    R<List<List<String>>> listAiAnalysisByUuid(String uuid);

    /**
     * 根据唯一标识去判断是否有正在进行AI分析的文本;返回true代表正在分析,false则没有
     * @param uuid 唯一标识
     * @return
     */
    R<String> analysisStatusByUuid(String uuid, String modelId);

    R<Set<String>> getAllSessionId();

    /**
     * 获取ArkTempToken
     * @return
     */
    R<AiTempTokenVo> getArkTempToken() throws ApiException;

}

