package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 云空间视频回收站实体。
 *
 * <p>对应表 {@code tb_anchor_video_recycle}。</p>
 *
 * <p>字段 {@code vedioSizie} 保留历史拼写，通过 {@code @TableField("video_size")} 映射到
 * DDL 列 {@code video_size}，与 {@link AnchorVideoEntity#vedioSizie} 命名保持一致。</p>
 */
@Data
@TableName("tb_anchor_video_recycle")
@Schema(description = "云空间视频回收站")
public class AnchorVideoRecycleEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，雪花 ID。
     */
    @Schema(description = "主键，雪花ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 视频唯一标识（UUID）。
     */
    @Schema(description = "视频唯一标识（UUID）")
    private String videoId;

    /**
     * 视频名称。
     */
    @Schema(description = "视频名称")
    private String videoName;

    /**
     * 在线播放地址。
     */
    @Schema(description = "在线播放地址")
    private String playUrl;

    /**
     * 腾讯云 VOD fileId（从 play_url 解析；playUrl 为空则存空串）。
     */
    @Schema(description = "腾讯云 VOD fileId")
    private String fileId;

    /**
     * 视频大小，单位：B。
     * 字段名保留历史拼写，映射到 DDL 列 video_size。
     */
    @Schema(description = "视频大小，单位：B")
    @TableField("video_size")
    private Long videoSize;

    /**
     * 执行删除操作的用户 ID。
     */
    @Schema(description = "执行删除操作的用户ID")
    private Long userId;

    /**
     * 执行删除操作的用户昵称。
     */
    @Schema(description = "执行删除操作的用户昵称")
    private String userName;

    /**
     * 租户 ID。
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 删除时间。
     */
    @Schema(description = "删除时间")
    private LocalDateTime deleteTime;

    /**
     * 恢复状态（预留）：0 未恢复 1 已恢复。
     */
    @Schema(description = "恢复状态（预留）：0 未恢复 1 已恢复")
    private Integer restoreStatus = 0;

    /**
     * VOD 兜底删除状态（预留）：0 未删除 1 已删除。
     */
    @Schema(description = "VOD兜底删除状态（预留）：0 未删除 1 已删除")
    private Integer vodDeleted = 0;

    /**
     * 软删除标记：0 正常 1 已删除。
     */
    @Schema(description = "软删除标记：0 正常 1 已删除")
    private Integer isDeleted = 0;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    private LocalDateTime createDate;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateDate;
}
