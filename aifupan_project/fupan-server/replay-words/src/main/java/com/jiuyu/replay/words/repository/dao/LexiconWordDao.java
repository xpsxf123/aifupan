package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.LexiconWordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 词库-词语关联
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Mapper
public interface LexiconWordDao extends BaseMapper<LexiconWordEntity> {
	
}
