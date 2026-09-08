package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 标签下拉选项。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 标签下拉选项")
public class SeoTagOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "拼音别名")
    private String slug;

    @Schema(description = "状态：1 启用 0 禁用")
    private Integer tagStatus;
}
