package com.jiuyu.replay.video.project.bo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 爆款搜索查询业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款搜索查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索查询业务对象")
public class VideoHotSearchQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "20")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页最大条数不能超过100条")
    private Integer limit = 20;

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词", example = "比熊")
    @Length(max = 100, message = "搜索关键词长度不能超过100个字符")
    private String keyword;
}
