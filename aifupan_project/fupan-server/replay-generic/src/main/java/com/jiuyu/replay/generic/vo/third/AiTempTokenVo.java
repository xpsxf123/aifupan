package com.jiuyu.replay.generic.vo.third;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：获取临时token
 * @date ：2025/2/21 下午6:12
 */
@Data
public class AiTempTokenVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "token")
    private String token;

    @Schema(description = "来源类型 0：豆包，1：通义，2：DeepSeek")
    private Integer resourceType;

    @Schema(description = "模型角色")
    private String[] model;

    @Schema(description = "模型id")
    private String modelId;

    @Schema(description = "模型名称")
    private String[] modelName;

    @Schema(description = "模型code")
    private String modelCode;

    @Schema(description = "模型定义")
    private String modelDefinition;

    @Schema(description = "模型的使用方式 0上下文缓存对话，1对话")
    private Integer useModelWay;

    @Schema(description = "上下文缓存的类型")
    private String mode;

    @Schema(description = "用户截断的策略 " +
            "last_history_tokens:使用last_history_tokens模式，取决于使用的模型支持哪种Session 缓存；" +
            "rolling_tokens：使用rolling_tokens模式，取决于使用的模型支持哪种Session 缓存")
    private String truncationStrategyType;

    @Schema(description = "type设置为last_history_tokens时，进行设置。缓存存储的最大 token 数，触发该上限将根据模型上下文大小对缓存内容进行截断，截断顺序按照时间由远及近")
    private Integer lastHistoryTokens;

    @Schema(description = "type设置为rolling_tokens时，进行设置。在context历史消息长度接近模型上下文时，是否自动对历史上下文进行裁剪")
    private Boolean rollingTokens;

    @Schema(description = "会话的保存时间")
    private Integer contextSaveTime;

    @Schema(description = "临时token的保存时间")
    private Long tempTokenSaveTime;

    @Schema(description = "最多Ai发送消息长度")
    private Integer maxSendMessageLength;

    @Schema(description = "输出字数")
    private Integer outWordNum;
}
