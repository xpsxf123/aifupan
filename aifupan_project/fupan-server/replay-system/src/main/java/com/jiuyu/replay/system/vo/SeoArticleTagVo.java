package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文章列表里的标签项。
 *
 * <p>必须带 status——列表需要把已禁用的标签置灰标注，运营才知道它在官网不展示。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "文章关联的标签")
public class SeoArticleTagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标签 ID")
    private Long id;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "状态：1 启用 0 禁用")
    private Integer tagStatus;
}
