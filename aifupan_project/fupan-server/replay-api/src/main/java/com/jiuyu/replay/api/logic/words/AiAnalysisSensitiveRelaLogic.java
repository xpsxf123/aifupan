package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaListVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaListBo;


/**
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
public interface AiAnalysisSensitiveRelaLogic {


    /**
     * AI分析关键词与记录关联关系表列表
     * @param aiAnalysisSensitiveRelaListBo AI分析关键词与记录关联关系表列表查询参数
     * @return
     */
    R<PageUtils<AiAnalysisSensitiveRelaListVo>> queryPage(AiAnalysisSensitiveRelaListBo aiAnalysisSensitiveRelaListBo);

    /**
    * AI分析关键词与记录关联关系表信息
    * @param id AI分析关键词与记录关联关系表id
    * @return
    */
    R<AiAnalysisSensitiveRelaInfoVo> info(Long id);

    /**
     * 新增AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    R<String> save(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo);

    /**
     * 修改AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    R<String> update(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo);

    /**
     * 删除AI分析关键词与记录关联关系表
     * @param id AI分析关键词与记录关联关系表id
     * @return
     */
    R<String> delete(Long id);


}

