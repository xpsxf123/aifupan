package com.jiuyu.replay.ai.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.ai.entity.GlobalProblemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 全局提示词 Dao
 *
 * @author lj
 * @date 2026-05-21
 */
@Mapper
public interface GlobalProblemDao extends BaseMapper<GlobalProblemEntity> {
}
