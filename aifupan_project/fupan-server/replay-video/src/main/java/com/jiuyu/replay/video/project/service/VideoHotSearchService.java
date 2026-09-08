package com.jiuyu.replay.video.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;

import java.util.List;

/**
 * <p>
 * 爆款表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
public interface VideoHotSearchService extends IService<VideoHotSearchEntity> {

    /**
     * 批量查询数据结果
     *
     * @param queryList 根据多个平台类型和关键词
     * @return
     */
    List<VideoHotSearchEntity> listByPlatformTypeAndSearchKeywords(List<VideoHotSearchEntity> queryList);
}
