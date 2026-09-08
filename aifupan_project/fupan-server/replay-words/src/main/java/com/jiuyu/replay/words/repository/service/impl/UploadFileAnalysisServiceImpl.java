package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.UploadFileAnalysisEntity;
import com.jiuyu.replay.words.repository.dao.UploadFileAnalysisDao;
import com.jiuyu.replay.words.repository.service.UploadFileAnalysisService;
import org.springframework.stereotype.Service;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Service("uploadFileAnalysisService")
public class UploadFileAnalysisServiceImpl extends ServiceImpl<UploadFileAnalysisDao, UploadFileAnalysisEntity> implements UploadFileAnalysisService {
}
