package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.AnchorKnowledgeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AnchorKnowledgeBll;
import com.jiuyu.replay.words.bo.AnchorKnowledgeListBo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeSaveBo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeListVo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 主播级别知识库 Logic 实现
 *
 * @author jy
 * @date 2026-06-29
 */
@Service
public class AnchorKnowledgeLogicImpl implements AnchorKnowledgeLogic {

    @Resource
    private AnchorKnowledgeBll anchorKnowledgeBll;

    @Override
    public R<PageUtils<AnchorKnowledgeListVo>> queryPage(AnchorKnowledgeListBo listBo) {
        return anchorKnowledgeBll.queryPage(listBo);
    }

    @Override
    public R<AnchorKnowledgeVo> info(String secUid, Long userId) {
        return anchorKnowledgeBll.info(secUid, userId);
    }

    @Override
    public R<String> save(AnchorKnowledgeSaveBo bo) {
        return anchorKnowledgeBll.save(bo);
    }

    @Override
    public R<String> update(AnchorKnowledgeSaveBo bo) {
        return anchorKnowledgeBll.update(bo);
    }

    @Override
    public R<String> delete(String secUid, Long userId) {
        return anchorKnowledgeBll.delete(secUid, userId);
    }
}
