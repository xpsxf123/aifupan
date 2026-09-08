package com.jiuyu.replay.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 上下文缓存
 *
 * @author lujie
 * @date 2026/06/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "replay_ai_context_cache")
public class AiContextCacheEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("cacheKey")
    private String cacheKey;

    @Field("cacheType")
    private String cacheType;

    @Field("messages")
    private String messages;

    @Field("systemPrompt")
    private String systemPrompt;

    @Field("createTime")
    private Date createTime;

    @Field("expireAt")
    private Date expireAt;
}
