package com.jiuyu.replay.video.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.dao.VideoHotSearchDao;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;
import com.jiuyu.replay.video.project.service.VideoHotSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 爆款表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
@Service
@RequiredArgsConstructor
public class VideoHotSearchServiceImpl extends ServiceImpl<VideoHotSearchDao, VideoHotSearchEntity> implements VideoHotSearchService {

    private final VideoHotSearchDao videoHotSearchDao;

    @Override
    public List<VideoHotSearchEntity> listByPlatformTypeAndSearchKeywords(List<VideoHotSearchEntity> queryList) {
        return videoHotSearchDao.listByPlatformTypeAndSearchKeywords(queryList);
    }
}
