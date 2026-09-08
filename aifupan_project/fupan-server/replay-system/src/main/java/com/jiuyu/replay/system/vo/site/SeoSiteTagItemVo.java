package com.jiuyu.replay.system.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 官网侧文章上挂的标签项。
 *
 * <p>与后台的 {@link com.jiuyu.replay.system.vo.SeoArticleTagVo} 刻意分开：
 * 后台那个带 {@code id} 与 {@code tagStatus}——列表要把禁用标签置灰、编辑时也不能丢弃；
 * 官网只需要渲染文字并链到 /tag/{slug}，多返回的每一个字段都是白送给公网的内部信息。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网文章标签项")
public class SeoSiteTagItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签别名，官网 /tag/{slug}")
    private String slug;
}
