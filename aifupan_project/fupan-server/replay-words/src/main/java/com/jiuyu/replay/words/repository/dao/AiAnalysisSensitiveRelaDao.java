package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.AiAnalysisSensitiveRelaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI分析关键词与记录关联关系表
 * 
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Mapper
public interface AiAnalysisSensitiveRelaDao extends BaseMapper<AiAnalysisSensitiveRelaEntity> {
	
}
