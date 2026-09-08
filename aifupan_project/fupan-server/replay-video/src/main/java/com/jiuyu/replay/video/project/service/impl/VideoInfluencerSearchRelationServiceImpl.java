package com.jiuyu.replay.video.project.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.dao.VideoInfluencerSearchRelationDao;
import com.jiuyu.replay.video.project.entity.VideoInfluencerSearchRelationEntity;
import com.jiuyu.replay.video.project.service.VideoInfluencerSearchRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 达人搜索快照-达人关联表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Service
@RequiredArgsConstructor
public class VideoInfluencerSearchRelationServiceImpl extends ServiceImpl<VideoInfluencerSearchRelationDao, VideoInfluencerSearchRelationEntity> implements VideoInfluencerSearchRelationService {

    private final VideoInfluencerSearchRelationDao videoInfluencerSearchRelationDao;

    @Override
    public IPage<VideoInfluencerSearchRelationEntity> queryHistoryRecord(Page<VideoInfluencerSearchRelationEntity> page, Long tenantId, Long userId, Long snapshotId, LocalDateTime queryTime) {
        return videoInfluencerSearchRelationDao.queryHistoryRecord(page, tenantId, userId, snapshotId, queryTime);
    }
}
