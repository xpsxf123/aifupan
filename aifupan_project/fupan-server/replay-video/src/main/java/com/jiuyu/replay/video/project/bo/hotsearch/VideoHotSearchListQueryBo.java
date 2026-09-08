package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.video.project.bo.VideoPageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/8/30 11:04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索列表查询业务对象")
public class VideoHotSearchListQueryBo extends VideoPageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "快照ID", example = "1238129381293")
    @NotNull(message = "快照ID不能为空")
    private Long snapshotId;

    @Schema(description = "点赞数条件大于某值", example = "10")
    private Long likeCount;

    @Schema(description = "评论数条件大于某值", example = "10")
    private Long commentCount;

    @Schema(description = "分享数条件大于某值", example = "10")
    private Long shareCount;

    @Schema(description = "收藏数条件大于某值", example = "10")
    private Long collectCount;

    @Schema(description = "发布时间开始值", example = "2025-08-11 10:30:00")
    private LocalDateTime publishStartTime;

    @Schema(description = "发布时间结束值", example = "2025-08-11 10:30:00")
    private LocalDateTime publishEndTime;

    @Schema(description = "时长开始值", example = "20")
    private Integer durationStartNumber;

    @Schema(description = "时长结束值", example = "60")
    private Integer durationEndNumber;

    @Schema(description = "排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序", example = "1")
    @EnumValue(byteValues = {1, 2, 3, 4, 5}, message = "排序类型标识不合法")
    private Byte sortCode = (byte) 1;

    @Schema(description = "排序顺序 0：降序 1：升序", example = "0")
    @EnumValue(byteValues = {0, 1}, message = "排序顺序不合法")
    private Byte sortSequence = (byte) 0;
}
