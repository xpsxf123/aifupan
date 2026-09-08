package com.jiuyu.replay.common.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.entity.ComputerConfigEntity;
import com.jiuyu.replay.common.repository.dao.ComputerConfigDao;
import com.jiuyu.replay.common.repository.service.ComputerConfigService;
import org.springframework.stereotype.Service;

@Service
public class ComputerConfigServiceImpl
        extends ServiceImpl<ComputerConfigDao, ComputerConfigEntity>
        implements ComputerConfigService {
}
