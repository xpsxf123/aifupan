package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 标签新增 / 修改入参。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 标签新增/修改入参")
public class SeoTagBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，新增时不传 */
    @Schema(description = "主键，新增时不传")
    private Long id;

    /** 标签名称，必填，1-20 字 */
    @Schema(description = "标签名称，必填")
    private String tagName;

    /** 拼音别名，留空时由名称生成 */
    @Schema(description = "拼音别名，留空时由名称生成")
    private String slug;

    /** 状态：1 启用 0 禁用 */
    @Schema(description = "状态：1 启用 0 禁用")
    private Integer tagStatus;
}
