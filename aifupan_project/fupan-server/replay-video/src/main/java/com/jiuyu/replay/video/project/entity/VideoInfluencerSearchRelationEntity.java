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
 * 达人搜索快照-达人关联表
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
@TableName("tb_video_influencer_search_relation")
@Schema(description = "达人搜索快照-达人关联表")
public class VideoInfluencerSearchRelationEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    /**
     * 快照ID
     */
    @TableField("snapshot_id")
    @Schema(description = "快照ID", example = "1001")
    private Long snapshotId;

    /**
     * 达人ID
     */
    @TableField("influencer_id")
    @Schema(description = "达人ID", example = "2001")
    private Long influencerId;

    /**
     * 在搜索结果中的排序
     */
    @TableField("sort_order")
    @Schema(description = "在搜索结果中的排序", example = "1")
    private Integer sortOrder;

    /**
     * 该达人在此次搜索中的视频数量
     */
    @TableField("video_count")
    @Schema(description = "该达人在此次搜索中的视频数量", example = "10")
    private Integer videoCount;

    /**
     * 粉丝数
     */
    @TableField("followers_count")
    @Schema(description = "粉丝数", example = "100000")
    private Long followersCount;

    /**
     * 是否已处理: 0-未处理, 1-已处理
     */
    @TableField("is_processed")
    @Schema(description = "是否已处理: 0-未处理, 1-已处理", example = "0")
    private Byte isProcessed;

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
