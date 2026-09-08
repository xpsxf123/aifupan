package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.IndustryKnowledgeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.IndustryKnowledgeBll;
import com.jiuyu.replay.words.bo.IndustryKnowledgeBo;
import com.jiuyu.replay.words.bo.IndustryKnowledgeListBo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeListVo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
@Service
public class IndustryKnowledgeLogicImpl implements IndustryKnowledgeLogic {

    @Resource
    private IndustryKnowledgeBll industryKnowledgeBll;

    @Override
    public R<PageUtils<IndustryKnowledgeListVo>> queryPage(IndustryKnowledgeListBo listBo) {
        return industryKnowledgeBll.queryPage(listBo);
    }

    @Override
    public R<IndustryKnowledgeVo> info(Long id) {
        return industryKnowledgeBll.info(id);
    }

    @Override
    public R<String> save(IndustryKnowledgeBo bo) {
        return industryKnowledgeBll.save(bo);
    }

    @Override
    public R<String> update(IndustryKnowledgeBo bo) {
        return industryKnowledgeBll.update(bo);
    }

    @Override
    public R<String> delete(Long id) {
        return industryKnowledgeBll.delete(id);
    }

}
