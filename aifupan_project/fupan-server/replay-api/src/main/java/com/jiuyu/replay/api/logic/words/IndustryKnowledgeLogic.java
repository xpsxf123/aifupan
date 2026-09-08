package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
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
public interface IndustryKnowledgeLogic {

    /**
     * 知识库列表
     *
     * @param listBo 列表查询参数
     * @return
     */
    R<PageUtils<IndustryKnowledgeListVo>> queryPage(IndustryKnowledgeListBo listBo);

    /**
     * 知识库信息
     *
     * @param id 知识库id
     * @return
     */
    R<IndustryKnowledgeVo> info(Long id);

    /**
     * 新增知识库
     *
     * @param bo 知识库对象
     * @return
     */
    R<String> save(IndustryKnowledgeBo bo);

    /**
     * 修改知识库
     *
     * @param bo 知识库对象
     * @return
     */
    R<String> update(IndustryKnowledgeBo bo);

    /**
     * 删除知识库
     *
     * @param id 知识库id
     * @return
     */
    R<String> delete(Long id);

}
