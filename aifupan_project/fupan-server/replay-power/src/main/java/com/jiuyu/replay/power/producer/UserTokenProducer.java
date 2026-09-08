package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.power.entity.UserLoginInfoEntity;
import com.jiuyu.replay.power.vo.UserCacheVo;

import java.util.List;

/**
 * 用户Token服务接口，统一处理用户登录信息的获取和存储
 * 支持Redis降级时从数据库读取数据
 *
 * @author RayChou
 * @date 2025/7/3
 */
public interface UserTokenProducer {

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
     * @param userVo     用户缓存信息
     * @param token      用户token
     * @param isHeartbeat 是否是心跳接口
     * @param isExcludePath 是否是排除路径
     */
    void updateTokenExpire(UserCacheVo userVo, String token, Boolean isHeartbeat, Boolean isExcludePath);

    /**
     * 获取用户登录信息集合根据用户id
     *
     * @param userId
     * @return
     */
    List<UserLoginInfoEntity> listUserLoginInfoByUserId(Long userId);

    /**
     * 批量删除用户token
     *
     * @param userId      用户Id
     * @param batchTokens 用户token集合
     */
    void batchRemoveTokenByUserId(Long userId, List<String> batchTokens);

    /**
     * 保存用户登录信息
     *
     * @param userCacheVo 用户缓存信息
     * @param token       用户token
     * @param loginIp     登录IP
     * @param source      登录来源
     * @param fingerprint 设备指纹
     */
    void saveUserLoginInfo(UserCacheVo userCacheVo, String token, String loginIp, String source, String fingerprint);

    /**
     * 删除用户token根据用户Id
     *
     * @param userId 用户id
     */
    void removeUserTokenByUserId(Long userId);

    /**
     * 更新用户基础信息
     *
     * @param userCacheVo 用户缓存信息
     */
    void updateBaseUserInfo(UserCacheVo userCacheVo);

    /**
     * 删除用户token
     *
     * @param token  用户token
     * @param userId 用户Id
     */
    void removeUserToken(String token, Long userId);

    /**
     * 修改用户状态 （冻结或解冻）
     *
     * @param userId 用户id
     * @param status 状态
     */
    void updateUserStatusByUserId(Long userId, Integer status);

    /**
     * 将Redis中的token信息全部加载到数据库
     *
     * @return 同步结果信息
     */
    String loadRedisTokensToDatabase();

    /**
     * 获取用户登录信息根据用户id和token
     *
     * @param userId 用户id
     * @param token  用户token
     */
    UserLoginInfoEntity getUserLoginInfoByUserIdAndToken(Long userId, String token);

    /**
     * 删除超过多少天未访问的token
     *
     * @param userId 用户id
     * @param day    天数
     */
    void removeUserTokenLastRequestDateAfterDayByUserId(Long userId, int day);

    /**
     * Redis恢复后同步降级期间的token相关数据到Redis
     * 将数据库中在降级期间更新的token信息同步回Redis
     *
     * @return 同步结果信息
     */
    String syncDegradedTokenDataToRedis();

    /**
     * 更新用户手机号缓存
     *
     * @param userId 用户id
     * @param newMobile 新手机号
     */
    void updateUserMobileCache(long userId, String newMobile);
}
