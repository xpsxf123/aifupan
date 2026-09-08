package com.jiuyu.governance.business.rbac.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 员工访问token
 *
 * @author HeHui
 * @date 2026-02-20 17:04
 */
@Getter
@Setter
@TableName(value = "employee_access_token")
public class EmployeeAccessToken {
    @TableId(value = "id", type = IdType.AUTO)
    @NotNull(message = "不能为null")
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @NotNull(message = "创建时间不能为null")
    private LocalDateTime createTime;

    /**
     * 访问token
     */
    @TableField(value = "access_token_hash")
    @Size(max = 100,message = "访问token最大长度要小于 100")
    @NotBlank(message = "访问token不能为空")
    private String accessTokenHash;

    /**
     * 登录IP
     */
    @TableField(value = "ip_address")
    @Size(max = 100,message = "登录IP最大长度要小于 100")
    @NotBlank(message = "登录IP不能为空")
    private String ipAddress;


    /**
     * 登录客户端类型 0未知
     */
    @TableField(value = "client_device")
    @NotNull(message = "登录客户端类型 0未知不能为null")
    private Integer clientDevice;

    /**
     * 客户端名称
     */
    @TableField(value = "device_name")
    @Size(max = 100,message = "客户端名称最大长度要小于 100")
    @NotBlank(message = "客户端名称不能为空")
    private String deviceName;

    /**
     * 过期时间
     */
    @TableField(value = "expire_time")
    @NotNull(message = "过期时间不能为null")
    private LocalDateTime expireTime;

    /**
     * 最近刷新（续期）时间
     */
    @TableField(value = "recently_refresh_time")
    @NotNull(message = "最近刷新（续期）时间不能为null")
    private LocalDateTime recentlyRefreshTime;

    /**
     * 员工ID
     */
    @TableField(value = "employee_id")
    @NotNull(message = "员工ID不能为null")
    private Long employeeId;

    /**
     * 登录状态 1 有效，0 无效
     */
    private Boolean tokenStatus;
}
