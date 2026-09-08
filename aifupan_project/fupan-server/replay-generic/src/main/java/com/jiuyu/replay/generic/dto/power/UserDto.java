package com.jiuyu.replay.generic.dto.power;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author RayChou
 * @date 2025/6/5 16:03
 */
@Data
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private Date createDate;
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
}
