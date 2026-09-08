package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.AiAnalysisRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Mapper
public interface AiAnalysisRecordDao extends BaseMapper<AiAnalysisRecordEntity> {
	
}
