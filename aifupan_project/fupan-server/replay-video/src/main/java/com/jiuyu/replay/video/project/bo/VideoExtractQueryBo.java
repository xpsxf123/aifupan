package com.jiuyu.replay.video.project.bo;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/8/14 17:45
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短视频提取文案业务对象")
public class VideoExtractQueryBo extends VideoPageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 查询接口参数
    @Schema(description = "开始时间", example = "2025-08-11 00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2025-08-11 23:59:59")
    private LocalDateTime endTime;

    @Schema(description = "短视频名称", example = "精彩视频分享")
    @Length(max = 500, message = "短视频名称不合法")
    private String videoTitle;

    @Schema(description = "来源类型：1: 短视频URL 2:本地上传 3：搜达人  4：搜爆款", example = "1")
    @EnumValue(byteValues = {1, 2, 3, 4}, message = "类型不合法")
    private Byte sourceType;

    @Schema(description = "操作人用户ID", example = "1")
    private Long operatorUserId;
}
