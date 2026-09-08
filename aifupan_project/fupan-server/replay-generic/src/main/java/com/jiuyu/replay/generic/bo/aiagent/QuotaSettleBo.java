package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * AI 算力额度结算请求（事后按真实账单 token 扣减）。
 *
 * <p>本轮结束后调用：按 {@code billedTokens} 真实扣减并释放预扣。{@code requestId} 幂等键保证重复结算不重复扣
 * （服务端去重，重放返回首次结果）。{@code reserveId} 为预扣令牌（reserve 返回原样回传）；reserve 曾失败（fail-open）
 * 时可空，此时直接扣减。</p>
 *
 * @author fupan-server
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuotaSettleBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /** 幂等键 = 本轮 runId。 */
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /** 预扣令牌（reserve 返回的 reserveId，原样回传）；reserve 失败 fail-open 时可空。 */
    private String reserveId;

    /** 本轮真实账单 token（已 ×倍率×缓存折扣，向下取整）。 */
    @NotNull(message = "billedTokens不能为空")
    @PositiveOrZero(message = "billedTokens不能为负")
    private Long billedTokens;

    /** 分模型账单明细（审计/归因，可空）。 */
    private List<ModelBilled> byModel;

    /** 会话标识（审计，可空，取 threadId）。 */
    private String sessionId;

    /** 本轮发生时间（毫秒时间戳，审计，可空）。 */
    private Long occurredAt;

    /** 单模型账单明细。 */
    @Data
    public static class ModelBilled implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 模型编码/名称。 */
        private String modelCode;

        /** 该模型本轮账单 token。 */
        private Long billed;
    }
}
