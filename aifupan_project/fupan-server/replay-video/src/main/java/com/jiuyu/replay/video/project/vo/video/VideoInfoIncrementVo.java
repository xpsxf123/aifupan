package com.jiuyu.replay.video.project.vo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author RayChou
 * @date 2025/8/22 15:07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频增长业务视图对象")
public class VideoInfoIncrementVo {

    @Schema(description = "日期", example = "2025-08-22")
    private LocalDate dataDate;

    @Schema(description = "数量", example = "100")
    private Long number;
}
