package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserAddBo {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;
    /**
     * 密码
     */
    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    @NotBlank(message = "手机号不能为空")
    private String phone;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    private String nickName;
    /**
     * 角色id列表
     */
    @Schema(description = "角色id列表")
    private List<Long> roleIdList;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;
}
