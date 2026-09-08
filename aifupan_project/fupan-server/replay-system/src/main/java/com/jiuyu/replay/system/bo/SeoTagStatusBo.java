package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 标签启用 / 禁用入参，支持批量。
 *
 * <p>{@code id} 与 {@code ids} 都会被读取并合并——前端列表里的行内开关传单个更顺手，
 * 批量操作传数组，两种都支持。见 {@link SeoArticleStatusBo} 的说明了解为何与文章拆开。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "标签启用/禁用入参")
public class SeoTagStatusBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单个标签 ID，与 ids 二者任传其一即可，都传则合并 */
    @Schema(description = "单个标签ID")
    private Long id;

    /** 标签 ID 集合，与 id 二者任传其一即可，都传则合并 */
    @Schema(description = "标签ID集合")
    private List<Long> ids;

    /** 目标状态：1 启用 0 禁用 */
    @Schema(description = "目标状态：1 启用 0 禁用")
    private Integer tagStatus;
}
