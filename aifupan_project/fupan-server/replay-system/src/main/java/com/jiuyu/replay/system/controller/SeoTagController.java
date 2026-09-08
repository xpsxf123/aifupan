package com.jiuyu.replay.system.controller;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.SeoIdsBo;
import com.jiuyu.replay.system.bo.SeoTagStatusBo;
import com.jiuyu.replay.system.bo.SeoTagBo;
import com.jiuyu.replay.system.bo.SeoTagListBo;
import com.jiuyu.replay.system.repository.service.SeoTagService;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import com.jiuyu.replay.system.vo.SeoTagListVo;
import com.jiuyu.replay.system.vo.SeoTagOptionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SEO 标签管理。
 *
 * @author claude
 * @date 2026-08-12
 */
@RestController
@CrossOrigin
@RequestMapping("replay/seoTag")
@Tag(name = "SEO 内容管理-标签")
public class SeoTagController {

    private final SeoTagService seoTagService;

    public SeoTagController(SeoTagService seoTagService) {
        this.seoTagService = seoTagService;
    }

    /**
     * 标签列表，含关联文章数。本期不含关联文章浏览量。
     *
     * @param bo 查询参数
     * @return 分页结果
     */
    @PostMapping("/list")
    @Operation(summary = "标签列表")
    public R<PageUtils<SeoTagListVo>> list(
            @Parameter(description = "查询参数", required = true) @RequestBody SeoTagListBo bo) {
        return R.ok("获取成功", seoTagService.queryPage(bo));
    }

    /**
     * 启用中的标签，供文章表单下拉使用。
     *
     * @return 标签选项列表
     */
    @GetMapping("/enabled")
    @Operation(summary = "启用中的标签（下拉用）")
    public R<List<SeoTagOptionVo>> enabled() {
        return R.ok("获取成功", seoTagService.listEnabled());
    }

    /**
     * 新增标签。
     *
     * @param bo 标签对象
     * @return 含实际入库 slug 的结果
     */
    /**
     * 标签详情，供编辑表单回填。出参含关联文章数。
     *
     * @param id 标签 id
     * @return 详情
     */
    @GetMapping("/info")
    @Operation(summary = "标签详情")
    public R<SeoTagListVo> info(
            @Parameter(description = "标签id", required = true) @RequestParam("id") Long id) {
        return R.ok("获取成功", seoTagService.info(id));
    }

    @PostMapping("/save")
    @Operation(summary = "新增标签")
    public R<SeoSaveResultVo> save(@Parameter(description = "标签对象", required = true) @RequestBody SeoTagBo bo) {
        return R.ok("添加成功", seoTagService.saveTag(bo));
    }

    /**
     * 修改标签。
     *
     * @param bo 标签对象
     * @return 含实际入库 slug 的结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改标签")
    public R<SeoSaveResultVo> update(@Parameter(description = "标签对象", required = true) @RequestBody SeoTagBo bo) {
        return R.ok("修改成功", seoTagService.updateTag(bo));
    }

    /**
     * 删除标签。有关联文章也允许删除，删除时清理关联，文章本身不受影响。
     *
     * @param id 标签 id
     * @return 删除结果
     */
    @GetMapping("/delete")
    @Operation(summary = "删除标签")
    public R<String> delete(@Parameter(description = "标签id", required = true) @RequestParam("id") Long id) {
        seoTagService.deleteTags(List.of(id));
        return R.ok("删除成功");
    }

    /**
     * 批量删除标签。
     *
     * @param bo 主键集合
     * @return 删除结果
     */
    @PostMapping("/deleteByIds")
    @Operation(summary = "批量删除标签")
    public R<String> deleteByIds(@Parameter(description = "主键集合", required = true) @RequestBody SeoIdsBo bo) {
        seoTagService.deleteTags(bo.getIds());
        return R.ok("删除成功");
    }

    /**
     * 变更标签启用状态，支持单条与批量。
     *
     * @param bo 状态变更参数
     * @return 变更结果
     */
    @PostMapping("/changeStatus")
    @Operation(summary = "启用/禁用标签")
    public R<String> changeStatus(
            @Parameter(description = "状态变更参数", required = true) @RequestBody SeoTagStatusBo bo) {
        List<Long> ids = new ArrayList<>();
        if (Objects.nonNull(bo.getId())) {
            ids.add(bo.getId());
        }
        if (bo.getIds() != null) {
            ids.addAll(bo.getIds());
        }
        seoTagService.changeStatus(ids, bo.getTagStatus());
        return R.ok("操作成功");
    }
}
