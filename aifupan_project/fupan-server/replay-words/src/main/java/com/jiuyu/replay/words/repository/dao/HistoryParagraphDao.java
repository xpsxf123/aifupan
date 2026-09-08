package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.HistoryParagraphEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ai问答的历史分析段落记录
 * 
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Mapper
public interface HistoryParagraphDao extends BaseMapper<HistoryParagraphEntity> {
	
}
