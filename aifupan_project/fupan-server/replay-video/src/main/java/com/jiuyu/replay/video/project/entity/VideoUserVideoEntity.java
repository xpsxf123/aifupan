package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户-视频关联表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Getter
@Setter
@Accessors(chain = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_user_video")
@Schema(description = "用户-视频关联表")
public class VideoUserVideoEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 视频唯一标识
     */
    @TableField("video_id")
    @Schema(description = "视频唯一标识", example = "1001")
    private Long videoId;

    /**
     * 视频标题
     */
    @Schema(description = "视频标题", example = "美食制作教程")
    private String videoTitle;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 来源类型：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款
     */
    @TableField("source_type")
    @Schema(description = "来源类型：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款", example = "1")
    private Byte sourceType;

    /**
     * 来源id 短视频：用户id 搜达人：达人id 搜爆款：爆款搜索id
     */
    @TableField("source_id")
    @Schema(description = "来源id 短视频：用户id 搜达人：达人id 搜爆款：爆款搜索id", example = "2001")
    private Long sourceId;

    /**
     * 文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败
     */
    @TableField("extract_status")
    @Schema(description = "文案提取状态: 0-未提取 1-待处理, 2-处理中, 3-已完成, 4-失败", example = "1")
    private Byte extractStatus;

    /**
     * 文案提取时间
     */
    @Schema(description = "文案提取时间", example = "2025-08-11 10:30:00")
    private LocalDateTime extractTime;

    /**
     * 提取失败原因
     */
    @Schema(description = "提取失败原因", example = "时长超过10分钟")
    private String extractErrorReason;

    /**
     * AI分析状态:  0-未分析 1-待处理, 2-处理中, 3-已完成, 4-失败
     */
    @TableField("analysis_status")
    @Schema(description = "AI分析状态: 0-未分析 1-待处理, 2-处理中, 3-已完成, 4-失败", example = "1")
    private Byte analysisStatus;

    /**
     * AI分析时间
     */
    @Schema(description = "AI分析时间", example = "2025-08-11 10:30:00")
    private LocalDateTime analysisTime;

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
