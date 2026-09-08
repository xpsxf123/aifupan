package com.jiuyu.replay.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * MQ消息记录表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-06-03
 */
@Mapper
public interface MqMessageRecordDao extends BaseMapper<MqMessageRecordEntity> {

}
