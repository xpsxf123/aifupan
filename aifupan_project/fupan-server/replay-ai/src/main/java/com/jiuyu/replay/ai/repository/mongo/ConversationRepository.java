package com.jiuyu.replay.ai.repository.mongo;

import com.jiuyu.replay.ai.entity.ConversationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午4:33
 */
@Component
public interface ConversationRepository extends MongoRepository<ConversationEntity, String> {
}
