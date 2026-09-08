package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AnchorKnowledgeListBo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeSaveBo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeListVo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeVo;

/**
 * 主播级别知识库 Logic 接口
 *
 * @author jy
 * @date 2026-06-29
 */
public interface AnchorKnowledgeLogic {

    R<PageUtils<AnchorKnowledgeListVo>> queryPage(AnchorKnowledgeListBo listBo);

    R<AnchorKnowledgeVo> info(String secUid, Long userId);

    R<String> save(AnchorKnowledgeSaveBo bo);

    R<String> update(AnchorKnowledgeSaveBo bo);

    R<String> delete(String secUid, Long userId);
}
