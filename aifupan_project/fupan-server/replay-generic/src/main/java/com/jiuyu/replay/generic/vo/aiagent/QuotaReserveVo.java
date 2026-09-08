package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 算力额度预扣结果。
 *
 * @author fupan-server
 */
@Data
public class QuotaReserveVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否放行（false=余额不足）。 */
    private Boolean allowed;

    /** 预扣令牌（结算/释放时原样回传）；allowed=false 时为空。 */
    private String reserveId;

    /** 当前可用额度（预扣后的快照参考）。 */
    private Long available;

    /** 不放行原因（allowed=false 时填）。 */
    private String reason;
}
