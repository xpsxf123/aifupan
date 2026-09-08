package com.jiuyu.replay.power.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.power.entity.UserLoginInfoEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户登录信息表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
public interface UserLoginInfoService extends IService<UserLoginInfoEntity> {

    /**
     * 更新用户登录信息的最后请求时间和过期时间
     *
     * @param userId     用户ID
     * @param token      用户token
     * @param expireTime 过期时间 为null不进行更新
     */
    void updateUserLoginInfoExpireTimeByUserIdAndToken(Long userId, String token, LocalDateTime expireTime);

    /**
     * 批量删除用户登录信息token
     *
     * @param batchTokens 用户token集合
     */
    void batchRemoveToken(List<String> batchTokens);

    /**
     * 保存用户登录信息
     *
     * @param userId      用户id
     * @param token       用户token
     * @param source      来源
     * @param fingerprint 指纹
     * @param expireTime  过期时间
     */
    void saveUserLoginInfo(Long userId, String token, String source, String fingerprint, LocalDateTime expireTime);

    /**
     * 删除用户登录信息根据用户id
     *
     * @param userId 用户id
     */
    void removeUserLoginInfoByUserId(Long userId);

    /**
     * 删除用户登录信息根据用户id
     *
     * @param userId      用户id
     * @param timeoutTime 超时未访问时间
     */
    void removeUserLoginInfoByUserIdAndLastRequestTime(Long userId, LocalDateTime timeoutTime);

    /**
     * 更新用户过期时间根据用户id
     *
     * @param userId     用户id
     * @param expireTime 过期时间
     */
    void updateUserLoginInfoExpireTimeByUserId(Long userId, LocalDateTime expireTime);

    /**
     * 删除用户信息根据token
     *
     * @param token 用户token
     */
    void removeUserLoginInfoByToken(String token);

    /**
     * 获取用户登录信息根据用户id和token
     *
     * @param userId 用户id
     * @param token  用户token
     * @return
     */
    UserLoginInfoEntity getUserLoginInfoByUserIdAndToken(Long userId, String token);
}
