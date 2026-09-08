package com.jiuyu.replay.power.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.power.entity.UserTokenEntity;
import com.jiuyu.replay.power.vo.UserCacheVo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户Token表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
public interface UserTokenService extends IService<UserTokenEntity> {

    /**
     * 根据token获取用户信息
     *
     * @param token 用户token
     * @return 用户缓存信息，如果token过期则返回null
     */
    UserCacheVo getUserByToken(String token);

    /**
     * 更新Token过期时间
     *
     * @param token      用户token
     * @param expireTime 过期时间 为null时不进行更新
     */
    void updateUserTokenExpireTimeByToken(String token, LocalDateTime expireTime);

    /**
     * 批量删除用户token
     *
     * @param batchTokens 用户tokens
     */
    void batchRemoveToken(List<String> batchTokens);

    /**
     * 删除用户Token根据用户id
     *
     * @param userId 用户id
     */
    void removeTokenByUserId(Long userId);

    /**
     * 保存用户token信息
     *
     * @param token       用户token
     * @param userCacheVo 用户缓存信息
     * @param expireTime  过期时间
     */
    void saveUserToken(String token, UserCacheVo userCacheVo, LocalDateTime expireTime);


    /**
     * 更新用户信息根据用户id
     *
     * @param userId      用户id
     * @param userCacheVo 用户缓存信息
     * @param expireTime  过期时间
     */
    void updateUserInfoByUserId(Long userId, UserCacheVo userCacheVo, LocalDateTime expireTime);

    /**
     * 删除用户token根据token
     *
     * @param token 用户token
     */
    void removeTokenByToken(String token);

    /**
     * 查询用户token信息集合根据用户id
     *
     * @param userId 用户id
     * @return
     */
    List<UserTokenEntity> listUserTokenByUserId(Long userId);

    /**
     * 删除用户token信息根据用户id和超时访问时间
     *
     * @param userId      用户id
     * @param timeoutTime 超时访问时间
     */
    void removeTokenByUserIdAndLastAccessTime(Long userId, LocalDateTime timeoutTime);
}
