package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.entity.UserLoginInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户登录信息表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Mapper
public interface UserLoginInfoDao extends BaseMapper<UserLoginInfoEntity> {

    /**
     * 更新用户登录信息的最后请求时间和过期时间
     *
     * @param userId     用户ID
     * @param token      用户token
     * @param expireTime 过期时间 为null不进行更新
     */
    void updateUserLoginInfoExpireTimeByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token, @Param("expireTime") LocalDateTime expireTime);
}
