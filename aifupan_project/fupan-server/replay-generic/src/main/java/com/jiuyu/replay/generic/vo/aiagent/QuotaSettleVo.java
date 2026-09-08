package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 算力额度结算结果。
 *
 * @author fupan-server
 */
@Data
public class QuotaSettleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否已结算（含本次扣减或幂等命中已结算）。 */
    private Boolean settled;

    /** 本次实际扣减 token（钳制到当前余额后的值；幂等命中为 0）。 */
    private Long deducted;

    /** 扣减后可用额度快照。 */
    private Long balanceAfter;

    /** 是否幂等命中（true=该 requestId 之前已结算/正在结算，未二次扣减）。 */
    private Boolean duplicate;
}
