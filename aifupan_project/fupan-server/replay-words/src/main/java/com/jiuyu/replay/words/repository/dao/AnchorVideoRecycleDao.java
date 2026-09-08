package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.AnchorVideoRecycleEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 云空间视频回收站 Mapper 接口。
 *
 * <p>本期仅需 {@link BaseMapper} 提供的基础 CRUD（{@code insert}）；
 * 列表查询接口在恢复功能迭代时再扩展。</p>
 */
@Mapper
public interface AnchorVideoRecycleDao extends BaseMapper<AnchorVideoRecycleEntity> {

    /**
     * 批量插入。
     *
     * @param entities 待插入的实体列表
     * @return 插入的行数
     */
    int batchInsert(List<AnchorVideoRecycleEntity> entities);
}
