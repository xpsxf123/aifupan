package com.jiuyu.replay.common.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.common.repository.dao.FileDao;
import com.jiuyu.replay.common.entity.FileEntity;
import com.jiuyu.replay.common.repository.service.FileService;


@Service("fileService")
public class FileServiceImpl extends ServiceImpl<FileDao, FileEntity> implements FileService {



}