package com.jiuyu.replay.order.repository.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.order.repository.dao.UserPropertyDetailsDao;
import com.jiuyu.replay.order.entity.UserPropertyDetailsEntity;
import com.jiuyu.replay.order.repository.service.UserPropertyDetailsService;

import java.math.BigDecimal;
import java.util.Date;


@Service("userPropertyDetailsService")
public class UserPropertyDetailsServiceImpl extends ServiceImpl<UserPropertyDetailsDao, UserPropertyDetailsEntity> implements UserPropertyDetailsService {


    /**
     * 统计租户的消耗总token数
     *
     * @param mainUserId      主用户ID
     * @param sinceCreateDate 创建时间
     */
    @Override
    public Long sumTotalTokensByTenant(long mainUserId, Date sinceCreateDate) {
        Long tokens = super.getBaseMapper().sumTotalTokensByTenant(mainUserId, sinceCreateDate);
        if (tokens == null) {
            return 0L;
        }

        return tokens;
    }
}
