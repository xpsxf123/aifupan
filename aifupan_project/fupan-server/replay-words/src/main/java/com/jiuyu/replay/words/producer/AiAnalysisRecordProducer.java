package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.entity.AiAnalysisRecordEntity;
import com.jiuyu.replay.words.vo.AiAnalysisRecordListVo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordListBo;

import java.util.List;


/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
public interface AiAnalysisRecordProducer {


    /**
     * AI分析出来的关键词总数和未匹配上词库的关键词个数列表
     * @param aiAnalysisRecordListBo AI分析出来的关键词总数和未匹配上词库的关键词个数列表查询参数
     * @return
     */
    PageUtils<AiAnalysisRecordListVo> queryPage(AiAnalysisRecordListBo aiAnalysisRecordListBo);

    /**
    * AI分析出来的关键词总数和未匹配上词库的关键词个数信息
    * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
    * @return
    */
    AiAnalysisRecordInfoVo info(Long id);

    /**
     * 新增AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
     AiAnalysisRecordInfoVo save(AiAnalysisRecordBo aiAnalysisRecordBo);

    /**
     * 修改AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    void update(AiAnalysisRecordBo aiAnalysisRecordBo);

    /**
     * 删除AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
     * @return
     */
    void deleteById(Long id);


    /**
     * 查询所有
     *
     * @return
     */
    List<AiAnalysisRecordEntity> allList();

    /**
     * 根据视频id查询
     *
     * @param videoIds
     * @return
     */
    List<AiAnalysisRecordEntity> allListByVideoIds(List<String> videoIds);
}

