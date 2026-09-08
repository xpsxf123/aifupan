package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.CompanyEntity;
import com.jiuyu.replay.power.repository.dao.CompanyDao;
import com.jiuyu.replay.power.repository.service.CompanyService;
import org.springframework.stereotype.Service;



@Service("companyService")
public class CompanyServiceImpl extends ServiceImpl<CompanyDao, CompanyEntity> implements CompanyService {



}