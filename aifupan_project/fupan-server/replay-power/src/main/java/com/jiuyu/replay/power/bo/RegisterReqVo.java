package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "预约注册")
public class RegisterReqVo {

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 验证码
     */
    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空")
    private String code;

    /**
     * 公司名称
     */
    @Schema(description = "公司名称")
    private String companyName;

    /**
     * 行业id
     */
    @Schema(description = "行业id, 优先填二级行业，如果没有二级有一级就填一级，都没有就不填了")
    private Long tradeId;

    @Schema(description = "渠道Code")
    private String inviteUrlCode;
}
