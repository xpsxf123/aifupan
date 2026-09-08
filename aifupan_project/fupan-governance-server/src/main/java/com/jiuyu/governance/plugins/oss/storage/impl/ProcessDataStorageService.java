package com.jiuyu.governance.plugins.oss.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.governance.plugins.oss.core.OssTemplate;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import com.jiuyu.governance.plugins.oss.storage.AbstractStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 过程数据存储服务
 * <p>
 * 存储直播/商品过程数据（governance-process 路径前缀）。
 * 下载时缓存到 Redis（5小时），上传时清除对应缓存。
 * </p>
 *
 * @author lj
 */
@Service
@Slf4j
public class ProcessDataStorageService extends AbstractStorageService {

    /** Redis缓存前缀 */
    private static final String CACHE_PREFIX = "governance:process:oss:";
    /** 缓存过期时间（小时） */
    private static final int CACHE_TTL_HOURS = 5;

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public ProcessDataStorageService(OssTemplate ossTemplate, ObjectMapper objectMapper,
                                     StringRedisTemplate stringRedisTemplate) {
        super(ossTemplate);
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public OssBucket getBucket() {
        return OssBucket.ANALYSIS_DATA;
    }

    @Override
    protected String getPathPrefix() {
        return "governance-process";
    }

    /**
     * 从 OSS 下载过程数据，带 Redis 缓存（5小时过期）。
     * <p>
     * ossKey 是完整的 OSS Key（含环境前缀和路径前缀），直接使用。
     * 优先从 Redis 读取，未命中再走 OSS，写入后回种缓存。
     * </p>
     */
    public <T> List<T> downloadProcessDataFromOss(String ossKey, Class<T> clazz) {
        String cacheKey = CACHE_PREFIX + ossKey;

        // 优先从Redis读取
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                T[] arr = objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructArrayType(clazz));
                return Arrays.asList(arr);
            }
        } catch (Exception e) {
            log.debug("[业绩] 读取过程数据缓存失败，回退到OSS, ossKey={}", ossKey, e);
        }

        // 缓存未命中，从OSS下载
        try {
            byte[] data = this.downloadAsBytes(ossKey);
            T[] arr = objectMapper.readValue(data,
                    objectMapper.getTypeFactory().constructArrayType(clazz));
            List<T> result = Arrays.asList(arr);

            // 回种Redis缓存
            try {
                String json = objectMapper.writeValueAsString(arr);
                stringRedisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("[业绩] 回种过程数据缓存失败, ossKey={}", ossKey, e);
            }

            return result;
        } catch (Exception e) {
            log.warn("[业绩] 从 OSS 下载过程数据失败，降级使用空数据, ossKey={}", ossKey, e);
            return Collections.emptyList();
        }
    }

    /**
     * 上传过程数据到 OSS，同时清除对应 Redis 缓存。
     */
    @Override
    public String upload(String bizPath, byte[] bytes, String contentType) {
        String fullKey = super.upload(bizPath, bytes, contentType);

        // 上传后清除缓存，确保下次读取拿到最新数据
        String cacheKey = CACHE_PREFIX + fullKey;
        try {
            stringRedisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("[业绩] 清除过程数据缓存失败, cacheKey={}", cacheKey, e);
        }

        return fullKey;
    }
}
