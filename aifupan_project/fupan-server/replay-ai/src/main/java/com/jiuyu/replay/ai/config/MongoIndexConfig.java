package com.jiuyu.replay.ai.config;

import com.jiuyu.replay.ai.entity.AiContextCacheEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initAiContextCacheIndex() {
        mongoTemplate.indexOps(AiContextCacheEntity.class)
                .ensureIndex(new Index().on("cacheKey", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps(AiContextCacheEntity.class)
                .ensureIndex(new Index().on("expireAt", Sort.Direction.ASC).expire(0));
        log.info("【ContextCache】索引已确保: replay_ai_context_cache (cacheKey唯一索引, expireAt TTL索引)");
    }
}
