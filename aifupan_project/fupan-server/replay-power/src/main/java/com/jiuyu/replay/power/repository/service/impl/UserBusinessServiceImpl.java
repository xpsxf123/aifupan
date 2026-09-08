package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.UserBusinessEntity;
import com.jiuyu.replay.power.repository.dao.UserBusinessDao;
import com.jiuyu.replay.power.repository.service.UserBusinessService;
import org.springframework.stereotype.Service;

/**
 * 用户业务表
 *
 * @author jxy
 * @date 2024-07-08
 */
@Service("userBusinessService")
public class UserBusinessServiceImpl extends ServiceImpl<UserBusinessDao, UserBusinessEntity> implements UserBusinessService {

}
