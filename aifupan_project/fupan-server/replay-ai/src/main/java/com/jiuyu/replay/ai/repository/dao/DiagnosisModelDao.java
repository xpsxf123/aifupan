package com.jiuyu.replay.ai.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.ai.entity.DiagnosisModelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * ai诊断中的模型设置-主播和视频
 * 
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Mapper
public interface DiagnosisModelDao extends BaseMapper<DiagnosisModelEntity> {
	
}
