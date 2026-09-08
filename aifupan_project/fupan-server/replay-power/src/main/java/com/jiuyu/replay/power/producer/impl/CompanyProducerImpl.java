package com.jiuyu.replay.power.producer.impl;


import com.jiuyu.replay.power.entity.CompanyEntity;
import com.jiuyu.replay.power.producer.CompanyProducer;
import com.jiuyu.replay.power.repository.service.CompanyService;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.vo.CompanyVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


/**
 * 公司表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:42
 */
@Service
public class CompanyProducerImpl implements CompanyProducer {

    @Resource
    private CompanyService companyService;
    @Resource
    private UserService userService;


    @Override
    public CompanyVo info(Long id) {
        CompanyEntity entity = companyService.getById(id);
        if (entity != null) {
            CompanyVo companyVo = new CompanyVo();
            BeanUtils.copyProperties(entity, companyVo);
            return companyVo;
        }
        return null;
    }
}

