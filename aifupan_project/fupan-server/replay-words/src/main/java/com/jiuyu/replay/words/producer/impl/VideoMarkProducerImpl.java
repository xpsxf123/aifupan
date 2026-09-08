package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.VideoMarkListVo;
import com.jiuyu.replay.words.vo.VideoMarkInfoVo;
import com.jiuyu.replay.words.bo.VideoMarkBo;
import com.jiuyu.replay.words.bo.VideoMarkListBo;
import com.jiuyu.replay.words.repository.service.VideoMarkService;
import com.jiuyu.replay.words.entity.VideoMarkEntity;
import com.jiuyu.replay.words.producer.VideoMarkProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 视频标记
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Service
public class VideoMarkProducerImpl implements VideoMarkProducer {

    @Resource
    private VideoMarkService videoMarkService;


    @Override
    public PageUtils<VideoMarkListVo> queryPage(VideoMarkListBo videoMarkListBo) {
        QueryWrapper<VideoMarkEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(videoMarkListBo.getKeyword())){
            wrapper.like("name", videoMarkListBo.getKeyword());
        }

        IPage<VideoMarkEntity> iPage = videoMarkService.page(new Query<VideoMarkEntity>().getPage(videoMarkListBo.getPage(), videoMarkListBo.getLimit()), wrapper);

        PageUtils<VideoMarkListVo> pageUtils = new PageUtils<>(videoMarkListBo.getPage(), videoMarkListBo.getLimit(), iPage);

        List<VideoMarkEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<VideoMarkListVo> vos = records.stream().map(item -> {
                VideoMarkListVo videoMarkVo = new VideoMarkListVo();
                BeanUtils.copyProperties(item, videoMarkVo);
                return videoMarkVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public VideoMarkInfoVo info(Long id) {

        VideoMarkEntity videoMarkEntity = videoMarkService.getById(id);
        if(videoMarkEntity != null) {
            VideoMarkInfoVo videoMarkInfoVo = new VideoMarkInfoVo();
            BeanUtils.copyProperties(videoMarkEntity, videoMarkInfoVo);
            return videoMarkInfoVo;
        }

        return null;
    }

    /**
     * 新增视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
     public VideoMarkInfoVo save(VideoMarkBo videoMarkBo) {

         VideoMarkEntity videoMarkEntity = new VideoMarkEntity();
         BeanUtils.copyProperties(videoMarkBo, videoMarkEntity);
         videoMarkEntity.setId(SnowflakeManager.nextValue());
         videoMarkEntity.setCreateDate(new Date());
         videoMarkEntity.setUpdateDate(new Date());

         videoMarkService.save(videoMarkEntity);

         VideoMarkInfoVo videoMarkInfoVo = new VideoMarkInfoVo();
         BeanUtils.copyProperties(videoMarkEntity, videoMarkInfoVo);

         return videoMarkInfoVo;
     }

    /**
     * 修改视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
    public void update(VideoMarkBo videoMarkBo) {

        VideoMarkEntity videoMarkEntity = new VideoMarkEntity();
        BeanUtils.copyProperties(videoMarkBo, videoMarkEntity);
        videoMarkEntity.setUpdateDate(new Date());

        videoMarkService.updateById(videoMarkEntity);
    }

    /**
     * 删除视频标记
     * @param id 视频标记id
     * @return
     */
    public void deleteById(Long id) {

        videoMarkService.removeById(id);
    }

    @Override
    public List<VideoMarkListVo> listByUUID(String uuid) {

        List<VideoMarkEntity> videoMarkEntities = this.videoMarkService.list(new QueryWrapper<VideoMarkEntity>().eq("file_uuid", uuid));

        if(videoMarkEntities != null && videoMarkEntities.size() > 0) {
            List<VideoMarkListVo> videoMarkListVos = videoMarkEntities.stream().map(item -> {
                VideoMarkListVo videoMarkListVo = new VideoMarkListVo();
                BeanUtils.copyProperties(item, videoMarkListVo);
                return videoMarkListVo;
            }).toList();

            return videoMarkListVos;
        }

        return null;
    }


}

