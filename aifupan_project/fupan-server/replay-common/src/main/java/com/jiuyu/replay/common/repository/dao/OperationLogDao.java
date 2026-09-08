package com.jiuyu.replay.common.repository.dao;

import com.jiuyu.replay.common.entity.OperationLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志表
 * 
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Mapper
public interface OperationLogDao extends BaseMapper<OperationLogEntity> {
	
}
