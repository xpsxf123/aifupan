package com.jiuyu.replay.video.project.vo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 视频评赞比视图对象
 *
 * @author RayChou
 * @date 2025/10/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频评赞比视图对象")
public class VideoCommentLikeRatioVo {

    @Schema(description = "日期", example = "2025-08-22")
    private LocalDate dataDate;

    @Schema(description = "评赞比（评论数/点赞数，保留两位小数）", example = "0.15")
    private String ratio;
}

