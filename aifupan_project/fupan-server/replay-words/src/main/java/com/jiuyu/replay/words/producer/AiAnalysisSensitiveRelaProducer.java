package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaListVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaListBo;

import java.util.List;


/**
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
public interface AiAnalysisSensitiveRelaProducer {


    /**
     * AI分析关键词与记录关联关系表列表
     * @param aiAnalysisSensitiveRelaListBo AI分析关键词与记录关联关系表列表查询参数
     * @return
     */
    PageUtils<AiAnalysisSensitiveRelaListVo> queryPage(AiAnalysisSensitiveRelaListBo aiAnalysisSensitiveRelaListBo);

    /**
    * AI分析关键词与记录关联关系表信息
    * @param id AI分析关键词与记录关联关系表id
    * @return
    */
    AiAnalysisSensitiveRelaInfoVo info(Long id);

    /**
     * 新增AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
     AiAnalysisSensitiveRelaInfoVo save(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo);

    /**
     * 修改AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    void update(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo);

    /**
     * 删除AI分析关键词与记录关联关系表
     * @param id AI分析关键词与记录关联关系表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 批量保存AI分析关键词与记录关联关系
     * @param notMarkWordList 数据列表
     */
    void saveBatch(List<AiAnalysisSensitiveRelaBo> notMarkWordList);

    /**
     * 根据词语列表和行业id从ai分析词库删除对应的词语
     * @param wordNameList 词语列表
     * @param tradeId 行业id
     */
    void delByWordListAndTrade(List<String> wordNameList, Long tradeId);
}

