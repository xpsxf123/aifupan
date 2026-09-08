package com.jiuyu.governance.business.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.mapper.EmployeeAccessTokenMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.EmployeeAccessToken;
import com.jiuyu.governance.common.utils.XXHash;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import com.jiuyu.governance.plugins.oauth.server.DbTokenStorage;
import com.jiuyu.governance.plugins.useragent.UserAgentHandler;
import com.jiuyu.governance.plugins.useragent.UserAgentInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 数据库存储accessToken
 *
 * @author HeHui
 * @date 2026-03-27 16:39
 */
@Service
public class AccessTokenDbStorage extends ServiceImpl<EmployeeAccessTokenMapper, EmployeeAccessToken> implements DbTokenStorage {

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
    @Override
    public void newLogin(long userId, String userType, String loginIp, long tenantId, String accessToken, String userAgent, LocalDateTime expireTime) {
        if(!OauthConstant.GOVERNANCE_USER.equals(userType)) {
            return;
        }
        UserAgentInfo userAgentInfo = UserAgentHandler.parse(userAgent);
        EmployeeAccessToken employeeAccessToken = new EmployeeAccessToken();
        employeeAccessToken.setCreateTime(LocalDateTime.now());
        employeeAccessToken.setAccessTokenHash(this.hashToken(accessToken));
        employeeAccessToken.setIpAddress(loginIp);
        employeeAccessToken.setClientDevice(userAgentInfo.getDeviceType().getValue());
        employeeAccessToken.setDeviceName(userAgentInfo.getDeviceName());
        employeeAccessToken.setExpireTime(expireTime);
        employeeAccessToken.setRecentlyRefreshTime(LocalDateTime.now());
        employeeAccessToken.setEmployeeId(userId);
        employeeAccessToken.setTokenStatus(true);
        super.save(employeeAccessToken);
    }

    /**
     * 登出 设置为失效
     *
     * @param accessToken 访问token
     */
    @Override
    public void logout(String accessToken) {
        String hash = this.hashToken(accessToken);
        super.lambdaUpdate()
            .eq(EmployeeAccessToken::getAccessTokenHash, hash)
            .set(EmployeeAccessToken::getTokenStatus, false)
            .update();
    }

    /**
     * 强制登出
     * 获取当前用户有效的登录token 并设置为失效
     *
     * @param userType 用户类型
     * @param userId   用户id
     *
     * @return 登出的token
     */
    @Override
    public List<String> logoutAndGetTokens(String userType, long userId) {
        if(!OauthConstant.GOVERNANCE_USER.equals(userType)) {
            return List.of();
        }
        List<EmployeeAccessToken> tokens = super.lambdaQuery()
            .select(EmployeeAccessToken::getId, EmployeeAccessToken::getAccessTokenHash)
            .eq(EmployeeAccessToken::getEmployeeId, userId)
            .eq(EmployeeAccessToken::getTokenStatus, true)
            .gt(EmployeeAccessToken::getExpireTime, LocalDateTime.now())
            .list();
        if (EmptyUtil.isNotEmpty(tokens)) {
            super.lambdaUpdate()
                .in(EmployeeAccessToken::getId, tokens.stream().map(EmployeeAccessToken::getId).toList())
                .set(EmployeeAccessToken::getTokenStatus, false)
                .update();
        }
        return tokens.stream().map(EmployeeAccessToken::getAccessTokenHash).toList();
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
    @Override
    public List<String> logoutAndGetTokens(String userType, Collection<Long> userIds) {
        if(!OauthConstant.GOVERNANCE_USER.equals(userType) || EmptyUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<EmployeeAccessToken> tokens = super.lambdaQuery()
            .select(EmployeeAccessToken::getId, EmployeeAccessToken::getAccessTokenHash)
            .in(EmployeeAccessToken::getEmployeeId, userIds)
            .eq(EmployeeAccessToken::getTokenStatus, true)
            .gt(EmployeeAccessToken::getExpireTime, LocalDateTime.now())
            .list();
        if (EmptyUtil.isNotEmpty(tokens)) {
            super.lambdaUpdate()
                .in(EmployeeAccessToken::getId, tokens.stream().map(EmployeeAccessToken::getId).toList())
                .set(EmployeeAccessToken::getTokenStatus, false)
                .update();
        }
        return tokens.stream().map(EmployeeAccessToken::getAccessTokenHash).toList();
    }

    /**
     * 更新token过期时间
     *
     * @param accessToken 访问token
     * @param expireTime  过期时间
     */
    @Override
    public void updateExpire(String accessToken, LocalDateTime expireTime) {
        String hash = this.hashToken(accessToken);
        super.lambdaUpdate()
            .eq(EmployeeAccessToken::getAccessTokenHash, hash)
            .eq(EmployeeAccessToken::getTokenStatus, true)
            .set(EmployeeAccessToken::getExpireTime, expireTime)
            .set(EmployeeAccessToken::getRecentlyRefreshTime, LocalDateTime.now())
            .update();
    }


    private String hashToken(String accessToken) {
        int startIndex = accessToken.indexOf(".");
        if (startIndex == -1) {
            throw new IllegalArgumentException("Invalid access token format");
        }
        int endIndex = accessToken.lastIndexOf(".");
        if (endIndex == -1 || endIndex <= startIndex) {
            throw new IllegalArgumentException("Invalid access token format");
        }
        String payload = accessToken.substring(startIndex + 1, endIndex);
        return XXHash.hashHex64(payload);
    }
}
