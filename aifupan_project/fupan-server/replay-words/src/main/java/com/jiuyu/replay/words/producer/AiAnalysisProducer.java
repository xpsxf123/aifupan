package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisListBo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisListVo;

import java.util.List;


/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
public interface AiAnalysisProducer {


    /**
     * AI分析表列表
     * @param aiAnalysisListBo AI分析表列表查询参数
     * @return
     */
    PageUtils<AiAnalysisListVo> queryPage(AiAnalysisListBo aiAnalysisListBo);

    /**
    * AI分析表信息
    * @param id AI分析表id
    * @return
    */
    AiAnalysisInfoVo info(Long id);

    /**
     * 新增AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
     AiAnalysisInfoVo save(AiAnalysisBo aiAnalysisBo);

    /**
     * 修改AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    void update(AiAnalysisBo aiAnalysisBo);

    /**
     * 删除AI分析表
     * @param id AI分析表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据唯一标识获取AI分析最后一条记录
     * @param uuid 唯一标识
     * @return
     */
    int LastSort(String uuid);

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
    String analysisStatusByUuid(String uuid, String modelId);
}

