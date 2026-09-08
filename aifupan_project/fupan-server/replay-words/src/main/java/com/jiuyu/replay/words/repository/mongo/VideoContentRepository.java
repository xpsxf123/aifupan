package com.jiuyu.replay.words.repository.mongo;

import com.jiuyu.replay.words.entity.VideoContentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public interface VideoContentRepository extends MongoRepository<VideoContentEntity, String> {

}
