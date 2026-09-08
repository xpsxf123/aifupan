package com.jiuyu.replay.generic.vo.words.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/25 15:06
 */
@Data
public class AnchorUrlTradeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主播")
    private String secUid;

    @Schema(description = "行业id")
    private Long tradeId;

    @Schema(description = "行业名称")
    private String tradeName;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "租房id")
    private Long tenantId;

    @Schema(description = "行业人数")
    private Integer userCount;

}
