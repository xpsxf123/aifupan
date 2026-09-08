package com.jiuyu.replay.common.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.entity.UserGrayscaleEntity;
import com.jiuyu.replay.common.repository.dao.UserGrayscaleDao;
import com.jiuyu.replay.common.repository.service.UserGrayscaleService;
import org.springframework.stereotype.Service;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 16:40
 */
@Service("userGrayscaleService")
public class UserGrayscaleServiceImpl extends ServiceImpl<UserGrayscaleDao, UserGrayscaleEntity> implements UserGrayscaleService {
}
