package com.jiuyu.replay.power.repository.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.RoleEntity;
import com.jiuyu.replay.power.repository.dao.RoleDao;
import com.jiuyu.replay.power.repository.service.RoleService;
import org.springframework.stereotype.Service;


@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleDao, RoleEntity> implements RoleService {

}