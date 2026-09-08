package com.jiuyu.replay.common.redisOperate;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/16 17:18
 */
@Component
public class GenerateHtmlRedisOPerate {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // 缓存key {userId_tenantId_videoId}
    public static final String KEY = "replay:ai:dataDiagnosisGenerate:{}";

    public static final int timeout = 2;

    /**
     * 删除整个键
     *
     * @param secUid 用户标识
     */
    public void deleteAll(String secUid) {
        String key = StrUtil.format(KEY, secUid);
        redisTemplate.delete(key);
    }

    private String getKey(Long userId, Long tenantId, String secUid) {
        return StrUtil.format(KEY, userId + "_" + tenantId + "_" + secUid);
    }

    /**
     * 删除某个videoId
     *
     * @param userId  用户ID
     * @param tenantId 租户ID
     * @param secUid  用户标识
     * @param videoId 删除的videoId
     */
    public Long delete(Long userId, Long tenantId, String secUid, String videoId) {
        String key = getKey(userId, tenantId, secUid);
        return redisTemplate.opsForSet().remove(key, videoId);
    }

    /**
     * 查询对应的键有多少个videoId
     *
     * @param userId  用户ID
     * @param tenantId 租户ID
     * @param secUid 用户标识
     * @return videoId的数量
     */
    public long size(Long userId, Long tenantId, String secUid) {
        String key = getKey(userId, tenantId, secUid);
        Long size = redisTemplate.opsForSet().size(key);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    /**
     * 保存videoId到set中
     *
     * @param userId  用户ID
     * @param tenantId 租户ID
     * @param secUid  用户标识
     * @param videoId 视频ID
     */
    public void save(Long userId, Long tenantId, String secUid, String videoId) {
        String key = getKey(userId, tenantId, secUid);
        redisTemplate.opsForSet().add(key, videoId);
        // 设置过期时间为2小时
        redisTemplate.expire(key, timeout, TimeUnit.HOURS);
    }

    /**
     * 删除整个键，并保存videoId
     *
     * @param userId  用户ID
     * @param tenantId 租户ID
     * @param secUid  用户标识
     * @param videoId 删除的videoId
     */
    public void deleteAndSave(Long userId, Long tenantId, String secUid, String videoId) {
        String key = getKey(userId, tenantId, secUid);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().add(key, videoId);
        redisTemplate.expire(key, timeout, TimeUnit.HOURS);
    }
}
