package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.IndustryKnowledgeBo;
import com.jiuyu.replay.words.bo.IndustryKnowledgeListBo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeListVo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeVo;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
public interface IndustryKnowledgeProducer {

    /**
     * 知识库列表
     *
     * @param listBo 列表查询参数
     * @return
     */
    PageUtils<IndustryKnowledgeListVo> queryPage(IndustryKnowledgeListBo listBo);

    /**
     * 知识库信息
     *
     * @param id 知识库id
     * @return
     */
    IndustryKnowledgeVo info(Long id);

    /**
     * 新增知识库
     *
     * @param bo 知识库对象
     * @return
     */
    IndustryKnowledgeVo save(IndustryKnowledgeBo bo);

    /**
     * 修改知识库
     *
     * @param bo 知识库对象
     */
    void update(IndustryKnowledgeBo bo);

    /**
     * 删除知识库
     *
     * @param id 知识库id
     */
    void deleteById(Long id);

    /**
     * 根据行业ID和知识库类型查询有效记录
     *
     * @param tradeId       行业ID
     * @param knowledgeType 知识库类型
     * @return
     */
    IndustryKnowledgeVo getByTradeIdAndType(Long tradeId, Integer knowledgeType);

}
