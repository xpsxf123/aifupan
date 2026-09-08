package com.jiuyu.replay.video.project.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/8/14 17:47
 */
@Data
@Schema(description = "短视频模块分页对象")
public class VideoPageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页
     */
    @Schema(description = "当前页")
    @NotNull(message = "当前页不能为空")
    private Integer page = 1;

    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数")
    @NotNull(message = "每页查询记录数不能为空")
    @Max(value = 100, message = "每页最大条数不能超过100条")
    private Integer limit = 10;
}
