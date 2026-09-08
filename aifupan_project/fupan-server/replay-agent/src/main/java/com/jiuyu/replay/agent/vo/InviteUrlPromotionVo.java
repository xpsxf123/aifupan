package com.jiuyu.replay.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Data
@Schema(description = "邀请链接的code和推广渠道信息项")
public class InviteUrlPromotionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邀请链接的code id
     */
    @Schema(description = "邀请链接的code id")
    private Long id;

    /**
     * url链接code
     */
    @Schema(description = "邀请链接的url链接code")
    private String urlCode;

    /**
     * 代理商推广渠道id
     */
    @Schema(description = "代理商推广渠道id")
    private Long promotionId;

    /**
     * 代理商推广渠道名称
     */
    @Schema(description = "代理商推广渠道名称")
    private String promotionName;



}
