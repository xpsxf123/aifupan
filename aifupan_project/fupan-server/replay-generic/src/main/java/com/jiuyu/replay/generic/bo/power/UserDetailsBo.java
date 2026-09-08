package com.jiuyu.replay.generic.bo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户详情信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Data
@Schema(description = "用户详情信息")
public class UserDetailsBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 渠道id
     */
    @Schema(description = "渠道id")
    private Long channelId;
}
