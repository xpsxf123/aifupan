package com.jiuyu.governance.plugins.oauth.server;


import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 数据库持久化token存储
 *
 * @author HeHui
 * @date 2026-02-20 17:29
 */
public interface DbTokenStorage {

    /**
     * 新登录
     * 生成一个新的登录日志并新增租户ID关联表数据
     *
     * @param userId      用户id
     * @param userType    用户类型
     * @param loginIp     登录ip
     * @param tenantId    租户id
     * @param accessToken 访问token
     * @param userAgent   登录设备信息
     * @param expireTime  token过期时间
     */
    void newLogin(long userId, String userType, String loginIp, long tenantId, String accessToken, String userAgent, LocalDateTime expireTime);


    /**
     * 登出 设置为失效
     *
     * @param accessToken 访问token
     */
    void logout(String accessToken);

    /**
     * 强制登出
     * 获取当前用户有效的登录token 并设置为失效
     *
     * @param userType 用户类型
     * @param userId   用户id
     *
     * @return 登出的token
     */
    default List<String> logoutAndGetTokens(String userType, long userId) {
        return this.logoutAndGetTokens(userType, List.of(userId));
    }

    /**
     * 批量强制登出
     * 获取当前用户有效的登录token 并设置为失效
     *
     * @param userType 用户类型
     * @param userIds  用户id
     *
     * @return 登出的token
     */
    List<String> logoutAndGetTokens(String userType, Collection<Long> userIds);

    /**
     * 更新token过期时间
     *
     * @param accessToken 访问token
     * @param expireTime  过期时间
     */
    void updateExpire(String accessToken, LocalDateTime expireTime);
}
