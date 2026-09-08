package com.jiuyu.replay.words.producer.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordBo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordListBo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.jiuyu.replay.words.producer.UploadFileAnalysisProducer;
import com.jiuyu.replay.words.producer.UploadFileProducer;
import com.jiuyu.replay.words.producer.VideoAnalysisRecordProducer;
import com.jiuyu.replay.words.repository.service.VideoAnalysisRecordService;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 视频的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Service
public class VideoAnalysisRecordProducerImpl implements VideoAnalysisRecordProducer {


    @Resource
    private VideoAnalysisRecordService videoAnalysisRecordService;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private UploadFileAnalysisProducer uploadFileAnalysisProducer;


    @Override
    public PageUtils<VideoAnalysisRecordListVo> queryPage(VideoAnalysisRecordListBo videoAnalysisRecordListBo) {
        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(videoAnalysisRecordListBo.getKeyword())){
            wrapper.like("name", videoAnalysisRecordListBo.getKeyword());
        }

        IPage<VideoAnalysisRecordEntity> iPage = videoAnalysisRecordService.page(new Query<VideoAnalysisRecordEntity>().getPage(videoAnalysisRecordListBo.getPage(), videoAnalysisRecordListBo.getLimit()), wrapper);

        PageUtils<VideoAnalysisRecordListVo> pageUtils = new PageUtils<>(videoAnalysisRecordListBo.getPage(), videoAnalysisRecordListBo.getLimit(), iPage);

        List<VideoAnalysisRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<VideoAnalysisRecordListVo> vos = records.stream().map(item -> {
                VideoAnalysisRecordListVo videoAnalysisRecordVo = new VideoAnalysisRecordListVo();
                BeanUtils.copyProperties(item, videoAnalysisRecordVo);
                return videoAnalysisRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public VideoAnalysisRecordInfoVo info(Long id) {

        VideoAnalysisRecordEntity videoAnalysisRecordEntity = videoAnalysisRecordService.getById(id);
        if(videoAnalysisRecordEntity != null) {
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = new VideoAnalysisRecordInfoVo();
            BeanUtils.copyProperties(videoAnalysisRecordEntity, videoAnalysisRecordInfoVo);
            return videoAnalysisRecordInfoVo;
        }

        return null;
    }

    /**
     * 新增视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
     public VideoAnalysisRecordInfoVo save(VideoAnalysisRecordBo videoAnalysisRecordBo) {

         VideoAnalysisRecordEntity videoAnalysisRecordEntity = new VideoAnalysisRecordEntity();
         BeanUtils.copyProperties(videoAnalysisRecordBo, videoAnalysisRecordEntity);
//         videoAnalysisRecordEntity.setId(SnowflakeManager.nextValue());
         videoAnalysisRecordEntity.setCreateDate(new Date());
         videoAnalysisRecordEntity.setUpdateDate(new Date());

         videoAnalysisRecordService.save(videoAnalysisRecordEntity);

         VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = new VideoAnalysisRecordInfoVo();
         BeanUtils.copyProperties(videoAnalysisRecordEntity, videoAnalysisRecordInfoVo);

         return videoAnalysisRecordInfoVo;
     }

    /**
     * 修改视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    public void update(VideoAnalysisRecordBo videoAnalysisRecordBo) {

        VideoAnalysisRecordEntity videoAnalysisRecordEntity = new VideoAnalysisRecordEntity();
        BeanUtils.copyProperties(videoAnalysisRecordBo, videoAnalysisRecordEntity);
        videoAnalysisRecordEntity.setUpdateDate(new Date());

        videoAnalysisRecordService.updateById(videoAnalysisRecordEntity);
    }

    /**
     * 删除视频的分析记录
     * @param id 视频的分析记录id
     * @return
     */
    public void deleteById(Long id) {

        videoAnalysisRecordService.removeById(id);
    }

    @Override
    public int getLastVersion(String videoId) {

        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.orderByDesc("version");
        wrapper.last(" limit 1");
        VideoAnalysisRecordEntity videoAnalysisRecordEntity = this.videoAnalysisRecordService.getOne(wrapper);
        if(videoAnalysisRecordEntity != null) {
            return videoAnalysisRecordEntity.getVersion() + 1;
        }
        return 0;
    }

    @Override
    public VideoAnalysisRecordInfoVo getLastInfo(String videoId) {

        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.orderByDesc("version");
        wrapper.last(" limit 1");
        VideoAnalysisRecordEntity videoAnalysisRecordEntity = this.videoAnalysisRecordService.getOne(wrapper);
        if(videoAnalysisRecordEntity != null) {
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = new VideoAnalysisRecordInfoVo();
            BeanUtils.copyProperties(videoAnalysisRecordEntity, videoAnalysisRecordInfoVo);
            return videoAnalysisRecordInfoVo;
        }

        return null;
    }

    @Override
    public VideoAnalysisRecordInfoVo infoLastByVideoIdAndTradeId(String videoId, Long tradeId) {

        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("trade_id", tradeId);
        wrapper.orderByDesc("version");
        wrapper.last(" limit 1");
        VideoAnalysisRecordEntity videoAnalysisRecordEntity = this.videoAnalysisRecordService.getOne(wrapper);
        if(videoAnalysisRecordEntity != null) {
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = new VideoAnalysisRecordInfoVo();
            BeanUtils.copyProperties(videoAnalysisRecordEntity, videoAnalysisRecordInfoVo);
            return videoAnalysisRecordInfoVo;
        }

        return null;
    }

    @Override
    public VideoAnalysisRecordInfoVo infoByVideoIdAndVersion(String videoId, int version) {

        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("version", version);
        wrapper.last(" limit 1");
        VideoAnalysisRecordEntity videoAnalysisRecordEntity = this.videoAnalysisRecordService.getOne(wrapper);
        if(videoAnalysisRecordEntity != null) {
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = new VideoAnalysisRecordInfoVo();
            BeanUtils.copyProperties(videoAnalysisRecordEntity, videoAnalysisRecordInfoVo);
            return videoAnalysisRecordInfoVo;
        }

        return null;

    }

    @Override
    public R<List<SentenceMarkVo>> selectAnalysisByVideoId(String videoId, Long tradeId) {
        if (videoId != null && tradeId != null) {
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.infoLastByVideoIdAndTradeId(videoId, tradeId);
            if(videoAnalysisRecordInfoVo != null) {
                String fileContent = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName());
                List<SentenceMarkVo> sentenceMarkVos = new ArrayList<>();
                if (!StringUtils.isEmpty(fileContent)){
                    sentenceMarkVos = JSON.parseArray(fileContent, SentenceMarkVo.class);
                    sentenceMarkVos.sort((o1,o2) -> Integer.compare(o1.getCurrentSort(),o2.getCurrentSort()));
                }
                return R.ok(sentenceMarkVos);
            }
        }
        return null;
    }

    @Override
    public void saveNewFilePath(Long id, String storeFileNameNew) {
        VideoAnalysisRecordEntity videoAnalysisRecordEntity = new VideoAnalysisRecordEntity();
        videoAnalysisRecordEntity.setId(id);
        videoAnalysisRecordEntity.setStoreFileNameNew(storeFileNameNew);
        videoAnalysisRecordEntity.setUpdateDate(new Date());
        this.videoAnalysisRecordService.updateById(videoAnalysisRecordEntity);
    }

    @Override
    public void saveCosKey(Long id, String cosSaveKey) {
        VideoAnalysisRecordEntity videoAnalysisRecordEntity = new VideoAnalysisRecordEntity();
        videoAnalysisRecordEntity.setId(id);
        videoAnalysisRecordEntity.setStoreFileOssKey(cosSaveKey);
        videoAnalysisRecordEntity.setUpdateDate(new Date());
        this.videoAnalysisRecordService.updateById(videoAnalysisRecordEntity);
    }


    /**
     * 获取昨天的全部分析记录
     * @return
     */
    @Override
    public List<VideoAnalysisRecordEntity> yesterdayDateRecord() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 获取昨天的日期
        LocalDateTime yesterday = now.minusDays(1);
        // 获取昨天的开始时间（即 00:00:00）
        LocalDateTime startOfYesterday = yesterday.withHour(0).withMinute(0).withSecond(0).withNano(0);
        // 设置时间为昨天的最后一秒（即 23:59:59）
        LocalDateTime endOfYesterday = yesterday.withHour(23).withMinute(59).withSecond(59).withNano(0);
        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.between("create_date", startOfYesterday, endOfYesterday);
        List<VideoAnalysisRecordEntity> recordEntities = this.videoAnalysisRecordService.list(wrapper);
        if (recordEntities != null && recordEntities.size() > 0) {
            return recordEntities;
        }
        return null;
    }

    @Override
    public List<VideoAnalysisRecordInfoVo> listByVideoIds(Collection<String> videoIds) {

        QueryWrapper<VideoAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.in("video_id", videoIds);

        List<VideoAnalysisRecordEntity> videoAnalysisRecordEntities = this.videoAnalysisRecordService.list(wrapper);

        if(videoAnalysisRecordEntities != null && videoAnalysisRecordEntities.size() > 0) {
            List<VideoAnalysisRecordInfoVo> videoAnalysisRecordInfoVos = videoAnalysisRecordEntities.stream().map(item -> {
                VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = new VideoAnalysisRecordInfoVo();
                BeanUtils.copyProperties(item, videoAnalysisRecordInfoVo);
                return videoAnalysisRecordInfoVo;
            }).collect(Collectors.toList());

            return videoAnalysisRecordInfoVos;
        }

        return null;
    }
}
