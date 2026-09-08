package com.jiuyu.replay.video.project.vo.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订阅分组视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 订阅分组的返回数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订阅分组视图对象")
public class SubscriptionGroupVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID", example = "1")
    private Long id;

    /**
     * 分组类型: 1-达人订阅, 2-爆款订阅
     */
    @Schema(description = "分组类型: 1-达人订阅, 2-爆款订阅", example = "1")
    private Byte groupType;

    /**
     * 分组类型名称
     */
    @Schema(description = "分组类型名称", example = "达人订阅")
    private String groupTypeName;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "美食达人")
    private String groupName;

    /**
     * 分组描述
     */
    @Schema(description = "分组描述", example = "专注美食内容的达人分组")
    private String description;

    /**
     * 是否默认分组: 0-否, 1-是
     */
    @Schema(description = "是否默认分组: 0-否, 1-是", example = "0")
    private Byte isDefault;

    /**
     * 排序
     */
    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    /**
     * 订阅数量
     */
    @Schema(description = "订阅数量", example = "10")
    private Integer subscriptionCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-08-14 10:30:00")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2025-08-14 10:30:00")
    private LocalDateTime updateDate;
}
