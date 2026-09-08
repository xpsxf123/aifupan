package com.jiuyu.replay.system.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 标签列表查询参数。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 标签列表查询参数")
public class SeoTagListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签名称，模糊匹配 */
    @Schema(description = "标签名称，模糊匹配")
    private String tagName;

    /** 状态筛选：1 启用 0 禁用，不传为全部 */
    @Schema(description = "状态筛选：1 启用 0 禁用，不传为全部")
    private Integer tagStatus;

    /** 排序方式：count 关联文章数倒序（默认）/ new 创建时间倒序。本期不支持按浏览量排序 */
    @Schema(description = "排序方式：count 关联文章数倒序（默认）/ new 创建时间倒序")
    private String orderBy;
}
