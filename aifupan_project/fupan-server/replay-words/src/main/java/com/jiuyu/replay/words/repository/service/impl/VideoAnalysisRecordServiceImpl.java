package com.jiuyu.replay.words.repository.service.impl;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.VideoAnalysisRecordDao;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.jiuyu.replay.words.repository.service.VideoAnalysisRecordService;

import java.util.List;


@Service("videoAnalysisRecordService")
public class VideoAnalysisRecordServiceImpl extends ServiceImpl<VideoAnalysisRecordDao, VideoAnalysisRecordEntity> implements VideoAnalysisRecordService {

    @Resource
    private VideoAnalysisRecordDao videoAnalysisRecordDao;

}