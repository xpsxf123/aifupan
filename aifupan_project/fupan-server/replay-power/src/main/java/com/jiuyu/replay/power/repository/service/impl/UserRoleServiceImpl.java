package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.UserRoleEntity;
import com.jiuyu.replay.power.repository.dao.UserRoleDao;
import com.jiuyu.replay.power.repository.service.UserRoleService;
import org.springframework.stereotype.Service;

@Service("userRoleService")
public class UserRoleServiceImpl extends ServiceImpl<UserRoleDao, UserRoleEntity> implements UserRoleService {


}