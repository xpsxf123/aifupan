package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 录制账户响应
 *
 * @author HeHui
 * @date 2026-03-24 11:16
 */
@Getter
@Setter
public class ReplayAccountResponse {

    /**
     * ID
     */
    private Long id;
    /**
     * 登录账号
     */
    private String username;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 密码
     */
    private String password;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 创建时间
     */
    private LocalDateTime createDate;
    /**
     * 用户登录过的ip,前后用_隔开
     */
    private String ips;
    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    private Integer status;
    /**
     * 上级用户id
     */
    private Long parentId;
    /**
     * 用户类型 0：普通用户 1：后台管理员 2：子账号
     */
    private Integer userType;
    /**
     * 当前激活的租户id
     */
    private Long activeTenantId;
    /**
     * 邀请链接的code
     */
    private String inviteUrlCode;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    private Integer adminUserType;

}
