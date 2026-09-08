package com.jiuyu.replay.system.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.system.entity.SeoSlugHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * SEO slug 变更历史。
 *
 * <p>无自定义方法：查询与写入都是单表条件操作，用 LambdaQueryWrapper / LambdaUpdateWrapper
 * 即可，不需要原生 SQL，因而也不需要配套的 mapper.xml。
 *
 * @author claude
 * @date 2026-08-18
 */
@Mapper
public interface SeoSlugHistoryDao extends BaseMapper<SeoSlugHistoryEntity> {
}
