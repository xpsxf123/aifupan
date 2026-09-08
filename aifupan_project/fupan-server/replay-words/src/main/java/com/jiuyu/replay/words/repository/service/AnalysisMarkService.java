package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.AnalysisMarkEntity;
import com.jiuyu.replay.words.enums.VideoSourceType;

import java.util.List;

/**
 * Service
 *
 * @author liaoxin
 * @date 2025-06-07
 */
public interface AnalysisMarkService extends IService<AnalysisMarkEntity> {

    Integer getMaxMarkNo(String sourceId, Integer sourceType);

    Integer checkIndexOverlap(String sourceId, Integer sourceType, Integer paraphStartNo, Integer paraphEndNo, Integer startIndex, Integer endIndex);

    /**
     * 批量查询视频是否存在标注
     *
     * @param videoIds   视频ID
     * @param sourceType 源类型
     *
     * @return 存在标注的视频ID
     */
    List<String> videoExistsMark(List<String> videoIds, VideoSourceType sourceType);
}