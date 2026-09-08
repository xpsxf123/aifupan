package com.jiuyu.replay.order.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.entity.PowerRollupWatermarkEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 算力汇总增量水位线
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Mapper
public interface PowerRollupWatermarkDao extends BaseMapper<PowerRollupWatermarkEntity> {

}
