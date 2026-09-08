package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.power.producer.CompanyProducer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 公司表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:42
 */
@Component
public class CompanyBll {

    @Resource
    private CompanyProducer companyProducer;

}

