package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文章发布 / 下架入参。
 *
 * <p>与标签的 {@link SeoTagStatusBo} 分开而不共用一个 BO：此前共用时同时挂着 {@code id} 与
 * {@code ids}，而文章接口只读 {@code id}、标签接口两个都读——传错字段会被静默忽略，
 * 「传了参数却没生效」这种问题毫无排查线索。拆开后每个接口的入参形状就是它真正接受的形状。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "文章发布/下架入参")
public class SeoArticleStatusBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文章 ID。发布/下架是单条操作，PRD 未要求批量 */
    @Schema(description = "文章ID")
    private Long id;

    /** 目标状态：1 已发布 0 已下架 */
    @Schema(description = "目标状态：1 已发布 0 已下架")
    private Integer articleStatus;
}
