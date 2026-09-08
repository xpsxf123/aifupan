package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioListBo;
import com.jiuyu.replay.words.entity.VideoDataViewingRatioEntity;
import com.jiuyu.replay.words.producer.VideoDataViewingRatioProducer;
import com.jiuyu.replay.words.repository.service.VideoDataViewingRatioService;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 数据看盘比例
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@Service
public class VideoDataViewingRatioProducerImpl implements VideoDataViewingRatioProducer {

    @Resource
    private VideoDataViewingRatioService videoDataViewingRatioService;


    @Override
    public PageUtils<VideoDataViewingRatioListVo> queryPage(VideoDataViewingRatioListBo videoDataViewingRatioListBo) {
        QueryWrapper<VideoDataViewingRatioEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(videoDataViewingRatioListBo.getKeyword())){
            wrapper.like("name", videoDataViewingRatioListBo.getKeyword());
        }

        IPage<VideoDataViewingRatioEntity> iPage = videoDataViewingRatioService.page(new Query<VideoDataViewingRatioEntity>().getPage(videoDataViewingRatioListBo.getPage(), videoDataViewingRatioListBo.getLimit()), wrapper);

        PageUtils<VideoDataViewingRatioListVo> pageUtils = new PageUtils<>(videoDataViewingRatioListBo.getPage(), videoDataViewingRatioListBo.getLimit(), iPage);

        List<VideoDataViewingRatioEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<VideoDataViewingRatioListVo> vos = records.stream().map(item -> {
                VideoDataViewingRatioListVo videoDataViewingRatioVo = new VideoDataViewingRatioListVo();
                BeanUtils.copyProperties(item, videoDataViewingRatioVo);
                return videoDataViewingRatioVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public VideoDataViewingRatioInfoVo info(Long id) {

        VideoDataViewingRatioEntity videoDataViewingRatioEntity = videoDataViewingRatioService.getById(id);
        if(videoDataViewingRatioEntity != null) {
            VideoDataViewingRatioInfoVo videoDataViewingRatioInfoVo = new VideoDataViewingRatioInfoVo();
            BeanUtils.copyProperties(videoDataViewingRatioEntity, videoDataViewingRatioInfoVo);
            return videoDataViewingRatioInfoVo;
        }

        return null;
    }

    @Override
    public VideoDataViewingRatioInfoVo save(VideoDataViewingRatioBo videoDataViewingRatioBo) {

         VideoDataViewingRatioEntity videoDataViewingRatioEntity = new VideoDataViewingRatioEntity();
         BeanUtils.copyProperties(videoDataViewingRatioBo, videoDataViewingRatioEntity);
         videoDataViewingRatioEntity.setId(SnowflakeManager.nextValue());
         videoDataViewingRatioEntity.setCreateDate(new Date());
         videoDataViewingRatioEntity.setUpdateDate(new Date());

         videoDataViewingRatioService.save(videoDataViewingRatioEntity);

         VideoDataViewingRatioInfoVo videoDataViewingRatioInfoVo = new VideoDataViewingRatioInfoVo();
         BeanUtils.copyProperties(videoDataViewingRatioEntity, videoDataViewingRatioInfoVo);

         return videoDataViewingRatioInfoVo;
     }

    @Override
    public void update(VideoDataViewingRatioBo videoDataViewingRatioBo) {

        VideoDataViewingRatioEntity videoDataViewingRatioEntity = new VideoDataViewingRatioEntity();
        BeanUtils.copyProperties(videoDataViewingRatioBo, videoDataViewingRatioEntity);
        videoDataViewingRatioEntity.setUpdateDate(new Date());

        videoDataViewingRatioService.updateById(videoDataViewingRatioEntity);
    }

    @Override
    public void deleteById(Long id) {

        videoDataViewingRatioService.removeById(id);
    }


}

