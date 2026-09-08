package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.video.VideoSliceBo;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.words.entity.VideoSliceEntity;
import com.jiuyu.replay.words.repository.service.VideoSliceService;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class VideoSliceRseImpl implements VideoSliceRse {

    @Resource
    private VideoSliceService videoSliceService;

    @Override
    public void saveVideoSlice(VideoSliceBo videoSliceBo) {
        VideoSliceEntity videoSliceEntity = BeanConvertUtils.convert(videoSliceBo, VideoSliceEntity.class);
        videoSliceEntity.setId(SnowflakeManager.nextValue());
        videoSliceEntity.setCreateDate(new Date());
        videoSliceEntity.setUpdateDate(new Date());

        this.videoSliceService.save(videoSliceEntity);
    }

    @Override
    public void removeBySourceIds(List<String> sourceIds) {
        if(sourceIds != null && sourceIds.size() > 0) {
            QueryWrapper<VideoSliceEntity> wrapper = new QueryWrapper<>();
            wrapper.in("source_id", sourceIds);
            this.videoSliceService.remove(wrapper);
        }
    }

    @Override
    public void updateVideoSlice(VideoSliceBo videoSliceBo) {
        VideoSliceEntity videoSliceEntity = BeanConvertUtils.convert(videoSliceBo, VideoSliceEntity.class);
        this.videoSliceService.updateById(videoSliceEntity);
    }

    @Override
    public VideoSliceVo getVideoSliceBySourceId(String sourceId, Integer sourceType) {
        VideoSliceEntity one = this.videoSliceService.lambdaQuery()
                .eq(VideoSliceEntity::getSourceId, sourceId)
                .eq(VideoSliceEntity::getSliceType, sourceType)
                .last("limit 1")
                .one();
        if (one == null) {
            return null;
        }
        return BeanUtil.copyProperties(one, VideoSliceVo.class);
    }
    @Override
    public VideoSliceVo getByVideoId(String sourceId) {
        VideoSliceEntity videoSliceEntity = this.videoSliceService.getOne(new LambdaQueryWrapper<VideoSliceEntity>().eq(VideoSliceEntity::getSourceId, sourceId));
        if(videoSliceEntity != null) {
            return BeanConvertUtils.convert(videoSliceEntity, VideoSliceVo.class);
        }
        return null;
    }

}
