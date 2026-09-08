package com.jiuyu.replay.power.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.entity.UserTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户Token表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Mapper
public interface UserTokenDao extends BaseMapper<UserTokenEntity> {

    /**
     * 更新Token过期时间
     *
     * @param token      用户token
     * @param expireTime 过期时间 为null不进行更新
     */
    void updateUserTokenExpireTimeByToken(@Param("token") String token, @Param("expireTime") LocalDateTime expireTime);
}
