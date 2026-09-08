package com.jiuyu.replay.words.repository.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.AnalysisMarkEntity;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.repository.dao.AnalysisMarkDao;
import com.jiuyu.replay.words.repository.service.AnalysisMarkService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ServiceImpl
 * 
 * @author liaoxin
 * @date 2025-06-07
 */
@Service
public class AnalysisMarkServiceImpl extends ServiceImpl<AnalysisMarkDao, AnalysisMarkEntity>
        implements AnalysisMarkService {

    @Override
    public Integer getMaxMarkNo(String sourceId, Integer sourceType) {
        return baseMapper.getMaxMarkNo(sourceId, sourceType);
    }

    @Override
    public Integer checkIndexOverlap(String sourceId, Integer sourceType, Integer paraphStartNo, Integer paraphEndNo, Integer startIndex, Integer endIndex) {
        return baseMapper.checkIndexOverlap(sourceId, sourceType, paraphStartNo, paraphEndNo, startIndex, endIndex);
    }


    /**
     * 批量查询视频是否存在标注
     *
     * @param videoIds   视频ID
     * @param sourceType 源类型
     *
     * @return 存在标注的视频ID
     */
    @Override
    public List<String> videoExistsMark(List<String> videoIds, VideoSourceType sourceType) {
        if (CollUtil.isEmpty(videoIds) || sourceType == null) {
            return List.of();
        }
        return baseMapper.videoExistsMark(videoIds, sourceType.getCode());
    }
}