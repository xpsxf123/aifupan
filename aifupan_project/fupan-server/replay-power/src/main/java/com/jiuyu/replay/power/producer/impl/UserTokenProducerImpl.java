package com.jiuyu.replay.power.producer.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.framework.util.CollUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.constant.BusinessCachePrefix;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.power.constant.PowerProperties;
import com.jiuyu.replay.power.entity.UserLoginInfoEntity;
import com.jiuyu.replay.power.entity.UserTokenEntity;
import com.jiuyu.replay.power.producer.UserTokenProducer;
import com.jiuyu.replay.power.repository.service.UserLoginInfoService;
import com.jiuyu.replay.power.repository.service.UserTokenService;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserLoginInfoCacheVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户Token服务实现类
 *
 * @author RayChou
 * @date 2025/7/3
 */
@Service
@Slf4j
public class UserTokenProducerImpl implements UserTokenProducer {

    @Resource
    private ResilientRedisTemplate<String, Object> resilientRedisTemplate;

    @Resource
    UserTokenService userTokenService;

    @Resource
    UserLoginInfoService userLoginInfoService;

    @Resource
    private PowerProperties powerProperties;

    // 用户token过期时间
    private final int tokenExpireDay = 30;

    @Override
    public UserCacheVo getUserByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        String tokenRedisKey = powerProperties.getUserLoginTokenRedisKey() + token;
        // 判断Redis是否处于降级状态
        if (!resilientRedisTemplate.isDegraded()) {
            // Redis正常，从Redis获取
            return (UserCacheVo) resilientRedisTemplate.getRedisTemplate().opsForValue().get(tokenRedisKey);
        }
        // Redis不可用或获取失败，从数据库获取
        UserCacheVo userVo = userTokenService.getUserByToken(token);
        log.debug("[用户登录] 从数据库获取用户信息: token={}, found={}", token, userVo != null);
        return userVo;
    }

    @Override
    public void updateTokenExpire(UserCacheVo userVo, String token, Boolean isHeartbeat, Boolean isExcludePath) {
        if (Objects.isNull(userVo) || StrUtil.isBlank(token)) {
            return;
        }
        // 把用户信息存到当前线程
        GlobalObject.setLocalUser(userVo);
        // 将用户ID存储到RequestContext中
        RequestContext.setUserId(userVo.getId());
        // 如果是排除路径不更新redis-token过期时间和最后请求时间
        if (isExcludePath) {
            return;
        }
        // 始终更新Redis（如果可用）
        updateRedis(userVo, token, isHeartbeat);

        // 数据库更新策略：心跳接口降低频率，业务接口正常更新
        if (isHeartbeat) {
            // 心跳接口：每5分钟更新一次数据库
            updateDatabaseWithThrottle(userVo, token);
        } else {
            // 业务接口：异步更新数据库
            updateDatabaseAsync(userVo, token);
        }
    }

    /**
     * 更新Redis中的token信息
     */
    private void updateRedis(UserCacheVo userVo, String token, Boolean isHeartbeat) {
        if (resilientRedisTemplate.isDegraded()) {
            return; // Redis降级时跳过
        }

        try {
            // 更新token过期时间
            resilientRedisTemplate.getRedisTemplate().expire(
                    powerProperties.getUserLoginTokenRedisKey() + token,
                    Duration.ofDays(tokenExpireDay)
            );
            String userLoginInfoRedisKey = powerProperties.getUserLoginInfoRedisKey() + userVo.getId();
            resilientRedisTemplate.getRedisTemplate().expire(userLoginInfoRedisKey, Duration.ofDays(tokenExpireDay));

            if (!isHeartbeat) {
                // 非心跳接口不更新登录信息中的最后请求时间
                return;
            }
            // 更新登录信息中的最后请求时间
            UserLoginInfoCacheVo userLoginInfoCacheVo = (UserLoginInfoCacheVo) resilientRedisTemplate
                    .getRedisTemplate().opsForHash().get(userLoginInfoRedisKey, token);
            if (userLoginInfoCacheVo != null) {
                userLoginInfoCacheVo.setLastRequestTime(System.currentTimeMillis());
                resilientRedisTemplate.getRedisTemplate().opsForHash()
                        .put(userLoginInfoRedisKey, token, userLoginInfoCacheVo);
            }
        } catch (Exception e) {
            log.debug("更新Redis失败", e);
        }
    }

    @Override
    public List<UserLoginInfoEntity> listUserLoginInfoByUserId(Long userId) {
        return userLoginInfoService.list(new LambdaQueryWrapper<>(UserLoginInfoEntity.class).eq(UserLoginInfoEntity::getUserId, userId).ge(UserLoginInfoEntity::getExpireTime, LocalDateTime.now()));
    }

    @Override
    public void batchRemoveTokenByUserId(Long userId, List<String> batchTokens) {
        if (CollectionUtil.isEmpty(batchTokens)) {
            return;
        }
        if (!resilientRedisTemplate.isDegraded()) {
            batchTokens.forEach(token -> {
                resilientRedisTemplate.getRedisTemplate().delete(powerProperties.getUserLoginTokenRedisKey() + token);
                resilientRedisTemplate.getRedisTemplate().opsForHash().delete(powerProperties.getUserLoginInfoRedisKey() + userId, token);
            });
        }
        // 无论是否降级都删除数据库中的token信息
        userTokenService.batchRemoveToken(batchTokens);
        userLoginInfoService.batchRemoveToken(batchTokens);
    }

    @Override
    public void saveUserLoginInfo(UserCacheVo userCacheVo, String token, String loginIp, String source, String fingerprint) {
        // 存储到Redis
        if (!resilientRedisTemplate.isDegraded()) {
            // 存储token缓存到Redis
            String tokenRedisKey = powerProperties.getUserLoginTokenRedisKey() + token;
            String userLoginInfoRedisKey = powerProperties.getUserLoginInfoRedisKey() + userCacheVo.getId();
            Duration expireTimeDuration = Duration.ofDays(tokenExpireDay);
            resilientRedisTemplate.getRedisTemplate().opsForValue().set(tokenRedisKey, userCacheVo, expireTimeDuration);

            // 存储登录信息
            UserLoginInfoCacheVo userLoginInfoCacheVo = new UserLoginInfoCacheVo(source, System.currentTimeMillis(), fingerprint);
            resilientRedisTemplate.getRedisTemplate().opsForHash().put(userLoginInfoRedisKey, token, userLoginInfoCacheVo);
            resilientRedisTemplate.getRedisTemplate().expire(userLoginInfoRedisKey, expireTimeDuration);
        }
        // 存储用户Token信息到数据库
        LocalDateTime expireTime = LocalDateTime.now().plusDays(tokenExpireDay);
        userTokenService.saveUserToken(token, userCacheVo, expireTime);
        // 存储用户登录信息
        userLoginInfoService.saveUserLoginInfo(userCacheVo.getId(), token, source, fingerprint, expireTime);
    }

    @Override
    public void removeUserTokenByUserId(Long userId) {
        if (Objects.isNull(userId)) {
            return;
        }
        // 未降级情况下删除缓存中数据
        if (!resilientRedisTemplate.isDegraded()) {
            Map<Object, Object> entries = resilientRedisTemplate.getRedisTemplate().opsForHash().entries(powerProperties.getUserLoginInfoRedisKey() + userId);
            if (!entries.isEmpty()) {
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    String loginToken = (String) entry.getKey();
                    resilientRedisTemplate.getRedisTemplate().delete(powerProperties.getUserLoginTokenRedisKey() + loginToken);
                }
            }
            resilientRedisTemplate.getRedisTemplate().delete(powerProperties.getUserLoginInfoRedisKey() + userId);
        }
        // 删除数据库中数据 登录信息&token信息
        userTokenService.removeTokenByUserId(userId);
        userLoginInfoService.removeUserLoginInfoByUserId(userId);
    }

    @Override
    public void updateBaseUserInfo(UserCacheVo userCacheVo) {
        if (Objects.isNull(userCacheVo)) {
            return;
        }
        // 未降级情况下更新缓存中数据
        if (!resilientRedisTemplate.isDegraded()) {
            String userLoginInfoRedisKey = powerProperties.getUserLoginInfoRedisKey() + userCacheVo.getId();
            Map<Object, Object> entries = resilientRedisTemplate.getRedisTemplate().opsForHash().entries(userLoginInfoRedisKey);
            if (!entries.isEmpty()) {
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    String token = (String) entry.getKey();
                    resilientRedisTemplate.getRedisTemplate().opsForValue().set(powerProperties.getUserLoginTokenRedisKey() + token, userCacheVo, Duration.ofDays(tokenExpireDay));
                }
                resilientRedisTemplate.getRedisTemplate().expire(userLoginInfoRedisKey, Duration.ofDays(tokenExpireDay));
            }
        }
        // 更新数据库中用户信息
        LocalDateTime expireTime = LocalDateTime.now().plusDays(tokenExpireDay);
        userTokenService.updateUserInfoByUserId(userCacheVo.getId(), userCacheVo, expireTime);
        userLoginInfoService.updateUserLoginInfoExpireTimeByUserId(userCacheVo.getId(), expireTime);
    }

    /**
     * 更新用户手机号缓存
     *
     * @param userId    用户id
     * @param newMobile 新手机号
     */
    @Override
    public void updateUserMobileCache(long userId, String newMobile) {
        // 未降级情况下更新缓存中数据
        if (resilientRedisTemplate.isDegraded()) {
            return;
        }
        String userLoginInfoRedisKey = powerProperties.getUserLoginInfoRedisKey() + userId;
        Map<Object, Object> entries = resilientRedisTemplate.getRedisTemplate().opsForHash().entries(userLoginInfoRedisKey);
        if (EmptyUtil.isEmpty(entries)) {
            return;
        }
        List<String> cacheList = entries.keySet().stream().map(key -> powerProperties.getUserLoginTokenRedisKey() + key).toList();
        Map<Integer, String> indexCacheMap = CollUtil.indexToMap(cacheList);
        List<Object> valueList = resilientRedisTemplate.getRedisTemplate().opsForValue().multiGet(cacheList);
        if (EmptyUtil.isEmpty(valueList)) {
            return;
        }
        for (int i = 0; i < valueList.size(); i++) {
            if (valueList.get(i) instanceof UserCacheVo userCacheVo) {
                userCacheVo.setPhone(newMobile);
                resilientRedisTemplate.getRedisTemplate().opsForValue().set(indexCacheMap.get(i), userCacheVo, Duration.ofDays(tokenExpireDay));
            }
        }
    }

    @Override
    public void removeUserToken(String token, Long userId) {
        if (StrUtil.isBlank(token) || Objects.isNull(userId)) {
            return;
        }
        if (!resilientRedisTemplate.isDegraded()) {
            resilientRedisTemplate.getRedisTemplate().delete(powerProperties.getUserLoginTokenRedisKey() + token);
            resilientRedisTemplate.getRedisTemplate().opsForHash().delete(powerProperties.getUserLoginInfoRedisKey() + userId, token);
        }
        userLoginInfoService.removeUserLoginInfoByToken(token);
        userTokenService.removeTokenByToken(token);
    }

    @Override
    public void updateUserStatusByUserId(Long userId, Integer status) {
        if (Objects.isNull(userId) || Objects.isNull(status)) {
            return;
        }
        // 非降级情况下修改用户缓存状态信息
        if (!resilientRedisTemplate.isDegraded()) {
            String userLoginInfoRedisKey = powerProperties.getUserLoginInfoRedisKey() + userId;
            Map<Object, Object> entries = resilientRedisTemplate.getRedisTemplate().opsForHash().entries(userLoginInfoRedisKey);
            if (!entries.isEmpty()) {
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    String token = (String) entry.getKey();
                    UserCacheVo userCacheVo = (UserCacheVo) resilientRedisTemplate.getRedisTemplate().opsForValue().get(powerProperties.getUserLoginTokenRedisKey() + token);
                    if (userCacheVo != null) {
                        userCacheVo.setStatus(status);
                        resilientRedisTemplate.getRedisTemplate().opsForValue().set(powerProperties.getUserLoginTokenRedisKey() + token, userCacheVo, Duration.ofDays(30));
                    }
                }
                resilientRedisTemplate.getRedisTemplate().expire(userLoginInfoRedisKey, Duration.ofDays(tokenExpireDay));
            }
        }

        // 修改数据库用户状态
        List<UserTokenEntity> userTokenEntityList = userTokenService.listUserTokenByUserId(userId);
        if (CollectionUtil.isEmpty(userTokenEntityList)) {
            return;
        }
        LocalDateTime expireTime = LocalDateTime.now().plusDays(tokenExpireDay);
        userTokenEntityList.forEach(userTokenEntity -> {
            UserCacheVo userCacheVo = JSON.parseObject(userTokenEntity.getUserInfo(), UserCacheVo.class);
            userCacheVo.setStatus(status);
            userTokenEntity.setUserInfo(JSON.toJSONString(userCacheVo));
            userTokenEntity.setExpireTime(expireTime);
        });
        userTokenService.updateBatchById(userTokenEntityList);
        userLoginInfoService.updateUserLoginInfoExpireTimeByUserId(userId, expireTime);
    }

    @Override
    public UserLoginInfoEntity getUserLoginInfoByUserIdAndToken(Long userId, String token) {
        if (Objects.isNull(userId) || Objects.isNull(token)) {
            return null;
        }
        return userLoginInfoService.getUserLoginInfoByUserIdAndToken(userId, token);
    }

    @Override
    public void removeUserTokenLastRequestDateAfterDayByUserId(Long userId, int day) {
        if (Objects.isNull(userId) || day <= 0) {
            return;
        }
        LocalDateTime timeoutTime = LocalDateTime.now().minusDays(day);
        // 判断是否处于降级状态
        if (!resilientRedisTemplate.isDegraded()) {
            Map<Object, Object> entries = resilientRedisTemplate.opsForHash().entries(powerProperties.getUserLoginInfoRedisKey() + userId);
            if (!entries.isEmpty()) {
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    String token = (String) entry.getKey();
                    UserLoginInfoCacheVo userLoginInfoCacheVo = (UserLoginInfoCacheVo) entry.getValue();
                    if (userLoginInfoCacheVo.getLastRequestTime() == null || timeoutTime.isAfter(LocalDateTime.ofInstant(Instant.ofEpochMilli(userLoginInfoCacheVo.getLastRequestTime()), ZoneId.systemDefault()))) {
                        resilientRedisTemplate.getRedisTemplate().opsForHash().delete(powerProperties.getUserLoginInfoRedisKey() + userId, token);
                        resilientRedisTemplate.getRedisTemplate().delete(powerProperties.getUserLoginTokenRedisKey() + token);
                    }
                }
            }
        }
        // 删除数据库中超过day天未访问的token
        userTokenService.removeTokenByUserIdAndLastAccessTime(userId, timeoutTime);
        userLoginInfoService.removeUserLoginInfoByUserIdAndLastRequestTime(userId, timeoutTime);
    }

    @Override
    public String loadRedisTokensToDatabase() {
        log.info("[Token同步] 开始将Redis中的token信息加载到数据库");

        // 检查Redis是否可用
        if (resilientRedisTemplate.isDegraded()) {
            log.warn("[Token同步] Redis处于降级状态，无法执行同步操作");
            throw new RuntimeException("Redis处于降级状态，无法执行同步操作");
        }

        try {
            AtomicInteger tokenCount = new AtomicInteger(0);
            AtomicInteger loginInfoCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // 1. 同步用户token信息
            log.info("[Token同步] 开始同步用户token信息");
            Set<String> tokenKeys = scanTokenKeys();
            if (tokenKeys != null && !tokenKeys.isEmpty()) {
                for (String tokenKey : tokenKeys) {
                    try {
                        // 从Redis获取用户缓存信息
                        UserCacheVo userCacheVo = (UserCacheVo) resilientRedisTemplate.getRedisTemplate().opsForValue().get(tokenKey);
                        if (userCacheVo != null) {
                            // 提取token
                            String token = tokenKey.replace(powerProperties.getUserLoginTokenRedisKey(), "");

                            // 检查数据库中是否已存在该token
                            UserTokenEntity existingToken = userTokenService.getOne(
                                    new LambdaQueryWrapper<>(UserTokenEntity.class)
                                            .eq(UserTokenEntity::getToken, token)
                                            .eq(UserTokenEntity::getIsDeleted, 0)
                            );

                            if (existingToken == null) {
                                // 获取Redis中的过期时间
                                Long expireSeconds = resilientRedisTemplate.getRedisTemplate().getExpire(tokenKey);
                                LocalDateTime expireTime = expireSeconds != null && expireSeconds > 0
                                        ? LocalDateTime.now().plusSeconds(expireSeconds)
                                        : LocalDateTime.now().plusDays(tokenExpireDay);

                                // 保存到数据库
                                userTokenService.saveUserToken(token, userCacheVo, expireTime);
                                tokenCount.incrementAndGet();
                                log.debug("[Token同步] 成功同步token: {}, 用户ID: {}", token, userCacheVo.getId());
                            } else {
                                log.debug("[Token同步] Token已存在，跳过: {}", token);
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("[Token同步] 同步token失败: {}, 错误: {}", tokenKey, e.getMessage(), e);
                    }
                }
            }

            // 2. 同步用户登录信息
            log.info("[Token同步] 开始同步用户登录信息");
            Set<String> loginInfoKeys = scanLoginInfoKeys();
            if (loginInfoKeys != null && !loginInfoKeys.isEmpty()) {
                for (String loginInfoKey : loginInfoKeys) {
                    try {
                        // 提取用户ID
                        String userIdStr = loginInfoKey.replace(powerProperties.getUserLoginInfoRedisKey(), "");
                        Long userId = Long.parseLong(userIdStr);

                        // 获取该用户的所有登录信息
                        Map<Object, Object> loginInfoMap = resilientRedisTemplate.getRedisTemplate().opsForHash().entries(loginInfoKey);
                        if (loginInfoMap != null && !loginInfoMap.isEmpty()) {
                            for (Map.Entry<Object, Object> entry : loginInfoMap.entrySet()) {
                                try {
                                    String token = (String) entry.getKey();
                                    UserLoginInfoCacheVo loginInfoCacheVo = (UserLoginInfoCacheVo) entry.getValue();

                                    if (loginInfoCacheVo != null) {
                                        // 检查数据库中是否已存在该登录信息
                                        UserLoginInfoEntity existingLoginInfo = userLoginInfoService.getOne(
                                                new LambdaQueryWrapper<>(UserLoginInfoEntity.class)
                                                        .eq(UserLoginInfoEntity::getUserId, userId)
                                                        .eq(UserLoginInfoEntity::getToken, token)
                                                        .eq(UserLoginInfoEntity::getIsDeleted, 0)
                                        );

                                        if (existingLoginInfo == null) {
                                            // 获取Redis中的过期时间
                                            Long expireSeconds = resilientRedisTemplate.getRedisTemplate().getExpire(loginInfoKey);
                                            LocalDateTime expireTime = expireSeconds != null && expireSeconds > 0
                                                    ? LocalDateTime.now().plusSeconds(expireSeconds)
                                                    : LocalDateTime.now().plusDays(tokenExpireDay);

                                            // 保存到数据库
                                            userLoginInfoService.saveUserLoginInfo(
                                                    userId,
                                                    token,
                                                    loginInfoCacheVo.getSource(),
                                                    loginInfoCacheVo.getFingerprint(),
                                                    expireTime
                                            );
                                            loginInfoCount.incrementAndGet();
                                            log.debug("[Token同步] 成功同步登录信息: 用户ID={}, token={}", userId, token);
                                        } else {
                                            log.debug("[Token同步] 登录信息已存在，跳过: 用户ID={}, token={}", userId, token);
                                        }
                                    }
                                } catch (Exception e) {
                                    errorCount.incrementAndGet();
                                    log.error("[Token同步] 同步登录信息失败: 用户ID={}, token={}, 错误: {}",
                                            userId, entry.getKey(), e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("[Token同步] 处理登录信息键失败: {}, 错误: {}", loginInfoKey, e.getMessage(), e);
                    }
                }
            }

            String resultMessage = String.format(
                    "Token同步完成！同步token数量: %d, 同步登录信息数量: %d, 错误数量: %d",
                    tokenCount.get(), loginInfoCount.get(), errorCount.get()
            );
            log.info("[Token同步] {}", resultMessage);

            return resultMessage;

        } catch (Exception e) {
            log.error("[Token同步] 同步过程中发生异常", e);
            throw new RuntimeException("同步过程中发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 使用SCAN命令安全地扫描Redis键，避免阻塞
     *
     * @param pattern 匹配模式
     * @return 匹配的键集合
     */
    private Set<String> scanRedisKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            // 使用SCAN命令，每次扫描1000个键，避免阻塞
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)  // 每次扫描的数量
                    .build();

            Cursor<byte[]> cursor = resilientRedisTemplate.getRedisTemplate()
                    .getConnectionFactory()
                    .getConnection()
                    .scan(options);

            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            cursor.close();

            log.debug("[Token同步] SCAN扫描到 {} 个匹配键: {}", keys.size(), pattern);

        } catch (Exception e) {
            log.error("[Token同步] SCAN扫描Redis键失败: pattern={}, error={}", pattern, e.getMessage(), e);
        }

        return keys;
    }

    /**
     * 扫描用户token键，排除登录信息键
     *
     * @return token键集合
     */
    private Set<String> scanTokenKeys() {
        Set<String> tokenKeys = new HashSet<>();
        try {
            String tokenPrefix = powerProperties.getUserLoginTokenRedisKey();
            String loginInfoPrefix = powerProperties.getUserLoginInfoRedisKey();

            // 使用SCAN扫描所有可能的键
            ScanOptions options = ScanOptions.scanOptions()
                    .match(tokenPrefix + "*")
                    .count(1000)
                    .build();

            Cursor<byte[]> cursor = resilientRedisTemplate.getRedisTemplate()
                    .getConnectionFactory()
                    .getConnection()
                    .scan(options);

            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                // 排除登录信息键：只要token键，不要login-info键
                if (key.startsWith(tokenPrefix) && !key.startsWith(loginInfoPrefix)) {
                    tokenKeys.add(key);
                }
            }
            cursor.close();

            log.debug("[Token同步] 扫描到 {} 个token键", tokenKeys.size());

        } catch (Exception e) {
            log.error("[Token同步] 扫描token键失败: error={}", e.getMessage(), e);
        }

        return tokenKeys;
    }

    /**
     * 扫描用户登录信息键
     *
     * @return 登录信息键集合
     */
    private Set<String> scanLoginInfoKeys() {
        return scanRedisKeys(powerProperties.getUserLoginInfoRedisKey() + "*");
    }

    /**
     * 简单的数据库更新频率控制（用于心跳接口）
     */
    private void updateDatabaseWithThrottle(UserCacheVo userVo, String token) {
        String throttleKey = BusinessCachePrefix.USER_TOKEN_HEARTBEAT_CACHE.prefix + token;

        try {
            // 检查上次更新时间（只在Redis可用时检查）
            String lastUpdateTime = null;
            if (!resilientRedisTemplate.isDegraded()) {
                lastUpdateTime = (String) resilientRedisTemplate.getRedisTemplate()
                        .opsForValue().get(throttleKey);
            }

            long currentTime = System.currentTimeMillis();

            // 如果从未更新过，或者距离上次更新超过5分钟，才更新数据库
            if (lastUpdateTime == null ||
                    (currentTime - Long.parseLong(lastUpdateTime) > 5 * 60 * 1000)) {

                // 异步更新数据库
                CompletableFuture.runAsync(() -> {
                    try {
                        updateDatabaseDirectly(userVo, token);

                        // 记录本次更新时间（只在Redis可用时记录）
                        if (!resilientRedisTemplate.isDegraded()) {
                            resilientRedisTemplate.getRedisTemplate().opsForValue()
                                    .set(throttleKey, String.valueOf(currentTime), Duration.ofMinutes(10));
                        }
                    } catch (Exception e) {
                        log.error("心跳数据库更新失败", e);
                    }
                });
            }
        } catch (Exception e) {
            log.debug("心跳频率控制失败，直接更新数据库", e);
            // 如果频率控制失败，直接异步更新数据库
            updateDatabaseAsync(userVo, token);
        }
    }

    /**
     * 异步更新数据库（用于业务请求）
     */
    private void updateDatabaseAsync(UserCacheVo userVo, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                updateDatabaseDirectly(userVo, token);
            } catch (Exception e) {
                log.error("异步更新数据库失败", e);
            }
        });
    }

    /**
     * 直接更新数据库
     */
    private void updateDatabaseDirectly(UserCacheVo userVo, String token) {
        userTokenService.updateUserTokenExpireTimeByToken(token,
                LocalDateTime.now().plusDays(tokenExpireDay));
        userLoginInfoService.updateUserLoginInfoExpireTimeByUserIdAndToken(
                userVo.getId(), token, LocalDateTime.now().plusDays(tokenExpireDay));
    }

    @Override
    public String syncDegradedTokenDataToRedis() {
        log.info("[Redis降级] 开始同步降级期间的token数据到Redis...");

        if (resilientRedisTemplate.isDegraded()) {
            log.warn("[Redis降级] Redis仍处于降级状态，无法执行同步操作");
            return "Redis仍处于降级状态，同步失败";
        }

        AtomicInteger syncTokenCount = new AtomicInteger(0);
        AtomicInteger syncLoginInfoCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        try {
            // 1. 同步用户token数据
            syncTokenCount.set(syncUserTokensToRedis());

            // 2. 同步用户登录信息数据
            syncLoginInfoCount.set(syncUserLoginInfosToRedis());

            String result = String.format("同步完成 - Token数据: %d条, 登录信息: %d条, 错误: %d条",
                    syncTokenCount.get(), syncLoginInfoCount.get(), errorCount.get());

            log.info("[Redis降级] {}", result);
            return result;

        } catch (Exception e) {
            log.error("[Redis降级] 同步降级期间token数据失败: {}", e.getMessage(), e);
            return "同步失败: " + e.getMessage();
        }
    }

    /**
     * 同步用户token数据到Redis
     */
    private int syncUserTokensToRedis() {
        log.info("[Redis降级] 开始同步用户token数据到Redis...");

        // 查询所有有效的token数据（未过期的）
        List<UserTokenEntity> validTokens = userTokenService.list(
                new LambdaQueryWrapper<UserTokenEntity>()
                        .gt(UserTokenEntity::getExpireTime, LocalDateTime.now())
                        .eq(UserTokenEntity::getIsDeleted, false)
        );

        int syncCount = 0;
        for (UserTokenEntity tokenEntity : validTokens) {
            try {
                String redisKey = powerProperties.getUserLoginTokenRedisKey() + tokenEntity.getToken();

                // 检查Redis中是否已存在，避免覆盖
                if (!resilientRedisTemplate.getRedisTemplate().hasKey(redisKey)) {
                    // 反序列化用户信息
                    UserCacheVo userCacheVo = JSON.parseObject(tokenEntity.getUserInfo(), UserCacheVo.class);

                    // 计算剩余过期时间
                    long remainingSeconds = Duration.between(LocalDateTime.now(), tokenEntity.getExpireTime()).getSeconds();
                    if (remainingSeconds > 0) {
                        resilientRedisTemplate.getRedisTemplate().opsForValue()
                                .set(redisKey, userCacheVo, Duration.ofSeconds(remainingSeconds));
                        syncCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("[Redis降级] 同步token数据失败, token: {}, 错误: {}", tokenEntity.getToken(), e.getMessage());
            }
        }

        log.info("[Redis降级] 用户token数据同步完成，共同步 {} 条记录", syncCount);
        return syncCount;
    }

    /**
     * 同步用户登录信息数据到Redis
     */
    private int syncUserLoginInfosToRedis() {
        log.info("[Redis降级] 开始同步用户登录信息数据到Redis...");

        // 查询所有有效的登录信息数据
        List<UserLoginInfoEntity> validLoginInfos = userLoginInfoService.list(
                new LambdaQueryWrapper<UserLoginInfoEntity>()
                        .gt(UserLoginInfoEntity::getExpireTime, LocalDateTime.now())
                        .eq(UserLoginInfoEntity::getIsDeleted, false)
        );

        int syncCount = 0;
        Map<Long, Map<String, UserLoginInfoCacheVo>> userLoginInfoMap = new HashMap<>();

        // 按用户ID分组
        for (UserLoginInfoEntity loginInfo : validLoginInfos) {
            try {
                Long userId = loginInfo.getUserId();
                String token = loginInfo.getToken();

                // 转换为缓存对象
                UserLoginInfoCacheVo cacheVo = new UserLoginInfoCacheVo();
                cacheVo.setSource(loginInfo.getSourceInfo());
                cacheVo.setFingerprint(loginInfo.getFingerprint());
                cacheVo.setLastRequestTime(loginInfo.getLastRequestTime() != null ?
                        loginInfo.getLastRequestTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() :
                        System.currentTimeMillis());

                userLoginInfoMap.computeIfAbsent(userId, k -> new HashMap<>()).put(token, cacheVo);

            } catch (Exception e) {
                log.warn("[Redis降级] 处理登录信息数据失败, userId: {}, token: {}, 错误: {}",
                        loginInfo.getUserId(), loginInfo.getToken(), e.getMessage());
            }
        }

        // 批量写入Redis
        for (Map.Entry<Long, Map<String, UserLoginInfoCacheVo>> entry : userLoginInfoMap.entrySet()) {
            try {
                Long userId = entry.getKey();
                Map<String, UserLoginInfoCacheVo> loginInfos = entry.getValue();

                String redisKey = powerProperties.getUserLoginInfoRedisKey() + userId;

                // 检查Redis中是否已存在
                if (!resilientRedisTemplate.getRedisTemplate().hasKey(redisKey)) {
                    resilientRedisTemplate.getRedisTemplate().opsForHash().putAll(redisKey, loginInfos);
                    resilientRedisTemplate.getRedisTemplate().expire(redisKey, Duration.ofDays(tokenExpireDay));
                    syncCount += loginInfos.size();
                } else {
                    // 如果已存在，只添加不存在的token
                    for (Map.Entry<String, UserLoginInfoCacheVo> loginEntry : loginInfos.entrySet()) {
                        String token = loginEntry.getKey();
                        if (!resilientRedisTemplate.getRedisTemplate().opsForHash().hasKey(redisKey, token)) {
                            resilientRedisTemplate.getRedisTemplate().opsForHash().put(redisKey, token, loginEntry.getValue());
                            syncCount++;
                        }
                    }
                }

            } catch (Exception e) {
                log.warn("[Redis降级] 同步用户登录信息失败, userId: {}, 错误: {}", entry.getKey(), e.getMessage());
            }
        }

        log.info("[Redis降级] 用户登录信息数据同步完成，共同步 {} 条记录", syncCount);
        return syncCount;
    }
}
