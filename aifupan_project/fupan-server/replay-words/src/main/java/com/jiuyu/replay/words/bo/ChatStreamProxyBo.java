package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 服务端AI流式对话代理请求参数（供C#客户端调用，服务端持有API Key转发到各厂商API）
 *
 * @author lujie
 * @date 2026/5/7
 */
@Data
public class ChatStreamProxyBo {

    @Schema(description = "AI模型配置表的ID")
    private Long modelId;

    @Schema(description = "AI身份设定（system prompt）")
    private String identity;

    @Schema(description = "组装好的提问内容")
    private String realContent;

    @Schema(description = "推理类型：enabled/disabled")
    private String thinkingType;

    @Schema(description = "上下文缓存ID（useModelWay=0时必传，从assemblePrompt接口获取）")
    private String contextId;

    @Schema(description = "模型使用方式 0=上下文缓存对话, 1=简单对话")
    private Integer useModelWay;
}
