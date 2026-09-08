package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.repository.dao.AudioAnalysisDao;
import com.jiuyu.replay.words.repository.service.AudioAnalysisService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service("audioAnalysisService")
public class AudioAnalysisServiceImpl extends ServiceImpl<AudioAnalysisDao, AudioAnalysisEntity> implements AudioAnalysisService {
}
