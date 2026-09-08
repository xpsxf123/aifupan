package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * slug 建议与查重结果。
 *
 * <p>字段名为前端强依赖契约，不可更名。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "slug 建议与查重结果")
public class SeoSlugSuggestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 建议值：入参 slug 为空时为由 name 生成的拼音，否则为规范化后的入参 */
    @Schema(description = "建议的 slug")
    private String slug;

    /** 是否可用（未被他人占用） */
    @Schema(description = "是否可用")
    private Boolean available;

    /** 占用者名称，available=false 时返回 */
    @Schema(description = "占用者名称")
    private String ownerName;

    /** 语义化备选，可为空数组 */
    @Schema(description = "语义化备选")
    private List<String> suggestions;
}
