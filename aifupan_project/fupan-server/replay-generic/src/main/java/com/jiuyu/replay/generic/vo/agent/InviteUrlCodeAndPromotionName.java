package com.jiuyu.replay.generic.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/2 20:49
 */
@Data
public class InviteUrlCodeAndPromotionName {

    @Schema(description = "邀请链接code")
    private String urlCode;

    @Schema(description = "渠道名称")
    private String promotionName;

}
