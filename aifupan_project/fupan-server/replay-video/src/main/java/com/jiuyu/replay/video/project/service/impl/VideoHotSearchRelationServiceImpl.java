package com.jiuyu.replay.video.project.service.impl;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchListQueryBo;
import com.jiuyu.replay.video.project.dao.VideoHotSearchRelationDao;
import com.jiuyu.replay.video.project.entity.VideoHotSearchRelationEntity;
import com.jiuyu.replay.video.project.service.VideoHotSearchRelationService;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 爆款搜索关联表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-29
 */
@Service
@RequiredArgsConstructor
public class VideoHotSearchRelationServiceImpl extends ServiceImpl<VideoHotSearchRelationDao, VideoHotSearchRelationEntity> implements VideoHotSearchRelationService {

    private final VideoHotSearchRelationDao videoHotSearchRelationDao;

    @Override
    public Page<VideoHotSearchResultVo> queryHotSearchResultsWithJoin(VideoHotSearchListQueryBo queryBo, Long userId, Long tenantId) {
        // 创建分页对象
        Page<VideoHotSearchResultVo> page = new Page<>(queryBo.getPage(), queryBo.getLimit());

        // 调用Mapper的连表查询方法
        return videoHotSearchRelationDao.queryHotSearchResultsWithJoin(page, queryBo, userId, tenantId);
    }

    @Override
    public Page<VideoHotSearchResultVo> queryHotSearchRecord(Page<VideoHotSearchResultVo> page, Long snapshotId, Long tenantId, Byte sortCode, Byte sortSequence) {
        return videoHotSearchRelationDao.queryHotSearchRecord(page, snapshotId, tenantId, sortCode, sortSequence);
    }
}
