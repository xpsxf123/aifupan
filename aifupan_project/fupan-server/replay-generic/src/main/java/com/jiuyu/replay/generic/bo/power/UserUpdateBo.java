package com.jiuyu.replay.generic.bo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改用户信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@Data
@Schema(description = "修改用户信息")
public class UserUpdateBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String username;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 用户类型 0：普通用户 1：后台管理员 2：子账号
     */
    @Schema(description = "用户类型 0：普通用户 1：后台管理员 2：子账号")
    private Integer userType;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;
}
