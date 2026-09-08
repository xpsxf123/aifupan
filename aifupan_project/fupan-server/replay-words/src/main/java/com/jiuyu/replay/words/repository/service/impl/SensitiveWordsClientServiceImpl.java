package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.SensitiveWordsClientEntity;
import com.jiuyu.replay.words.repository.dao.SensitiveWordsClientDao;
import com.jiuyu.replay.words.repository.service.SensitiveWordsClientService;
import org.springframework.stereotype.Service;


@Service("sensitiveWordsClientService")
public class SensitiveWordsClientServiceImpl extends ServiceImpl<SensitiveWordsClientDao, SensitiveWordsClientEntity> implements SensitiveWordsClientService {



}