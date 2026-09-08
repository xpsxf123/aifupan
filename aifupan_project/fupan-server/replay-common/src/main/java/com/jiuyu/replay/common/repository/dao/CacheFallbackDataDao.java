package com.jiuyu.replay.common.repository.dao;

import com.jiuyu.replay.common.entity.CacheFallbackDataEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 缓存降级数据表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Mapper
public interface CacheFallbackDataDao extends BaseMapper<CacheFallbackDataEntity> {

}
