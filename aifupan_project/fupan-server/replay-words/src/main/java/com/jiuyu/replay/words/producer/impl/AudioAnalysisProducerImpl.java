package com.jiuyu.replay.words.producer.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AudioAnalysisBo;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.jiuyu.replay.words.producer.AudioAnalysisProducer;
import com.jiuyu.replay.words.repository.service.AudioAnalysisService;
import com.jiuyu.replay.words.repository.service.VideoAnalysisRecordService;
import com.jiuyu.replay.words.vo.AudioAnalysisVo;
import com.jiuyu.replay.words.vo.AudioAnalysissVO;
import com.jiuyu.replay.words.vo.OnlineAnalysisItemVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AudioAnalysisProducerImpl implements AudioAnalysisProducer {

    @Resource
    AudioAnalysisService audioAnalysisService;
    @Resource
    private VideoAnalysisRecordService videoAnalysisRecordService;

    /**
     * 保存直播视频分析内容
     * @param audioAnalysis
     */
    @Override
    @Transactional
    public void saveBatchs(List<AudioAnalysisEntity> audioAnalysis) {
        for (AudioAnalysisEntity audioAnalysisEntity:audioAnalysis){
                audioAnalysisService.save(audioAnalysisEntity);
        }
    }

    @Override
    public List<AudioAnalysisEntity> listByVideoiId(Long Videoid) {
        QueryWrapper<AudioAnalysisEntity> audioAnalysisEntityQueryWrapper = new QueryWrapper<>();
        audioAnalysisEntityQueryWrapper.eq("videoi_id", Videoid);
        List<AudioAnalysisEntity> list = audioAnalysisService.list(audioAnalysisEntityQueryWrapper);
        return list;
    }

    @Override
    public R<List<AudioAnalysisEntity>> selectByVideoId(AudioAnalysisBo audioAnalysisBo) {

       QueryWrapper<AudioAnalysisEntity> wrapper =  new QueryWrapper<>();
        wrapper.eq("video_id", audioAnalysisBo.getVideoId());
        if(audioAnalysisBo.getTradeId()!=null){
            wrapper.eq("trade_id", audioAnalysisBo.getTradeId());
        }

        List<AudioAnalysisEntity> list = audioAnalysisService.list(wrapper);

        Optional<AudioAnalysisEntity> maxVersionEntity = list.stream()
                .max(Comparator.comparingInt(AudioAnalysisEntity::getVersion));

        List<AudioAnalysisEntity> collect = list.stream()
                .filter(entity -> entity.getVersion().equals(maxVersionEntity.get().getVersion()))
                .toList();

        List<AudioAnalysisEntity> Entitylist = collect.stream().map(item -> {
            AudioAnalysisEntity audioAnalysisEntity = new AudioAnalysisEntity();
            BeanUtils.copyProperties(item, audioAnalysisEntity);
            JSONObject jsonObject = JSONObject.parseObject(item.getDataJson());
            String content = jsonObject.getString("content");
            audioAnalysisEntity.setDataJson(content);
            return audioAnalysisEntity;
        }).toList();
        List<AudioAnalysisEntity> entities = new ArrayList<>(Entitylist);
        Collections.sort(entities, new Comparator<AudioAnalysisEntity>() {
            @Override
            public int compare(AudioAnalysisEntity o1, AudioAnalysisEntity o2) {
                return Integer.compare(o1.getParagraph(), o2.getParagraph());
            }
        });
        return R.ok(entities);
    }

    @Override
    public R saveAudioAnalysis(List<AudioAnalysisVo> audioAnalysisVo) {
        QueryWrapper<AudioAnalysisEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("video_id",audioAnalysisVo.get(0).getVideoId())
                    .orderByDesc("num")
                    .last("LIMIT 1");

        AudioAnalysisEntity result = audioAnalysisService.getOne(queryWrapper);
        if(audioAnalysisVo !=null && audioAnalysisVo.size()>0){
            List<AudioAnalysisEntity> list = audioAnalysisVo.stream().map(item -> {
                AudioAnalysisEntity audioAnalysisEntity = new AudioAnalysisEntity();
                BeanUtils.copyProperties(item, audioAnalysisEntity);
                return audioAnalysisEntity;
            }).toList();
            audioAnalysisService.saveBatch(list);
        }
        return R.ok();
    }

    /**
     * 客户端根据视频Id和行业ID查询该视频的分析内容
     * @param videoId
     * @return
     */
    @Override
    public R<List<AudioAnalysissVO>> selectByVideoIdOrTradeId(AudioAnalysisBo videoId) {

        QueryWrapper<AudioAnalysisEntity> wrapper =  new QueryWrapper<>();
        wrapper.eq("video_id", videoId.getVideoId());
        wrapper.eq("trade_id", videoId.getTradeId());
        List<AudioAnalysisEntity> list = audioAnalysisService.list(wrapper);

        if(list != null && list.size() > 0) {
            Optional<AudioAnalysisEntity> maxVersionEntity = list.stream()
                    .max(Comparator.comparingInt(AudioAnalysisEntity::getVersion));

            List<AudioAnalysisEntity> collect = list.stream()
                    .filter(entity -> entity.getVersion().equals(maxVersionEntity.get().getVersion()))
                    .toList();
            if(collect!=null && collect.size()>0){
                List<AudioAnalysissVO> list1 = collect.stream().map(item -> {
                    AudioAnalysissVO audioAnalysissVO = new AudioAnalysissVO();
                    BeanUtils.copyProperties(item, audioAnalysissVO);
                    return audioAnalysissVO;
                }).toList();
                return R.ok(list1);
            }
        }


        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAnalysis(String videoId) {

        // 删除视频的分析数据（旧）
        this.audioAnalysisService.remove(new QueryWrapper<AudioAnalysisEntity>().eq("video_id", videoId));

        // 删除视频的分析记录
        this.videoAnalysisRecordService.remove(new QueryWrapper<VideoAnalysisRecordEntity>().eq("video_id", videoId));
    }

    @Override
    public List<OnlineAnalysisItemVo> listByVideoIdAndTradeId(String videoId, Long tradeId) {
        QueryWrapper<AudioAnalysisEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("trade_id", tradeId);
        List<AudioAnalysisEntity> audioAnalysisEntities = this.audioAnalysisService.list(wrapper);
        if(audioAnalysisEntities != null && audioAnalysisEntities.size() > 0) {
            Optional<AudioAnalysisEntity> maxAnalysisEntity = audioAnalysisEntities.stream().max(Comparator.comparingInt(AudioAnalysisEntity::getVersion));
            List<AudioAnalysisEntity> analysisEntityList = audioAnalysisEntities.stream()
                    .filter(item -> item.getVersion().equals(maxAnalysisEntity.get().getVersion()))
                    .sorted(Comparator.comparingInt(AudioAnalysisEntity::getParagraph)).toList();

            List<OnlineAnalysisItemVo> analysisItemVoList = analysisEntityList.stream().map(item -> {
                OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                onlineAnalysisItemVo.setId(item.getId());
                onlineAnalysisItemVo.setFileUuid(item.getVideoId());
                onlineAnalysisItemVo.setTradeId(Long.valueOf(item.getTradeId()));
                onlineAnalysisItemVo.setParagraph(item.getParagraph());
                onlineAnalysisItemVo.setStatus(item.getStatus());
                onlineAnalysisItemVo.setDataJson(item.getDataJson());
                return onlineAnalysisItemVo;
            }).toList();
            return analysisItemVoList;
        }
        return null;
    }

    @Override
    public List<AudioAnalysisVo> listAnalysisOneByVideoId(String videoId) {
        List<AudioAnalysisEntity> audioAnalysisEntities = this.audioAnalysisService.list(
                new QueryWrapper<AudioAnalysisEntity>().eq("video_id", videoId).eq("version", 0));
        if(audioAnalysisEntities != null && audioAnalysisEntities.size() > 0) {
            List<AudioAnalysisVo> audioAnalysisVos = audioAnalysisEntities.stream().map(item -> {
                AudioAnalysisVo audioAnalysisVo = new AudioAnalysisVo();
                BeanUtils.copyProperties(item, audioAnalysisVo);
                return audioAnalysisVo;
            }).sorted(Comparator.comparingInt(AudioAnalysisVo::getParagraph)).toList();

            return audioAnalysisVos;
        }
        return null;
    }

}
