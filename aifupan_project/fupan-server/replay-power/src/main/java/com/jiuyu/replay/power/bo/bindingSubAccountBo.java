package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "子账号参数-解绑和绑定")
public class bindingSubAccountBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "父账号id-不传使用当前登录人")
    @NotNull(message = "父账号id不能为空", groups = {adminBinding.class})
    private Long currentUserId;

    @Schema(description = "子账号id-解绑使用")
    @NotNull(message = "子账号id不能为空", groups = {unbind.class})
    private Long subUserId;

    @Schema(description = "子账号用户名-绑定使用-无用")
    private String subUserName;

    @Schema(description = "子账号手机号-绑定使用")
    @NotNull(message = "子账号手机号不能为空", groups = {adminBinding.class, binding.class})
    private String subPhone;

    @Schema(description = "绑定验证码-绑定使用")
    @NotNull(message = "验证码不能为空", groups = {adminBinding.class, binding.class})
    private String code;

    @Schema(description = "解绑原因-解绑使用")
    private String unbindReason;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;

    public static interface adminBinding {
    }

    public static interface binding {
    }

    public static interface unbind {
    }
}
