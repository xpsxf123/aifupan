package com.jiuyu.replay.video.project.vo.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分组视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 分组显示数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分组视图对象")
public class GroupVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID", example = "1001")
    private Long groupId;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "默认分组")
    private String groupName;

    /**
     * 分组类型: 1-达人订阅, 2-爆款订阅
     */
    @Schema(description = "分组类型: 1-达人订阅, 2-爆款订阅", example = "1")
    private Byte groupType;

    /**
     * 分组类型名称
     */
    @Schema(description = "分组类型名称", example = "达人分组")
    private String groupTypeName;

    /**
     * 分组描述
     */
    @Schema(description = "分组描述", example = "这是默认分组")
    private String groupDescription;

    /**
     * 成员数量
     */
    @Schema(description = "成员数量", example = "10")
    private Integer memberCount;

    /**
     * 视频总数
     */
    @Schema(description = "视频总数", example = "10")
    private Integer videoCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-08-14 10:30:00")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "张三")
    private String creator;


    @Schema(description = "用户ID")
    private Long userId;
}
