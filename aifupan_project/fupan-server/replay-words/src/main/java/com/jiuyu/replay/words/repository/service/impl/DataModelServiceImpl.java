package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.DataModelDao;
import com.jiuyu.replay.words.entity.DataModelEntity;
import com.jiuyu.replay.words.repository.service.DataModelService;


@Service("dataModelService")
public class DataModelServiceImpl extends ServiceImpl<DataModelDao, DataModelEntity> implements DataModelService {



}