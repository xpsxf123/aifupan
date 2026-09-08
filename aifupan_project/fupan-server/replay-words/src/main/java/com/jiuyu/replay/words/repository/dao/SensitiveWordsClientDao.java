package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.SensitiveWordsClientEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端自定义词语
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:10:10
 */
@Mapper
public interface SensitiveWordsClientDao extends BaseMapper<SensitiveWordsClientEntity> {
	
}
