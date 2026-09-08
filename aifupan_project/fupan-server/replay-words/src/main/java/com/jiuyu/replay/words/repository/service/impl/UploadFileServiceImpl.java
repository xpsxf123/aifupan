package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.UploadFileEntity;
import com.jiuyu.replay.words.repository.dao.UploadFilesDao;
import com.jiuyu.replay.words.repository.service.UploadFileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Service("uploadFileService")
public class UploadFileServiceImpl  extends ServiceImpl<UploadFilesDao, UploadFileEntity> implements UploadFileService {

}
