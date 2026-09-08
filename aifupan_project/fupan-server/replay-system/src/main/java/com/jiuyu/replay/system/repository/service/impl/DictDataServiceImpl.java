package com.jiuyu.replay.system.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.system.repository.dao.DictDataDao;
import com.jiuyu.replay.system.entity.DictDataEntity;
import com.jiuyu.replay.system.repository.service.DictDataService;


@Service("dictDataService")
public class DictDataServiceImpl extends ServiceImpl<DictDataDao, DictDataEntity> implements DictDataService {



}