package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.PackageDao;
import com.jiuyu.replay.order.entity.PackageEntity;
import com.jiuyu.replay.order.repository.service.PackageService;


@Service("packageService")
public class PackageServiceImpl extends ServiceImpl<PackageDao, PackageEntity> implements PackageService {



}