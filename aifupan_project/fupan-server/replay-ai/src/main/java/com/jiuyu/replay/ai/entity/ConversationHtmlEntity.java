package com.jiuyu.replay.ai.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/18 11:42
 */
@Data
@Schema(description = "ai记录")
@Document(collection = "replay_ai_conversation_html")
@CompoundIndexes({
        @CompoundIndex(name = "conversationId_isDelected_index", def = "{ 'conversationId': 1, 'isDelected': 0 }")
})
public class ConversationHtmlEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    /**
     * 来源id
     */
    @Field("conversationId")
    private String conversationId;

    /**
     * html内容
     */
    @Field("htmlContent")
    private String htmlContent;

    /**
     * 生成html方式
     */
    @Field("htmlType")
    private Integer htmlType;

    /**
     * 创建时间
     */
    @Field("createDate")
    private String createDate;

    /**
     * 删除状态
     */
    @Field("isDelected")
    private Integer isDelected = 0;
}
