package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * slug 建议与查重入参。
 *
 * <p>拼音生成与查重合并为一个接口：导入功能本就要在后端生成 slug，
 * 前端若再引拼音库会形成两套实现，分词差异会导致「预览一个值、存成另一个值」。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "slug 建议与查重入参")
public class SeoSlugSuggestBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对象类型：article / category / tag */
    @Schema(description = "对象类型：article / category / tag")
    private String type;

    /** 名称或标题，用于生成拼音 */
    @Schema(description = "名称或标题，用于生成拼音")
    /**
     * 待生成拼音的名称。
     *
     * <p><b>这个字段刻意仍叫 name</b>：它不对应任何表列，是三种类型（文章标题 / 分类名 / 标签名）
     * 共用的通用入参，叫 categoryName 或 tagName 都不准确。表列避开 MySQL 关键字与它无关。
     */
    private String name;

    /** 运营已手填的 slug；为空则由 name 生成 */
    @Schema(description = "运营已手填的 slug；为空则由 name 生成")
    private String slug;

    /** 编辑时传自身 ID，查重排除自己 */
    @Schema(description = "编辑时传自身 ID，查重排除自己")
    private Long excludeId;
}
