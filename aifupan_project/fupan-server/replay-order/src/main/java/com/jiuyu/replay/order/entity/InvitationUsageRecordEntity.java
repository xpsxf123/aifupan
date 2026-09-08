package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：liwj
 * @description：用户使用邀请码记录表实体类
 * @date ：2025/9/19 22:06
 */
@Data
@TableName("tb_invitation_usage_record")
public class InvitationUsageRecordEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键（对应表中id字段）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 批次id（对应表中invitation_code_batch_id字段，非空）
     */
    private Long invitationCodeBatchId;

    /**
     * 邀请码id（对应表中invitation_code_id字段，非空）
     */
    private Long invitationCodeId;

    /**
     * 邀请码（对应表中invitation_code字段，非空）
     */
    private String invitationCode;

    /**
     * 租户id（对应表中tenant_id字段，默认0）
     */
    private Long tenantId = 0L;
    /**
     * 使用的用户id（对应表中user_id字段，默认0）
     */
    private Long userId;

    /**
     * 使用后关联的订单id（对应表中order_id字段，默认0）
     */
    private Long orderId;

    /**
     * 创建人（对应表中create_id字段，非空）
     */
    private Long createId;

    /**
     * 创建时间（对应表中create_date字段，非空）
     * 插入时自动填充，无需手动设置
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

    /**
     * 修改人（对应表中update_id字段，非空）
     * 插入/更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateId;

    /**
     * 最后修改时间（对应表中update_date字段，非空）
     * 插入/更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;

    /**
     * 是否已删除（对应表中is_deleted字段，默认0：未删除，1：已删除）
     * MyBatis-Plus逻辑删除字段，需配置全局逻辑删除规则
     */
    @TableLogic
    private Integer isDeleted = 0;
}
