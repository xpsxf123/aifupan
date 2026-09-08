package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.power.vo.CompanyVo;

/**
 * 公司表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:42
 */
public interface CompanyProducer {

    /**
     * 查询公司
     * @param id
     * @return
     */
    CompanyVo info(Long id);

}

