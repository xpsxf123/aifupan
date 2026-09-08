package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.AnchorKnowledgeEntity;
import com.jiuyu.replay.words.repository.dao.AnchorKnowledgeDao;
import com.jiuyu.replay.words.repository.service.AnchorKnowledgeService;
import org.springframework.stereotype.Service;

/**
 * 主播级别知识库 Service 实现
 *
 * @author jy
 * @date 2026-06-29
 */
@Service("anchorKnowledgeService")
public class AnchorKnowledgeServiceImpl extends ServiceImpl<AnchorKnowledgeDao, AnchorKnowledgeEntity> implements AnchorKnowledgeService {
}
