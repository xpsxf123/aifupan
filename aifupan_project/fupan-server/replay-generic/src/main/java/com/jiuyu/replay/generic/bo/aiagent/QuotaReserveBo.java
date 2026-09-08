package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 算力额度预扣（预校验）请求。
 *
 * <p>对话开跑前在热路径调用：按预估 token 预扣占位 + 余额校验。{@code requestId} 为本轮唯一幂等键
 * （取 AG-UI 的 runId），重试同一 requestId 返回同一预扣，不重复占位。</p>
 *
 * @author fupan-server
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuotaReserveBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /** 幂等键 = 本轮 runId。 */
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /** 预估消耗 token（账单口径，用于预扣额度；真实量在结算时纠正）。 */
    @NotNull(message = "estimateTokens不能为空")
    @Positive(message = "estimateTokens必须为正")
    private Long estimateTokens;
}
