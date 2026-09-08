package com.jiuyu.replay.ai.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.ai.entity.CustPromptEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户自定义提示词 Dao
 *
 * @author jxy
 * @date 2025-01-21
 */
@Mapper
public interface CustPromptDao extends BaseMapper<CustPromptEntity> {
}

