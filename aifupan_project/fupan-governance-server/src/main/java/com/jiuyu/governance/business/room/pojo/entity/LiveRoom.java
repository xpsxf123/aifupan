package com.jiuyu.governance.business.room.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播间
 */
@Getter
@Setter
@TableName(value = "live_room")
public class LiveRoom {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @NotNull(message = "ID不能为null")
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_date")
    @NotNull(message = "创建时间不能为null")
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_date")
    @NotNull(message = "最后修改时间不能为null")
    private LocalDateTime updateDate;

    /**
     * 创建人
     */
    @TableField(value = "create_by")
    @NotNull(message = "创建人不能为null")
    private Long createBy;

    /**
     * 修改人
     */
    @TableField(value = "update_by")
    @NotNull(message = "修改人不能为null")
    private Long updateBy;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    @NotNull(message = "是否已删除不能为null")
    private Boolean isDeleted;

    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    @NotNull(message = "租户ID不能为null")
    private Long tenantId;

    /**
     * 主播唯一标识
     */
    @TableField(value = "sec_uid")
    @Size(max = 500,message = "主播唯一标识最大长度要小于 500")
    @NotBlank(message = "主播唯一标识不能为空")
    private String secUid;

    /**
     * 主页url
     */
    @TableField(value = "home_url")
    @Size(max = 2000,message = "主页url最大长度要小于 2000")
    @NotBlank(message = "主页url不能为空")
    private String homeUrl;

    /**
     * 直播间url
     */
    @TableField(value = "live_url")
    @Size(max = 2000,message = "直播间url最大长度要小于 2000")
    @NotBlank(message = "直播间url不能为空")
    private String liveUrl;

    /**
     * 主播名称
     */
    @TableField(value = "anchor_name")
    @Size(max = 500,message = "主播名称最大长度要小于 500")
    @NotBlank(message = "主播名称不能为空")
    private String anchorName;

    /**
     * 首播日期
     */
    @TableField(value = "debut_date")
    private LocalDate debutDate;

    /**
     * 主播头像
     */
    @TableField(value = "anchor_avatar")
    @Size(max = 2000,message = "主播头像最大长度要小于 2000")
    @NotBlank(message = "主播头像不能为空")
    private String anchorAvatar;

    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    @TableField(value = "platform")
    @NotNull(message = "平台类型 0：抖音 1：快手 2：视频号不能为null")
    private Integer platform;

    /**
     * 主播抖音号
     */
    @TableField(value = "anchor_number")
    @Size(max = 255,message = "主播抖音号最大长度要小于 255")
    @NotBlank(message = "主播抖音号不能为空")
    private String anchorNumber;

    /**
     * 行业id
     */
    @TableField(value = "trade_id")
    @NotNull(message = "行业id不能为null")
    private Long tradeId;

    /**
     * 所属公司ID
     */
    @TableField(value = "company_id")
    @NotNull(message = "所属公司ID不能为null")
    private Long companyId;

    /**
     * 所属部门ID
     */
    @TableField(value = "dept_id")
    @NotNull(message = "所属部门ID不能为null")
    private Long deptId;

    /**
     * 所属小组ID
     */
    @TableField(value = "team_id")
    @NotNull(message = "所属小组ID不能为null")
    private Long teamId;

    /**
     * 账号状态
     */
    @TableField(value = "account_status")
    private AccountStatus accountStatus;

}
