package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AnchorKnowledgeListBo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeSaveBo;
import com.jiuyu.replay.words.producer.AnchorKnowledgeProducer;
import com.jiuyu.replay.words.vo.AnchorKnowledgeListVo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 主播级别知识库 BLL
 *
 * @author jy
 * @date 2026-06-29
 */
@Component
public class AnchorKnowledgeBll {

    @Resource
    private AnchorKnowledgeProducer anchorKnowledgeProducer;

    public R<PageUtils<AnchorKnowledgeListVo>> queryPage(AnchorKnowledgeListBo listBo) {
        return R.ok("获取成功", anchorKnowledgeProducer.queryPage(listBo));
    }

    public R<AnchorKnowledgeVo> info(String secUid, Long userId) {
        AnchorKnowledgeVo vo = anchorKnowledgeProducer.getByUserAndSecUid(userId, secUid);
        return R.ok("获取成功", vo);
    }

    public R<String> save(AnchorKnowledgeSaveBo bo) {
        anchorKnowledgeProducer.saveOrUpdate(bo);
        return R.ok("保存成功");
    }

    public R<String> update(AnchorKnowledgeSaveBo bo) {
        anchorKnowledgeProducer.saveOrUpdate(bo);
        return R.ok("修改成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(String secUid, Long userId) {
        anchorKnowledgeProducer.deleteBySecUid(userId, secUid);
        return R.ok("删除成功");
    }
}
