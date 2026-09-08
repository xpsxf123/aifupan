package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 提示词组装结果（供C#客户端调用，服务器完成全部提示词拼装后返回）
 *
 * @author lujie
 * @date 2026/4/28
 */
@Data
public class PromptAssemblyResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "完全组装好的提问内容（已替换占位符，已添加额外要求，已追加重新提问上下文）")
    private String assembledPrompt;

    @Schema(description = "AI身份设定（system prompt）")
    private String identity;

    @Schema(description = "上下文缓存ID（useModelWay=0时有值）")
    private String contextId;

    @Schema(description = "模型使用方式 0=上下文缓存对话, 1=简单对话")
    private Integer useModelWay;

    @Schema(description = "模型配置信息，包含token/modelId/max_tokens等，C#客户端用此调用Volcengine")
    private AiTempTokenVo modelConfig;

    @Schema(description = "上下文缓存Redis Key（useModelWay=0时有值，用于TTL刷新）")
    private String contextRedisKey;

    @Schema(description = "系统提示词（首次创建上下文时有值，C#客户端用于 Responses API 的 instructions）")
    private String systemPrompt;

    @Schema(description = "上下文缓存占位符 key 列表（前端调用 updateContextId 刷新 TTL 时需原样回传）")
    private List<String> systemPromptKeys;
}
