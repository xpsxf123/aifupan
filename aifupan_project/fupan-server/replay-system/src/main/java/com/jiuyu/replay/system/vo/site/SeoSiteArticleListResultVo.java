package com.jiuyu.replay.system.vo.site;

import com.jiuyu.replay.generic.utils.PageUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 官网文章列表接口的出参，文章列表页 / 分类页 / 标签页共用。
 *
 * <p>把分类（或标签）信息与列表一起返回，是为了让分类页的 SSR <b>只打一次后端</b>：
 * 栏目页要用分类名渲染 h1、用 description 渲染 meta，拆成两个接口意味着每次
 * 服务端渲染都要串行请求两次，而这点信息小到可以忽略。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@Schema(description = "官网文章列表结果")
public class SeoSiteArticleListResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分页数据")
    private PageUtils<SeoSiteArticleListVo> page;

    /** 仅当按 categorySlug 查询时非空 */
    @Schema(description = "当前分类信息，非分类页时为 null")
    private SeoSiteCategoryVo category;

    /** 仅当按 tagSlug 查询时非空 */
    @Schema(description = "当前标签信息，非标签页时为 null")
    private SeoSiteTagVo tag;
}
