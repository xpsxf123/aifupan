package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 算力额度预扣释放请求（取消/报错且未结算时撤销预扣）。
 *
 * <p>仅用于「跑到一半被取消、根本没结算」的轮；已结算的轮预扣已随结算释放，再调本接口为无副作用 no-op。</p>
 *
 * @author fupan-server
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuotaReleaseBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /** 幂等键 = 本轮 runId。 */
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /** 预扣令牌（reserve 返回的 reserveId，原样回传）。 */
    @NotBlank(message = "reserveId不能为空")
    private String reserveId;
}
