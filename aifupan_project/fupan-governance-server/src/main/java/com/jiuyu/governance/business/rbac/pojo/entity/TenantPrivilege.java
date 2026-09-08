package com.jiuyu.governance.business.rbac.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户权限&权益
 */
@Getter
@Setter
@TableName(value = "tenant_privilege")
public class TenantPrivilege {
    /**
     * 租户ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    @NotNull(message = "租户ID不能为null")
    private Long id;

    /**
     * 账户状态 0停用，1正常,2 冻结
     */
    @TableField(value = "account_status")
    @NotNull(message = "账户状态 0停用，1正常不能为null")
    private Integer accountStatus;

    /**
     * 首次开通时间
     */
    @TableField(value = "first_open_time")
    @NotNull(message = "首次开通时间不能为null")
    private LocalDateTime firstOpenTime;

    /**
     * 最近一次的开通时间
     */
    @TableField(value = "recently_open_time")
    @NotNull(message = "最近一次的开通时间不能为null")
    private LocalDateTime recentlyOpenTime;

    /**
     * 最近冻结时间
     */
    @TableField(value = "recently_freeze_time")
    private LocalDateTime recentlyFreezeTime;

    /**
     * 操作冻结的爱复盘后台用户ID
     */
    @TableField(value = "freeze_user_id")
    private Long freezeUserId;

    /**
     * 首次开通账户的爱复盘后台管理人
     */
    @TableField(value = "first_open_user_id")
    @NotNull(message = "首次开通账户的爱复盘后台管理人不能为null")
    private Long firstOpenUserId;

    /**
     * 最近一次开通账户的爱复盘后台管理人
     */
    @TableField(value = "recently_open_user_id")
    @NotNull(message = "最近一次开通账户的爱复盘后台管理人不能为null")
    private Long recentlyOpenUserId;
}
