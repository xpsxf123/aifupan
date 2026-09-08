package com.jiuyu.replay.video.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;

import java.util.List;

/**
 * <p>
 * 达人信息表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
public interface VideoInfluencerInfoService extends IService<VideoInfluencerInfoEntity> {

    /**
     * 批量保存或更新达人数据（原子性操作）
     *
     * @param infoEntities 达人信息实体列表
     */
    void batchSaveOrUpdate(List<VideoInfluencerInfoEntity> infoEntities);

    /**
     * 单个保存或更新达人数据（原子性操作）
     *
     * @param infoEntity 达人信息实体
     * @return 保存或更新后的达人信息实体
     */
    VideoInfluencerInfoEntity singleSaveOrUpdate(VideoInfluencerInfoEntity infoEntity);
}
