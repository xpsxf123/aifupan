package com.jiuyu.replay.video.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoHotSearchVideoEntity;

import java.util.List;

/**
 * <p>
 * 爆款搜索视频关联表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-09-02
 */
public interface VideoHotSearchVideoService extends IService<VideoHotSearchVideoEntity> {

    /**
     * 查询爆款视频关联数据表数据根据平台类型和关键字
     *
     * @param platformType
     * @param searchKeyword
     * @return
     */
    List<VideoHotSearchVideoEntity> listVideoHotSearchVideoByPlatformTypeAndKeyWord(Byte platformType, String searchKeyword, Integer Limit);
}
