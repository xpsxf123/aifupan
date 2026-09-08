package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.words.bo.SyncContrastListBo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.SyncContrastDao;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.repository.service.SyncContrastService;


@Service("syncContrastService")
public class SyncContrastServiceImpl extends ServiceImpl<SyncContrastDao, SyncContrastEntity> implements SyncContrastService {

    @Override
    public Page<SyncContrastListVo> pageSyncContrastNew(SyncContrastListBo syncContrastListBo) {
        return baseMapper.pageSyncContrastNew(new Page<>(syncContrastListBo.getPage(), syncContrastListBo.getLimit()), syncContrastListBo);
    }
}