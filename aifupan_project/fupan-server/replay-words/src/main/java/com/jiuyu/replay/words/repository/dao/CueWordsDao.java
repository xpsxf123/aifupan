package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.CueWordsEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提示词
 * 
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Mapper
public interface CueWordsDao extends BaseMapper<CueWordsEntity> {
	
}
