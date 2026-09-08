package com.jiuyu.replay.common.utils;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 在方法中操作redis的工具类
 */
public class RedisOperationUtils {

    static final RedisTemplate redisTemplate = (RedisTemplate) ApplicationContextUtil.getBean("redisTemplate");

    /**
     * 删除实体类的缓存
     * @param name
     * @param key
     */
    public static void deleteEntityCache(String name, Object key) {
        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.entityCacheKey, name, key));
    }

    /**
     * 获取缓存中的数据
     * @param key
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T get(String key, Class<T> clazz) {
        return BeanUtil.copyProperties(redisTemplate.opsForValue().get(key), clazz);
    }

    /**
     * 使用scan命令获取所有匹配指定模式的key
     *
     * @param pattern 匹配模式，如"user:*"
     * @return 匹配模式的所有key集合
     */
    public static Set<String> scanKeys(String pattern) {

        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

        Set<String> keys = new HashSet<>();
        try (Cursor<byte[]> cursor = (Cursor<byte[]>) redisTemplate.executeWithStickyConnection(connection  ->
                connection.scan(options)))  {

            while (cursor.hasNext())  {
                keys.add(new  String(cursor.next(),  StandardCharsets.UTF_8)); // 显式指定字符集
            }
        } catch (Exception e) {
            throw new RuntimeException("SCAN操作失败", e);
        }
        return keys;
    }
}
