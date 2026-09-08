package com.jiuyu.replay.words.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.AnchorUrlDetailsDao;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import com.jiuyu.replay.words.repository.service.AnchorUrlDetailsService;

import java.util.List;


/**
 * @author LJ
 */
@Service("anchorUrlDetailsService")
public class AnchorUrlDetailsServiceImpl extends ServiceImpl<AnchorUrlDetailsDao, AnchorUrlDetailsEntity> implements AnchorUrlDetailsService {

    @Override
    public List<String> secUidListByKeywordStatus(Integer duration, Integer limit) {
        return baseMapper.secUidListByKeywordStatus(duration, limit);
    }
}

