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
 * 达人搜索快照表
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
@TableName("tb_video_influencer_search_snapshot")
@Schema(description = "达人搜索快照表")
public class VideoInfluencerSearchSnapshotEntity implements Serializable {

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
     * 搜索关键词（抖音号/昵称）
     */
    @TableField("search_keyword")
    @Schema(description = "搜索关键词（抖音号/昵称）", example = "美食达人")
    private String searchKeyword;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @TableField("platform_type")
    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    private Byte platformType;

    /**
     * 搜索到的达人总数
     */
    @TableField("total_influencers")
    @Schema(description = "搜索到的达人总数", example = "50")
    private Integer totalInfluencers;

    /**
     * 搜索到的视频总数
     */
    @TableField("total_videos")
    @Schema(description = "搜索到的视频总数", example = "500")
    private Integer totalVideos;

    /**
     * 搜索时间
     */
    @TableField("search_time")
    @Schema(description = "搜索时间", example = "2025-08-11 10:30:00")
    private LocalDateTime searchTime;

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
