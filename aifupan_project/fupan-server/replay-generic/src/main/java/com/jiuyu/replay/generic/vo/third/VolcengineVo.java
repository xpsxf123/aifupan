package com.jiuyu.replay.generic.vo.third;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 上午11:41
 */
@Data
@Schema(description = "豆包AI的vo")
public class VolcengineVo {

    /**
     * 火山引擎accessKeyId
     */
    @Schema(description = "火山引擎accessKeyId")
    private String accessKeyId;

    /**
     * 火山引擎accessKeySecret
     */
    @Schema(description = "火山引擎accessKeySecret")
    private String secretAccessKey;

    /**
     * 火山引擎region
     */
    @Schema(description = "火山引擎region")
    private String region;

    /**
     * 火山引擎模型
     */
    @Schema(description = "火山引擎模型")
    private String[] model;

    /**
     * 火山引擎模型名称
     */
    @Schema(description = "火山引擎模型名称")
    private String[] modelName;

    /**
     * 默认模型名称
     */
    @Schema(description = "默认模型名称")
    private String defaultModel;

    /**
     * 上下文缓存的类型
     */
    private String mode;

    /**
     * 火山引擎apiKey
     */
    @Schema(description = "火山引擎apiKey")
    private String apiKey;

    /**
     * 火山引擎临时token保存时间
     */
    @Schema(description = "火山引擎临时token保存时间")
    public Integer tempTokenSaveTime;

    /**
     * 会话的保存时间
     */
    @Schema(description = "会话的保存时间")
    public Integer contextSaveTime;

    /**
     * 用户截断的策略
     * last_history_tokens:使用last_history_tokens模式，取决于使用的模型支持哪种Session 缓存；
     * rolling_tokens：使用rolling_tokens模式，取决于使用的模型支持哪种Session 缓存
     */
    @Schema(description = "用户截断的策略")
    public String truncationStrategyType;

    /**
     * type设置为last_history_tokens时，进行设置。缓存存储的最大 token 数，触发该上限将根据模型上下文大小对缓存内容进行截断，截断顺序按照时间由远及近
     */
    @Schema(description = "type设置为last_history_tokens时，进行设置。缓存存储的最大 token 数，触发该上限将根据模型上下文大小对缓存内容进行截断，截断顺序按照时间由远及近")
    public Integer lastHistoryTokens;

    /**
     * type设置为rolling_tokens时，进行设置。在context历史消息长度接近模型上下文时，是否自动对历史上下文进行裁剪
     */
    @Schema(description = "type设置为rolling_tokens时，进行设置。在context历史消息长度接近模型上下文时，是否自动对历史上下文进行裁剪")
    public Boolean rollingTokens;

    /**
     * 最多Ai发送消息长度
     */
    @Schema(description = "最多Ai发送消息长度")
    public Integer maxSendMessageLength;

}
