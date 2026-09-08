package com.jiuyu.replay.ai.repository.service;

public interface AiContextCacheService {

    String getDeepSeekMessages(String cacheKey);

    void saveDeepSeekMessages(String cacheKey, String messagesJson, long ttlSeconds);

    String getDoubaoSystemPrompt(String cacheKey);

    void saveDoubaoSystemPrompt(String cacheKey, String systemPrompt, long ttlSeconds);
}
