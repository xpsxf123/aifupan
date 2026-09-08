package com.jiuyu.replay.video.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.video.project.dao.VideoHotSearchVideoDao;
import com.jiuyu.replay.video.project.entity.VideoHotSearchVideoEntity;
import com.jiuyu.replay.video.project.service.VideoHotSearchVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 爆款搜索视频关联表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-09-02
 */
@Service
@RequiredArgsConstructor
public class VideoHotSearchVideoServiceImpl extends ServiceImpl<VideoHotSearchVideoDao, VideoHotSearchVideoEntity> implements VideoHotSearchVideoService {

    private final VideoHotSearchVideoDao videoHotSearchVideoDao;

    @Override
    public List<VideoHotSearchVideoEntity> listVideoHotSearchVideoByPlatformTypeAndKeyWord(Byte platformType, String searchKeyword, Integer limit) {
        return videoHotSearchVideoDao.listVideoHotSearchVideoByPlatformTypeAndKeyWord(platformType, searchKeyword, limit);
    }
}
