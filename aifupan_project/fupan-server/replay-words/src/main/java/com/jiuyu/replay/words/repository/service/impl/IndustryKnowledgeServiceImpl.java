package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.IndustryKnowledgeEntity;
import com.jiuyu.replay.words.repository.dao.IndustryKnowledgeDao;
import com.jiuyu.replay.words.repository.service.IndustryKnowledgeService;
import org.springframework.stereotype.Service;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
@Service("industryKnowledgeService")
public class IndustryKnowledgeServiceImpl extends ServiceImpl<IndustryKnowledgeDao, IndustryKnowledgeEntity> implements IndustryKnowledgeService {
}
