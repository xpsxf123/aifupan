package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.SensitiveWordsEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:10:10
 */
@Mapper
public interface SensitiveWordsDao extends BaseMapper<SensitiveWordsEntity> {
	
}
