package com.jiuyu.replay.ai.repository.service.impl;

import com.jiuyu.replay.ai.entity.AiContextCacheEntity;
import com.jiuyu.replay.ai.repository.mongo.AiContextCacheRepository;
import com.jiuyu.replay.ai.repository.service.AiContextCacheService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AiContextCacheServiceImpl implements AiContextCacheService {
    private final AiContextCacheRepository repository;

    @Override
    public String getDeepSeekMessages(String cacheKey) {
        try {
            Optional<AiContextCacheEntity> opt = repository.findByCacheKey(cacheKey);
            if (opt.isPresent()) {
                log.info("【ContextCache】DeepSeek消息读取成功: cacheKey={}", cacheKey);
                return opt.get().getMessages();
            }
            log.info("【ContextCache】DeepSeek消息不存在: cacheKey={}", cacheKey);
            return null;
        } catch (Exception e) {
            log.warn("【ContextCache】DeepSeek消息读取异常: cacheKey={}, error={}", cacheKey, e.getMessage());
            return null;
        }
    }

    @Override
    public void saveDeepSeekMessages(String cacheKey, String messagesJson, long ttlSeconds) {
        try {
            Date now = new Date();
            Date expireAt = new Date(System.currentTimeMillis() + ttlSeconds * 1000);
            repository.findByCacheKey(cacheKey).ifPresentOrElse(entity -> {
                entity.setMessages(messagesJson);
                entity.setExpireAt(expireAt);
                repository.save(entity);
                log.info("【ContextCache】DeepSeek消息更新成功: cacheKey={}, ttl={}s", cacheKey, ttlSeconds);
            }, () -> {
                AiContextCacheEntity entity = AiContextCacheEntity.builder()
                        .cacheKey(cacheKey)
                        .cacheType("DEEPSEEK_MESSAGE")
                        .messages(messagesJson)
                        .createTime(now)
                        .expireAt(expireAt)
                        .build();
                repository.save(entity);
                log.info("【ContextCache】DeepSeek消息创建成功: cacheKey={}, ttl={}s", cacheKey, ttlSeconds);
            });
        } catch (Exception e) {
            log.error("【ContextCache】DeepSeek消息写入失败: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getDoubaoSystemPrompt(String cacheKey) {
        try {
            Optional<AiContextCacheEntity> opt = repository.findByCacheKey(cacheKey);
            if (opt.isPresent()) {
                log.info("【ContextCache】Doubao系统提示词读取成功: cacheKey={}", cacheKey);
                return opt.get().getSystemPrompt();
            }
            log.info("【ContextCache】Doubao系统提示词不存在: cacheKey={}", cacheKey);
            return null;
        } catch (Exception e) {
            log.warn("【ContextCache】Doubao系统提示词读取异常: cacheKey={}, error={}", cacheKey, e.getMessage());
            return null;
        }
    }

    @Override
    public void saveDoubaoSystemPrompt(String cacheKey, String systemPrompt, long ttlSeconds) {
        try {
            Date now = new Date();
            Date expireAt = new Date(System.currentTimeMillis() + ttlSeconds * 1000);
            repository.findByCacheKey(cacheKey).ifPresentOrElse(entity -> {
                entity.setSystemPrompt(systemPrompt);
                entity.setExpireAt(expireAt);
                repository.save(entity);
                log.info("【ContextCache】Doubao系统提示词更新成功: cacheKey={}, ttl={}s", cacheKey, ttlSeconds);
            }, () -> {
                AiContextCacheEntity entity = AiContextCacheEntity.builder()
                        .cacheKey(cacheKey)
                        .cacheType("DOUBAO_SYSTEM")
                        .systemPrompt(systemPrompt)
                        .createTime(now)
                        .expireAt(expireAt)
                        .build();
                repository.save(entity);
                log.info("【ContextCache】Doubao系统提示词创建成功: cacheKey={}, ttl={}s", cacheKey, ttlSeconds);
            });
        } catch (Exception e) {
            log.error("【ContextCache】Doubao系统提示词写入失败: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            throw e;
        }
    }
}
