package com.jiuyu.replay.words.repository.service.impl;

import com.jiuyu.replay.words.bo.ClientAiFavBo;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.ClientAiFavDao;
import com.jiuyu.replay.words.entity.ClientAiFavEntity;
import com.jiuyu.replay.words.repository.service.ClientAiFavService;


@Service("clientAiFavService")
public class ClientAiFavServiceImpl extends ServiceImpl<ClientAiFavDao, ClientAiFavEntity> implements ClientAiFavService {

    @Override
    public int hasDelete(ClientAiFavBo clientAiFavBo) {
        return baseMapper.hasDelete(clientAiFavBo);
    }
}