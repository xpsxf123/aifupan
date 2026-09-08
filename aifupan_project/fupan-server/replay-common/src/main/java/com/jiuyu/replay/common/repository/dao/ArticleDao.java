package com.jiuyu.replay.common.repository.dao;

import com.jiuyu.replay.common.entity.ArticleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Mapper
public interface ArticleDao extends BaseMapper<ArticleEntity> {
	
}
