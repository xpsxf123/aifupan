package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 订阅分组表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_user_subscription_group")
@Schema(description = "订阅分组表")
public class VideoUserSubscriptionGroupEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    /**
     * 分组类型: 1-达人订阅, 2-爆款订阅
     */
    @TableField("group_type")
    @Schema(description = "分组类型: 1-达人订阅, 2-爆款订阅", example = "1")
    private Byte groupType;

    /**
     * 分组名称
     */
    @TableField("group_name")
    @Schema(description = "分组名称", example = "美食达人")
    private String groupName;

    /**
     * 分组描述
     */
    @TableField("group_description")
    @Schema(description = "分组描述", example = "专注美食内容的达人分组")
    private String description;

    /**
     * 是否默认分组: 0-否, 1-是
     */
    @TableField("is_default")
    @Schema(description = "是否默认分组: 0-否, 1-是", example = "0")
    private Byte isDefault;

    /**
     * 创建时间
     */
    @TableField("created_date")
    @Schema(description = "创建时间", example = "2025-08-11 10:30:00")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    @Schema(description = "更新时间", example = "2025-08-11 10:30:00")
    private LocalDateTime updateDate;

    /**
     * 是否删除: 0-未删除, 1-已删除
     */
    @TableField("is_deleted")
    @TableLogic
    @Schema(description = "是否删除: 0-未删除, 1-已删除", example = "0")
    private Byte isDeleted;
}
