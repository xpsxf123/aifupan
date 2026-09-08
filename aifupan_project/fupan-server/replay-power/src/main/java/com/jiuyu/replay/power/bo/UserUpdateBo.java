package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateBo {


    /**
     * id
     */
    @Schema(description = "id")
    @NotBlank(message = "id不能为空")
    private Long id;
    /**
     * 用户名
     */
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;
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

    @Schema(description = "账号类型")
    private Integer userType;

    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    private Integer status;

    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;

}
