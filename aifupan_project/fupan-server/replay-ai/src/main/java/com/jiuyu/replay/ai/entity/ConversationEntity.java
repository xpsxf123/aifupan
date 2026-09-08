package com.jiuyu.replay.ai.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午3:30
 */
@Data
@Schema(description = "ai记录")
@Document(collection = "replay_ai_conversation")
@CompoundIndexes({
        @CompoundIndex(name = "sourceId_sourceType_userId_tenantId_askType_index", def = "{ 'sourceId': 1, 'sourceType': 1, 'userId': 1, 'tenantId': 1, 'askType': 1 }")
})
public class ConversationEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    /**
     * 来源id
     */
    @Field("sourceId")
    private String sourceId;

    /**
     * 来源类型 0视频，1文件，2对比分析
     */
    @Field("sourceType")
    private Integer sourceType;

    /**
     * 用户id
     */
    @Field("userId")
    private Long userId;

    /**
     * 租户id
     */
    @Field("tenantId")
    private Long tenantId;

    /**
     * code
     */
    @Field("code")
    private String code;

    /**
     * 助手类型
     */
    @Field("askType")
    private Integer askType;

    /**
     * 一次 chat completion 接口调用的唯一标识
     */
    @Field("completionId")
    private String completionId;

    /**
     * 提示词id
     */
    @Field("cueWordsId")
    private Long cueWordsId;

    /**
     * 提示词类型 0系统，1用户
     */
    @Field("cueWordsType")
    private Integer cueWordsType;

    /**
     * 问答的code，一问一答的code的是一样的
     */
    @Field("qaCode")
    private String qaCode;

    /**
     * 会话id
     */
    @Field("contextId")
    private String contextId;

    /**
     * 数据类型 Q答，A问
     */
    @Field("type")
    private String type;

    /**
     * 内容
     */
    @Field("content")
    private String content;

    /**
     * 问AI的真实问题
     */
    @Field("realContent")
    private String realContent;

    /**
     * 点赞状态 -1未点赞 0点赞，1踩
     */
    @Field("giveStatuc")
    private Integer giveStatuc;

    /**
     * 创建时间
     */
    @Field("createDate")
    private String createDate;

    /**
     * 创建时间戳
     */
    @Indexed(direction = IndexDirection.DESCENDING)
    @Field("createTime")
    private Long createTime;

    /**
     * 生成html方式  0：服务器生成，1：客户端生成
     */
    @Field("htmlType")
    private Integer htmlType;

    /**
     * html生成状态 0：待生成，1：生成中，2：生成成功，3：生成失败
     */
    @Field("htmlStatus")
    private Integer htmlStatus;

    /**
     * html生成时间
     */
    @Field("htmlCreateDate")
    private String htmlCreateDate;

    /**
     * html保存路径
     */
    @Field("htmlSavePath")
    private String htmlSavePath;

    /**
     * html生成错误原因
     */
    @Field("htmlCreateError")
    private String htmlCreateError;

    /**
     * 上一次对话的id
     */
    @Field("lastConversationId")
    private String lastConversationId;

    /**
     * 优化文本
     */
    @Field("optimizeText")
    private String optimizeText;

    /**
     * 额外要求
     */
    @Field("extraRequire")
    private String extraRequire;

    /**
     * 提问类型：0：正常问题，1：重新提问
     */
    @Field("questionType")
    private Integer questionType;

    /**
     * 分析类型 0-普通分析，1-综合分析
     */
    @Field("analysisType")
    private Integer analysisType;

    /**
     * ai纠错状态 0：正常，1：纠错中，2：纠错完成
     */
    @Field("aiCorrectStatus")
    private Integer aiCorrectStatus;

    /**
     * 纠错来源类型 0：服务器生成，1：客户端生成
     */
    @Field("aiCorrectType")
    private String aiCorrectType;

    /**
     * ai纠错错误原因
     */
    @Field("aiCorrectError")
    private String aiCorrectError;

    /**
     * ai纠错创建时间
     */
    @Field("aiCorrectCreateTime")
    private Long aiCorrectCreateTime;
}
