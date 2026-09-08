package com.jiuyu.replay.ai.repository.mongo;

import com.jiuyu.replay.ai.entity.AiContextCacheEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiContextCacheRepository extends MongoRepository<AiContextCacheEntity, String> {
    Optional<AiContextCacheEntity> findByCacheKey(String cacheKey);
}
