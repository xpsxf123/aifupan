package com.jiuyu.replay.generic.vo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "用户")
public class UserVo {

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
     * 密码
     */
    @Schema(description = "密码")
    private String password;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 用户登录过的ip,前后用_隔开
     */
    @Schema(description = "用户登录过的ip,前后用_隔开")
    private String ips;
    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    @Schema(description = "冻结状态 0：未冻结 1：已冻结")
    private Integer status;
    /**
     * 上级用户id
     */
    @Schema(description = "上级用户id")
    private Long parentId;
    /**
     * 用户类型 0：普通用户 1：后台管理员 2：子账号
     */
    @Schema(description = "用户类型 0：普通用户 1：后台管理员 2：子账号")
    private Integer userType;
    /**
     * 当前激活的租户id
     */
    @Schema(description = "当前激活的租户id")
    private Long activeTenantId;
    /**
     * 邀请链接的code
     */
    @Schema(description = "邀请链接的code")
    private String inviteUrlCode;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;
}
