package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.UserPropertyDetailsEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;

/**
 * 用户资产消费记录表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Mapper
public interface UserPropertyDetailsDao extends BaseMapper<UserPropertyDetailsEntity> {

    /**
     * 统计租户的消耗总token数
     *
     * @param mainUserId       主用户ID
     * @param sinceCreateDate 创建时间
     */
    Long sumTotalTokensByTenant(@Param("mainUserId") long mainUserId, @Param("sinceCreateDate") Date sinceCreateDate);
}
