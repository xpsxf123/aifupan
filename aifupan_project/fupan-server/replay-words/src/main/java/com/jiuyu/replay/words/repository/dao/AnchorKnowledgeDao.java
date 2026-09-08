package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.AnchorKnowledgeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主播级别知识库 Mapper
 *
 * @author jy
 * @date 2026-06-29
 */
@Mapper
public interface AnchorKnowledgeDao extends BaseMapper<AnchorKnowledgeEntity> {
}
