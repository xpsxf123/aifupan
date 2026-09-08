package com.jiuyu.replay.power.repository.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.entity.UserLoginInfoEntity;
import com.jiuyu.replay.power.repository.dao.UserLoginInfoDao;
import com.jiuyu.replay.power.repository.service.UserLoginInfoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户登录信息表 服务实现类
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Service
public class UserLoginInfoServiceImpl extends ServiceImpl<UserLoginInfoDao, UserLoginInfoEntity> implements UserLoginInfoService {

    @Resource
    UserLoginInfoDao userLoginInfoDao;

    @Override
    public void updateUserLoginInfoExpireTimeByUserIdAndToken(Long userId, String token, LocalDateTime expireTime) {
        userLoginInfoDao.updateUserLoginInfoExpireTimeByUserIdAndToken(userId, token, expireTime);
    }

    @Override
    public void batchRemoveToken(List<String> batchTokens) {
        remove(new LambdaQueryWrapper<>(UserLoginInfoEntity.class).in(UserLoginInfoEntity::getToken, batchTokens));
    }

    @Override
    public void saveUserLoginInfo(Long userId, String token, String source, String fingerprint, LocalDateTime expireTime) {
        UserLoginInfoEntity userLoginInfoEntity = new UserLoginInfoEntity();
        userLoginInfoEntity.setId(SnowflakeManager.nextValue());
        userLoginInfoEntity.setUserId(userId);
        userLoginInfoEntity.setToken(token);
        userLoginInfoEntity.setSourceInfo(source);
        userLoginInfoEntity.setFingerprint(fingerprint);
        userLoginInfoEntity.setLastRequestTime(LocalDateTime.now());
        userLoginInfoEntity.setExpireTime(expireTime);
        save(userLoginInfoEntity);
    }

    @Override
    public void removeUserLoginInfoByUserId(Long userId) {
        remove(new LambdaQueryWrapper<>(UserLoginInfoEntity.class).eq(UserLoginInfoEntity::getUserId, userId));
    }

    @Override
    public void removeUserLoginInfoByUserIdAndLastRequestTime(Long userId, LocalDateTime timeoutTime) {
        remove(new LambdaQueryWrapper<>(UserLoginInfoEntity.class).eq(UserLoginInfoEntity::getUserId, userId).lt(UserLoginInfoEntity::getLastRequestTime, timeoutTime));
    }

    @Override
    public void updateUserLoginInfoExpireTimeByUserId(Long userId, LocalDateTime expireTime) {
        update(new LambdaUpdateWrapper<>(UserLoginInfoEntity.class).set(UserLoginInfoEntity::getExpireTime, expireTime).eq(UserLoginInfoEntity::getUserId, userId));
    }

    @Override
    public void removeUserLoginInfoByToken(String token) {
        remove(new LambdaQueryWrapper<>(UserLoginInfoEntity.class).eq(UserLoginInfoEntity::getToken, token));
    }

    @Override
    public UserLoginInfoEntity getUserLoginInfoByUserIdAndToken(Long userId, String token) {
        return getOne(new LambdaQueryWrapper<>(UserLoginInfoEntity.class).eq(UserLoginInfoEntity::getUserId, userId).eq(UserLoginInfoEntity::getToken, token));
    }
}
