package com.jiuyu.replay.power.repository.service.impl;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.entity.UserTokenEntity;
import com.jiuyu.replay.power.repository.dao.UserTokenDao;
import com.jiuyu.replay.power.repository.service.UserTokenService;
import com.jiuyu.replay.power.vo.UserCacheVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户Token表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Service
@Slf4j
public class UserTokenServiceImpl extends ServiceImpl<UserTokenDao, UserTokenEntity> implements UserTokenService {

    @Resource
    private UserTokenDao userTokenDao;

    @Override
    public UserCacheVo getUserByToken(String token) {
        UserTokenEntity userTokenEntity = getOne(new LambdaQueryWrapper<>(UserTokenEntity.class).eq(UserTokenEntity::getToken, token));
        if (Objects.isNull(userTokenEntity)) {
            return null;
        }
        // 检查token是否过期
        if (userTokenEntity.getExpireTime().isBefore(LocalDateTime.now())) {
            log.debug("[用户登录] Token已过期: {}, 过期时间: {}", token, userTokenEntity.getExpireTime());
            return null;
        }

        // 反序列化用户信息
        return JSON.parseObject(userTokenEntity.getUserInfo(), UserCacheVo.class);
    }

    @Override
    public void updateUserTokenExpireTimeByToken(String token, LocalDateTime expireTime) {
        userTokenDao.updateUserTokenExpireTimeByToken(token, expireTime);
    }

    @Override
    public void batchRemoveToken(List<String> batchTokens) {
        remove(new LambdaQueryWrapper<>(UserTokenEntity.class).in(UserTokenEntity::getToken, batchTokens));
    }

    @Override
    public void removeTokenByUserId(Long userId) {
        remove(new LambdaQueryWrapper<>(UserTokenEntity.class).eq(UserTokenEntity::getUserId, userId));
    }

    @Override
    public void saveUserToken(String token, UserCacheVo userCacheVo, LocalDateTime expireTime) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        if (Objects.isNull(userCacheVo)) {
            return;
        }
        UserTokenEntity userTokenEntity = new UserTokenEntity();
        userTokenEntity.setId(SnowflakeManager.nextValue());
        userTokenEntity.setUserId(userCacheVo.getId());
        userTokenEntity.setToken(token);
        userTokenEntity.setUserInfo(JSON.toJSONString(userCacheVo));
        userTokenEntity.setExpireTime(expireTime);
        userTokenEntity.setLastAccessTime(LocalDateTime.now());
        save(userTokenEntity);
    }

    @Override
    public void updateUserInfoByUserId(Long userId, UserCacheVo userCacheVo, LocalDateTime expireTime) {
        if (Objects.isNull(userCacheVo) || Objects.isNull(userCacheVo.getId()) || Objects.isNull(expireTime)) {
            return;
        }
        update(new LambdaUpdateWrapper<>(UserTokenEntity.class).set(UserTokenEntity::getUserInfo, JSON.toJSONString(userCacheVo)).set(UserTokenEntity::getExpireTime, expireTime).eq(UserTokenEntity::getUserId, userId));
    }

    @Override
    public void removeTokenByToken(String token) {
        remove(new LambdaQueryWrapper<>(UserTokenEntity.class).eq(UserTokenEntity::getToken, token));
    }

    @Override
    public List<UserTokenEntity> listUserTokenByUserId(Long userId) {
        return list(new LambdaQueryWrapper<>(UserTokenEntity.class).eq(UserTokenEntity::getUserId, userId));
    }

    @Override
    public void removeTokenByUserIdAndLastAccessTime(Long userId, LocalDateTime timeoutTime) {
        remove(new LambdaQueryWrapper<>(UserTokenEntity.class).eq(UserTokenEntity::getUserId, userId).lt(UserTokenEntity::getLastAccessTime, timeoutTime));
    }
}
