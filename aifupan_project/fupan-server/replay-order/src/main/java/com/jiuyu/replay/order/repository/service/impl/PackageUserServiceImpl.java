package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.PackageUserDao;
import com.jiuyu.replay.order.entity.PackageUserEntity;
import com.jiuyu.replay.order.repository.service.PackageUserService;


@Service("packageUserService")
public class PackageUserServiceImpl extends ServiceImpl<PackageUserDao, PackageUserEntity> implements PackageUserService {


}
