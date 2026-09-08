package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.DataScreenshotDao;
import com.jiuyu.replay.words.entity.DataScreenshotEntity;
import com.jiuyu.replay.words.repository.service.DataScreenshotService;


@Service("dataScreenshotService")
public class DataScreenshotServiceImpl extends ServiceImpl<DataScreenshotDao, DataScreenshotEntity> implements DataScreenshotService {



}