package com.jiuyu.replay.video.project.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 视频文案提取批量删除请求对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 用于接收前端提交的批量删除请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频文案提取批量删除请求对象")
public class VideoExtractBatchDeleteBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "要删除的记录ID列表", example = "[1, 2, 3]", required = true)
    @NotEmpty(message = "删除记录ID列表不能为空")
    private List<@NotNull(message = "记录ID不能为空") Long> ids;
}
