package com.jiuyu.replay.video.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoInfluencerSearchRelationEntity;

import java.time.LocalDateTime;

/**
 * <p>
 * 达人搜索快照-达人关联表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
public interface VideoInfluencerSearchRelationService extends IService<VideoInfluencerSearchRelationEntity> {

    /**
     * 搜索达人历史记录
     *
     * @param page       分页参数
     * @param tenantId   租户id
     * @param userId
     * @param snapshotId 快照id
     * @param queryTime  查询时间
     * @return
     */
    IPage<VideoInfluencerSearchRelationEntity> queryHistoryRecord(Page<VideoInfluencerSearchRelationEntity> page, Long tenantId, Long userId, Long snapshotId, LocalDateTime queryTime);
}
