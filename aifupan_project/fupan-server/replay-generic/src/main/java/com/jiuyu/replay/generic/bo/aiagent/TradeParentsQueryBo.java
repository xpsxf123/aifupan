package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行业层级链查询入参（身份三件套 + 行业 ID）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TradeParentsQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 行业 ID
     */
    @NotNull(message = "tradeId不能为空")
    private Long tradeId;
}
