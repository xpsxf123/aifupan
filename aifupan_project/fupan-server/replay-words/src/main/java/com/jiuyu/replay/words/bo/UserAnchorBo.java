package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserAnchorBo {

    /**
     * 主播SecUid
     */
    @Schema(description = "主播SecUid")
    private String secUid;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
}
