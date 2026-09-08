package com.jiuyu.replay.generic.bo.words.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 设置主播行业参数
 *
 * @author AI Assistant
 */
@Data
@Schema(description = "设置主播行业参数")
public class SetAnchorTradeBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主播SecUid
     */
    @NotBlank(message = "主播secUid不能为空")
    @Schema(description = "主播SecUid", requiredMode = Schema.RequiredMode.REQUIRED)
    private String secUid;

    /**
     * 系统行业ID
     */
    @Schema(description = "系统行业ID")
    private Long systemTradeId;

    /**
     * AI纠正后的行业ID
     */
    @Schema(description = "AI纠正后的行业ID")
    private Long aiCorrectTradeId;
}
