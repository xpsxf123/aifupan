package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.WordRuleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 词语匹配规则
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Mapper
public interface WordRuleDao extends BaseMapper<WordRuleEntity> {
	
}
