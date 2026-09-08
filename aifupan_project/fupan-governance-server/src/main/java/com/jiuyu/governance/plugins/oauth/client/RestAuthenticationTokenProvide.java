package com.jiuyu.governance.plugins.oauth.client;

import cn.hutool.core.util.NumberUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.provides.AuthenticationTokenProvide;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import com.jiuyu.governance.plugins.oauth.pojo.OauthPackageLevelProperties;
import com.jiuyu.governance.plugins.oauth.pojo.ReplayAccessUser;
import com.jiuyu.governance.plugins.oauth.server.AuthenticationTokenStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于远程调用的token鉴权提供实现
 *
 * @author HeHui
 * @date 2026-03-17 18:31
 */
@Service
@Slf4j
@EnableConfigurationProperties(OauthPackageLevelProperties.class)
public class RestAuthenticationTokenProvide implements AuthenticationTokenProvide {

    private final AuthenticationTokenStorage authenticationTokenStorage;

    private final TenantPrivilegeService tenantPrivilegeService;

    private final ReplayHttpServer replayHttpServer;

    private final Cache<String, AccessUser> replayCache;

    private final ParameterizedTypeReference<ApiResponse<ReplayAccessUser>> replayResponseType = new ParameterizedTypeReference<ApiResponse<ReplayAccessUser>>() {
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final OauthPackageLevelProperties properties;

    public RestAuthenticationTokenProvide(AuthenticationTokenStorage authenticationTokenStorage, TenantPrivilegeService tenantPrivilegeService, ReplayHttpServer replayHttpServer, OauthPackageLevelProperties levelProperties) {
        this.authenticationTokenStorage = authenticationTokenStorage;
        this.tenantPrivilegeService = tenantPrivilegeService;
        this.replayHttpServer = replayHttpServer;
        this.replayCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(3))
            .maximumSize(3000)
            .build();
        this.properties = levelProperties;
    }


    /**
     * 获取用户信息
     *
     * @param accessToken 访问令牌
     *
     * @return {@link Optional }<{@link AccessUser }>
     */
    @Override
    public Optional<AccessUser> getUser(String accessToken) {
        if (EmptyUtil.isEmpty(accessToken)) {
            return Optional.empty();
        }
        // 当前系统的token
        if (!this.isReplayToken(accessToken)) {
            return this.authenticationTokenStorage.getUser(accessToken);
        }
        AccessUser accessUser = replayCache.get(accessToken, token -> {
            ApiResponse<ReplayAccessUser> apiResponse = replayHttpServer.get("/replay/user/infoByClient", Map.of("Token", accessToken)).retrieve().body(replayResponseType);
            if (apiResponse == null) {
                log.error("[认证授权] rest replay server error, token: {}, result: null", accessToken);
                return null;
            }
            if (apiResponse.failed()) {
                log.error("[认证授权] rest replay server error, token: {}, result: {}", accessToken, apiResponse.getMsg());
                return null;
            }
            ReplayAccessUser replayAccessUser = apiResponse.getData();
            Map<String, String> metadata = Map.of("packageLevel", replayAccessUser.getPackageLevel() == null ? "" : replayAccessUser.getPackageLevel().toString(), "packageName", replayAccessUser.getPackageName() == null ? "" : replayAccessUser.getPackageName(), "expirationDate", replayAccessUser.getExpirationDate() == null ? "" : replayAccessUser.getExpirationDate());
            String userType = Objects.equals(replayAccessUser.getUserType(), 1) ? OauthConstant.SYSTEM_USER : OauthConstant.CLIENT_USER;
            return new AccessUser(replayAccessUser.getToken(), replayAccessUser.getId(), replayAccessUser.getNickName(), replayAccessUser.getActiveTenantId(), userType, replayAccessUser.getIps(), metadata);
        });
        if (accessUser != null && Objects.equals(accessUser.userType(), OauthConstant.CLIENT_USER)) {
            if (EmptyUtil.isEmpty(accessUser.metadata())) {
                return Optional.empty();
            }
            boolean skipPackageLevelCheck = this.isSkipLevelCheck();
            if (skipPackageLevelCheck) {
                return Optional.of(accessUser);
            }
            String packageLevel = accessUser.metadata().get("packageLevel");
            if (!NumberUtil.isInteger(packageLevel)) {
                return Optional.empty();
            }
            if (Integer.parseInt(packageLevel) < InitializationTenant.MIN_GOVERNANCE_LEVEL) {
                return Optional.empty();
            }
//            if (!tenantPrivilegeService.available(accessUser.currentTenantId())) {
//                return Optional.empty();
//            }
        }
        return Optional.ofNullable(accessUser);
    }


    /**
     * 是否跳过套餐等级检查
     *
     * @return {@link Boolean }
     */
    private boolean isSkipLevelCheck() {
        if (Boolean.FALSE.equals(properties.getEnable())) {
            return true;
        }
        if (EmptyUtil.isEmpty(properties.getExcludePaths())) {
            return true;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        if (requestAttributes instanceof ServletRequestAttributes request) {
            String url = request.getRequest().getRequestURI();
            if (EmptyUtil.isEmpty(url) || url.equals("/")) {
                return false;
            }
            return properties.getExcludePaths().stream().anyMatch(path -> pathMatcher.match(path, url));
        }
        return false;
    }

    /**
     * 令牌是否有效
     *
     * @param accessToken 访问令牌
     *
     * @return {@link Boolean }
     */
    @Override
    public boolean effective(String accessToken) {
        if (EmptyUtil.isEmpty(accessToken)) {
            return false;
        }
        return this.getUser(accessToken).isPresent();
    }


    /**
     * 是否 replay的token
     *
     * @param accessToken 访问令牌
     *
     * @return {@link Boolean }
     */
    private boolean isReplayToken(String accessToken) {
        // todo 简单逻辑判断 少于100通通都是replay的
        return accessToken.length() < 100;
    }
}
