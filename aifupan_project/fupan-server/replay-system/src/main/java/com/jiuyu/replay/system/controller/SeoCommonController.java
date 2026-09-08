package com.jiuyu.replay.system.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.SeoSlugSuggestBo;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.vo.SeoSlugSuggestVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SEO 内容管理公共接口。
 *
 * @author claude
 * @date 2026-08-12
 */
@RestController
@CrossOrigin
@RequestMapping("replay/seoCommon")
@Tag(name = "SEO 内容管理-公共")
public class SeoCommonController {

    private final SeoSlugService seoSlugService;

    public SeoCommonController(SeoSlugService seoSlugService) {
        this.seoSlugService = seoSlugService;
    }

    /**
     * slug 建议与查重。
     *
     * <p>拼音生成与查重合并为一个接口，前端 300ms 防抖调用。
     *
     * @param bo 查询参数
     * @return 建议值 + 是否可用 + 占用者 + 语义备选
     */
    @PostMapping("/slugSuggest")
    @Operation(summary = "slug 建议与查重")
    public R<SeoSlugSuggestVo> slugSuggest(
            @Parameter(description = "查询参数", required = true) @RequestBody SeoSlugSuggestBo bo) {
        return R.ok("获取成功", seoSlugService.suggest(bo));
    }
}
