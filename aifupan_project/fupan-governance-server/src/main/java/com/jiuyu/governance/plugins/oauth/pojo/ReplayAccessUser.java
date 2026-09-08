package com.jiuyu.governance.plugins.oauth.pojo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * replay系统的访问用户信息
 * <p>
 * 该类用于封装系统中的用户基本信息和权限相关信息，包括用户身份信息、权限等级、套餐信息等。
 * 这些信息将被用于生成JWT令牌和进行访问控制。
 * </p>
 *
 * @author HeHui
 * @apiNote 前端在接口上看到此参数请忽略
 * @date 2025-11-04 10:27
 */
@Getter
@Setter
public class ReplayAccessUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 8298653810104099273L;

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
     * 用户登录过的ip,前后用_隔开
     */
    private String ips;


    /**
     * 用户类型 0：普通用户 1：后台管理员 2：子账号
     */
    private Integer userType;
    /**
     * 当前激活的租户id
     */
    private Long activeTenantId;

    /**
     * 套餐名称
     */
    private String packageName;

    /**
     * 套餐等级
     */
    private Integer packageLevel;

    /**
     * 过期时间
     */
    private String expirationDate;


    /**
     * 业务服务的登录token
     */
    private String token;

    /**
     * 访问签名密钥
     */
    private String secret;
}
