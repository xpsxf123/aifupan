package com.jiuyu.replay.system.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * SEO 分类列表查询参数。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "SEO 分类列表查询参数")
public class SeoCategoryListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类名称，模糊匹配 */
    @Schema(description = "分类名称，模糊匹配")
    private String categoryName;
}
