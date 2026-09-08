package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.ibatis.annotations.Insert;

@Data
@Schema(description = "注册")
public class RegisterBo {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;
    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    @NotBlank(message = "手机号不能为空", groups = {Insert.class})
    private String phone;
    /**
     * 邀请码
     */
    @Schema(description = "邀请码")
    private String invitationCode;
    /**
     * 验证码
     */
    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空", groups = {Insert.class})
    private String code;
    /**
     * 邀请链接的code
     */
    @Schema(description = "邀请链接的code")
    private String inviteUrlCode;
    /**
     * 平台销售人员id
     */
    @Schema(description = "平台销售人员id")
    private Long saleId;
    /**
     * 来源渠道id
     */
    @Schema(description = "来源渠道id")
    private Long channelId;
    /**
     * 代理商销售人员id
     */
    @Schema(description = "代理商销售人员id")
    private Long agentSaleId;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;

    /**
     * 代理商id
     */
    private Long agentId;
}
