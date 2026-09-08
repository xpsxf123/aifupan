package com.jiuyu.replay.common.redisOperate;

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * &#064;description：有关ai的redis缓存
 * @date ：2025/7/5 下午7:12
 */
@Component
@Slf4j
public class AiRedisOperate {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // ai临时token缓存key
    public static final String AI_TEMP_ASK_TOKEN_KEY = "replay:ai:ai-temp-ask-token:{}";

    /**
     * 设置ai临时token
     *
     * @param aiModelCode ai模型Code
     * @param token       临时token
     * @return true/false
     */
    public boolean setAiTempArkToken(String aiModelCode, String token, long expire) {
        try {
            redisTemplate.opsForValue().set(CharSequenceUtil.format(AI_TEMP_ASK_TOKEN_KEY, aiModelCode), token, expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("设置ai临时token失败, error: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 获取ai临时token缓存key
     *
     * @param aiModelCode ai模型Code
     * @return ai临时token缓存key
     */
    public String getAiTempAskTokenKey(String aiModelCode) {
        return CharSequenceUtil.format(AI_TEMP_ASK_TOKEN_KEY, aiModelCode);
    }

    /**
     * 获取ai临时token
     *
     * @param aiModelCode ai模型Code
     * @return 临时token
     */
    public String getAiTempArkToken(String aiModelCode) {
        try {
            return redisTemplate.opsForValue().get(CharSequenceUtil.format(AI_TEMP_ASK_TOKEN_KEY, aiModelCode));
        } catch (Exception e) {
            log.error("获取ai临时token失败, error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取ai模型缓存时间
     *
     * @param aiModelCode ai模型Code
     * @return 缓存时间 单位：秒
     */
    public Long getAiModelExpire(String aiModelCode) {
        try {
            return redisTemplate.getExpire(CharSequenceUtil.format(AI_TEMP_ASK_TOKEN_KEY, aiModelCode), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取ai模型缓存时间失败, error: {}", e.getMessage());
            return null;
        }
    }


}
