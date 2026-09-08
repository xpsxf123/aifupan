package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.entity.UserPropertyDetailsEntity;

import java.util.Date;

/**
 * 用户资产消费记录表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface UserPropertyDetailsService extends IService<UserPropertyDetailsEntity> {


    /**
     * 统计租户的消耗总token数
     *
     * @param mainUserId       主用户ID
     * @param sinceCreateDate 创建时间
     */
    Long sumTotalTokensByTenant(long mainUserId, Date sinceCreateDate);
}

