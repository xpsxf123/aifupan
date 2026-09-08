package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.UploadFileAnalysisRecordEntity;
import com.jiuyu.replay.words.repository.dao.UploadFileAnalysisRecordDao;
import com.jiuyu.replay.words.repository.service.UploadFileAnalysisRecordService;
import org.springframework.stereotype.Service;


@Service("uploadFileAnalysisRecordService")
public class UploadFileAnalysisRecordServiceImpl extends ServiceImpl<UploadFileAnalysisRecordDao, UploadFileAnalysisRecordEntity> implements UploadFileAnalysisRecordService {
}