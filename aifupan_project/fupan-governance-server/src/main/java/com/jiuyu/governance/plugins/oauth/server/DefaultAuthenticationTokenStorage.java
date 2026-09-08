package com.jiuyu.governance.plugins.oauth.server;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.symmetric.AES;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.util.DateOps;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.utils.XXHash;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 默认身份认证存储 redis + mysql
 *
 * @author HeHui
 * @date 2026-02-20 17:27
 */
@Slf4j
@Service
public class DefaultAuthenticationTokenStorage implements AuthenticationTokenStorage {

    // 业务
    private final TenantPrivilegeService tenantPrivilegeService;

    /**
     * 缓存 token
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 数据库token存储
     */
    private final DbTokenStorage dbTokenStorage;

    /**
     * jwt密钥
     */
    private final String jwtSecret;


    /**
     * 登录token有效期 秒
     */
    private final long tokenExpire;

    /**
     * 缓存前缀
     */
    private final String prefix;


    /**
     * 登录token窗口滑动延长有效期的系数 如 100秒 有效期还剩 89秒时，则延长有效期
     */
    private final BigDecimal extendTimeFactor = BigDecimal.valueOf(0.9);

    private final BigDecimal tokenExpireBig;

    private final AES aes;

    public DefaultAuthenticationTokenStorage(TenantPrivilegeService tenantPrivilegeService, StringRedisTemplate redisTemplate, @Value("${jiuyu.oauth.server.secret}") String jwtSecret, @Value("${jiuyu.oauth.server.token-expire-hour}") Integer tokenExpire, @Value("${jiuyu.oauth.client.secret}") String clientSecret, ObjectProvider<DbTokenStorage> dbTokenStorageProvider) {
        this.tenantPrivilegeService = tenantPrivilegeService;
        this.redisTemplate = redisTemplate;
        this.jwtSecret = jwtSecret;
        this.tokenExpire = Duration.ofHours(tokenExpire).toSeconds();
        this.prefix = "oauth:token:";
        this.tokenExpireBig = BigDecimal.valueOf(this.tokenExpire);
        this.aes = new AES(clientSecret.getBytes(StandardCharsets.UTF_8));
        this.dbTokenStorage = dbTokenStorageProvider.getIfUnique(() -> {
            log.warn("[授权认证] 未配置数据库token存储，将忽略token存储到数据库的操作，且无法通过用户ID强制踢人");
            return new DbTokenStorage() {
                @Override
                public void newLogin(long userId, String userType, String loginIp, long tenantId, String accessToken, String userAgent, LocalDateTime expireTime) {

                }

                @Override
                public void logout(String accessToken) {

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
                    return List.of();
                }

                @Override
                public void updateExpire(String accessToken, LocalDateTime expireTime) {

                }
            };
        });
    }

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
    @Override
    public String login(String userType, long userId, String username, String loginIp, long tenantId, String userAgent, Map<String, String> metadata) {
        String uuid = IdUtil.fastSimpleUUID();
        try {
            String accessToken = generateAccessToken(userId, username, userType, loginIp, uuid, "", tenantId, metadata);
            dbTokenStorage.newLogin(userId, userType, loginIp, tenantId, accessToken, userAgent, LocalDateTime.now().plusSeconds(tokenExpire));
            log.info("[授权认证] {} user {} ID: {} login save db success", userType, username, userId);
            redisTemplate.opsForValue().set(prefix + userType + ":" + hashToken(accessToken), accessToken, tokenExpire, TimeUnit.SECONDS);
            log.info("[授权认证] {} user {} ID: {} login generate accessToken: {} cache success", userType, username, userId, accessToken);
            return aes.encryptBase64(accessToken);
        } catch (Exception e) {
            throw new BusinessException(BizErrorCode.UNAUTHORIZED, "登录失败", e);
        }
    }


    /**
     * 登出
     *
     * @param accessToken 访问token
     *
     * @return 是否成功
     */
    @Override
    public boolean logout(String accessToken) {
        try {
            AccessUser accessUser = analysis(accessToken);
            String rawToken = accessUser.accessToken();
            String cacheKey = prefix + accessUser.userType() + ":" + hashToken(rawToken);
            redisTemplate.delete(cacheKey);
            dbTokenStorage.logout(rawToken);
            log.info("[授权认证] {} user {} ID: {} logout success", accessUser.userType(), accessUser.username(), accessUser.userId());
            return true;
        } catch (Exception e) {
            log.error("[授权认证] logout error", e);
            return false;
        }
    }

    /**
     * 强制登出 指定用户所有设备
     *
     * @param userType 用户类型
     * @param userId   用户id
     *
     * @return 是否成功
     */
    @Override
    public boolean logout(String userType, long userId) {
        List<String> tokens = dbTokenStorage.logoutAndGetTokens(userType, userId);
        if (EmptyUtil.isNotEmpty(tokens)) {
            redisTemplate.delete(tokens.stream().map(token -> prefix + userType + ":" + token).toList());
            return true;
        }
        return false;
    }


    /**
     * 批量强制登出
     *
     * @param userType 用户类型
     * @param userIds  用户id
     *
     * @return 是否成功
     */
    @Override
    public boolean logout(String userType, Collection<Long> userIds) {
        List<String> tokens = dbTokenStorage.logoutAndGetTokens(userType, userIds);
        if (EmptyUtil.isNotEmpty(tokens)) {
            redisTemplate.delete(tokens.stream().map(token -> prefix + userType + ":" + token).toList());
            return true;
        }
        return false;
    }

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
    @Override
    public Optional<AccessUser> getUser(String accessToken) {
        try {
            AccessUser user = analysis(accessToken);
            String cacheKey = prefix + user.userType() + ":" + hashToken(user.accessToken());
            long expire = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (expire > 0) {
                // 令牌有效时间小于扩展因子，则刷新有效期
                if (NumberUtil.div(BigDecimal.valueOf(expire), tokenExpireBig).compareTo(extendTimeFactor) < 0) {
                    // 判断租户是否可用
                    boolean available = tenantPrivilegeService.available(user.currentTenantId());
                    if (!available) {
                        redisTemplate.delete(cacheKey);
                        return Optional.empty();
                    }
                    redisTemplate.expire(cacheKey, tokenExpire, TimeUnit.SECONDS);
                    dbTokenStorage.updateExpire(user.accessToken(), LocalDateTime.now().plusSeconds(tokenExpire));
                }
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("[授权认证] access token analysis extract UserInfo error, accessToken: {}", accessToken, e);
            return Optional.empty();
        }
    }

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
    @Override
    public boolean effective(String accessToken) {
        try {
            AccessUser accessUser = analysis(accessToken);
            return redisTemplate.hasKey(prefix + accessUser.userType() + ":" + hashToken(accessUser.accessToken()));
        } catch (Exception e) {
            log.error("[授权认证] access token analysis error, accessToken: {}", accessToken, e);
            return false;
        }
    }

    /**
     * Hash Access Token / 令牌哈希
     * <p>
     * Calculates the hash value of the access token.
     * 计算访问令牌的哈希值。
     * </p>
     *
     * @param accessToken The access token to hash / 要哈希的访问令牌
     *
     * @return The hash value of the access token / 访问令牌的哈希值
     */
    private String hashToken(String accessToken) {
        int startIndex = accessToken.indexOf(".");
        if (startIndex == -1) {
            return null;
        }
        int endIndex = accessToken.lastIndexOf(".");
        if (endIndex == -1 || endIndex <= startIndex) {
            return null;
        }
        String payload = accessToken.substring(startIndex + 1, endIndex);
        return XXHash.hashHex64(payload);
    }


    /**
     * Generate Access Token / 生成访问令牌
     * <p>
     * Creates a new JWT access token for the specified user ID.
     * 为指定的用户ID创建一个新的JWT访问令牌。
     * </p>
     *
     * @param userId   The user ID / 用户ID
     * @param username The username of the user / 用户名
     * @param userType The user type / 用户类型
     * @param loginIp  The IP address of the user's login / 用户登录的IP地址
     * @param jwtId    The unique identifier for the JWT token / JWT令牌的唯一标识符
     * @param tenantId The tenant ID / 租户ID
     * @param metadata The metadata of the user / 用户的元数据
     *
     * @return The generated JWT access token / 生成的JWT访问令牌
     */
    private String generateAccessToken(long userId, String username, String userType, String loginIp, String jwtId, String equipment, long tenantId, Map<String, String> metadata) throws JOSEException, ExecutionException, InterruptedException {
        // 1. 构建声明（Claims）
        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .jwtID(jwtId)
            .subject(userId + "") // 主题（用户标识）
            .claim("username", username)
            .claim("ips", loginIp)
            .claim("tenantId", tenantId)
            .claim("userType", userType)
            .claim("metadata", EmptyUtil.isEmpty(metadata) ? null : JsonTemplate.toJson(metadata))
            .issuer(equipment == null ? "unknown" : equipment)           // 发行者 设备
            .issueTime(now)             // 签发时间
            .expirationTime(DateOps.of(now.getTime()).addYear(1).getDate()) // 过期时间 1年
            .build();
        // 2. 创建签名器（Signer）
        JWSSigner signer = new MACSigner(jwtSecret);

        // 3. 创建 SignedJWT 对象
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        // 4. 签名
        signedJWT.sign(signer);
        // 5. 序列化为字符串
        return signedJWT.serialize();
    }


    /**
     * Analyze User ID from Access Token / 从访问令牌分析用户ID
     * <p>
     * Extracts the user ID from the provided JWT access token.
     * 从提供的JWT访问令牌中提取用户ID。
     * </p>
     *
     * @param accessToken The access token to analyze / 要分析的访问令牌
     *
     * @return The user extracted from the token / 从令牌中提取的用户
     */
    private AccessUser analysis(String accessToken) throws ParseException, JOSEException {
        if (accessToken.indexOf(".") == -1) {
            accessToken = aes.decryptStr(accessToken);
        }
        // 1. 解析 JWT
        SignedJWT signedJWT = SignedJWT.parse(accessToken);

        // 2. 创建验证器
        JWSVerifier verifier = new MACVerifier(jwtSecret);

        // 3. 验证签名
        if (!signedJWT.verify(verifier)) {
            throw new IllegalArgumentException("Invalid Access Token");
        }
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        String metadata = claimsSet.getStringClaim("metadata");
        Map<String, Object> map = JsonTemplate.isJsonObj(metadata) ? JsonTemplate.toMap(metadata) : Map.of();
        Map<String, String> context = map.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
        return new AccessUser(accessToken,
            Long.parseLong(claimsSet.getSubject()),
            claimsSet.getStringClaim("username"),
            claimsSet.getLongClaim("tenantId"),
            claimsSet.getStringClaim("userType"),
            claimsSet.getStringClaim("ips"),
            context
        );
    }
}
