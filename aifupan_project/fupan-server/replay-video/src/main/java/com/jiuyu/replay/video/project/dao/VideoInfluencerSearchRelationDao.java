package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.video.project.entity.VideoInfluencerSearchRelationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 达人搜索快照-达人关联表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Mapper
public interface VideoInfluencerSearchRelationDao extends BaseMapper<VideoInfluencerSearchRelationEntity> {

    /**
     * 搜索达人历史记录
     *
     * @param page       分页参数
     * @param tenantId   租户id
     * @param userId     用户id
     * @param snapshotId 快照id
     * @param queryTime  查询时间
     * @return
     */
    IPage<VideoInfluencerSearchRelationEntity> queryHistoryRecord(@Param("page") Page<VideoInfluencerSearchRelationEntity> page, @Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("snapshotId") Long snapshotId, @Param("queryTime") LocalDateTime queryTime);
}
