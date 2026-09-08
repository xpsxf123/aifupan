package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
@Schema(description = "修改密码")
public class UpdatePasswordBo {

    /**
     * 原密码
     */
    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空")
    private String password;
    /**
     * 新密码
     */
    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    /**
     * 确认密码
     */
    @Schema(description = "二次密码")
    @NotBlank(message = "二次密码不能为空")
    private String checkPassword;
}
