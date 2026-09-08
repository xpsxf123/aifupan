package com.jiuyu.replay.third.repository.dao;

import com.jiuyu.replay.third.entity.AiModelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型配置表
 * 
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Mapper
public interface AiModelDao extends BaseMapper<AiModelEntity> {
	
}
