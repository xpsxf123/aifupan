package com.jiuyu.replay.words.bo.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "置顶主播参数")
public class TopAnchorBo {

    /**
     * 主播secuid
     */
    @Schema(description = "主播secuid")
    private String secUid;
    /**
     * 动作 0：取消置顶 1：置顶
     */
    @Schema(description = "动作 0：取消置顶 1：置顶")
    private Integer action;
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
