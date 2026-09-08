package com.jiuyu.governance.plugins.oauth.server;


import com.jiuyu.framework.oauth.AccessUser;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 身份认证存储
 *
 * @author HeHui
 * @date 2026-02-20 17:04
 */
public interface AuthenticationTokenStorage {


    /**
     * 登录
     * 注意此操作不管多设备登录情况，多设备登录请在业务逻辑中处理
     *
     * @param userType  用户类型
     * @param userId    用户id
     * @param username  用户名
     * @param loginIp   登录ip
     * @param tenantId  租户id
     * @param userAgent 登录设备信息
     * @param metadata  元数据
     *
     * @return 登录token
     */
    String login(String userType, long userId, String username, String loginIp, long tenantId, String userAgent, Map<String, String> metadata);


    /**
     * 登出
     *
     * @param accessToken 访问token
     *
     * @return 是否成功
     */
    boolean logout(String accessToken);


    /**
     * 强制登出 指定用户所有设备
     *
     * @param userType 用户类型
     * @param userId   用户id
     *
     * @return 是否成功
     */
    boolean logout(String userType, long userId);



    /**
     * 批量强制登出
     *
     * @param userType 用户类型
     * @param userIds  用户id
     *
     * @return 是否成功
     */
    boolean logout(String userType, Collection<Long> userIds);


    /**
     * Get User ID from Access Token / 从访问令牌获取用户ID
     * <p>
     * Extracts and returns the user ID associated with the provided access token.
     * 从提供的访问令牌中提取并返回关联的用户ID。
     * </p>
     *
     * @param accessToken The access token to analyze / 要分析的访问令牌
     *
     * @return containing the user ID if token is valid, empty otherwise /
     *     如果令牌有效则包含用户，否则为空
     */
    Optional<AccessUser> getUser(String accessToken);


    /**
     * Check if Access Token is Effective / 检查访问令牌是否有效
     * <p>
     * Validates whether the provided access token is still effective (not expired).
     * 验证提供的访问令牌是否仍然有效（未过期）。
     * </p>
     *
     * @param accessToken The access token to validate / 要验证的访问令牌
     *
     * @return containing true if token is valid, false otherwise /
     *     如果令牌有效则true否则false
     */
    boolean effective(String accessToken);
}
