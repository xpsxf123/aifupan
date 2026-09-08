package com.jiuyu.replay.system.controller;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.SeoCategoryBo;
import com.jiuyu.replay.system.bo.SeoCategoryListBo;
import com.jiuyu.replay.system.repository.service.SeoCategoryService;
import com.jiuyu.replay.system.vo.SeoCategoryListVo;
import com.jiuyu.replay.system.vo.SeoCategoryOptionVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
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

import java.util.List;

/**
 * SEO 分类管理。
 *
 * @author claude
 * @date 2026-08-12
 */
@RestController
@CrossOrigin
@RequestMapping("replay/seoCategory")
@Tag(name = "SEO 内容管理-分类")
public class SeoCategoryController {

    private final SeoCategoryService seoCategoryService;

    public SeoCategoryController(SeoCategoryService seoCategoryService) {
        this.seoCategoryService = seoCategoryService;
    }

    /**
     * 分类列表，含关联文章数。
     *
     * @param bo 查询参数
     * @return 分页结果
     */
    @PostMapping("/list")
    @Operation(summary = "分类列表")
    public R<PageUtils<SeoCategoryListVo>> list(
            @Parameter(description = "查询参数", required = true) @RequestBody SeoCategoryListBo bo) {
        return R.ok("获取成功", seoCategoryService.queryPage(bo));
    }

    /**
     * 全部分类，供下拉使用。
     *
     * @return 分类选项列表
     */
    @GetMapping("/all")
    @Operation(summary = "全部分类（下拉用）")
    public R<List<SeoCategoryOptionVo>> all() {
        return R.ok("获取成功", seoCategoryService.listAll());
    }

    /**
     * 新增分类。
     *
     * @param bo 分类对象
     * @return 含实际入库 slug 的结果
     */
    /**
     * 分类详情，供编辑表单回填。出参含关联文章数。
     *
     * @param id 分类 id
     * @return 详情
     */
    @GetMapping("/info")
    @Operation(summary = "分类详情")
    public R<SeoCategoryListVo> info(
            @Parameter(description = "分类id", required = true) @RequestParam("id") Long id) {
        return R.ok("获取成功", seoCategoryService.info(id));
    }

    @PostMapping("/save")
    @Operation(summary = "新增分类")
    public R<SeoSaveResultVo> save(
            @Parameter(description = "分类对象", required = true) @RequestBody SeoCategoryBo bo) {
        return R.ok("添加成功", seoCategoryService.saveCategory(bo));
    }

    /**
     * 修改分类。
     *
     * @param bo 分类对象
     * @return 含实际入库 slug 的结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改分类")
    public R<SeoSaveResultVo> update(
            @Parameter(description = "分类对象", required = true) @RequestBody SeoCategoryBo bo) {
        return R.ok("修改成功", seoCategoryService.updateCategory(bo));
    }

    /**
     * 删除分类。分类下有文章时拒绝删除，msg 含文章数量。
     *
     * @param id 分类 id
     * @return 删除结果
     */
    @GetMapping("/delete")
    @Operation(summary = "删除分类")
    public R<String> delete(@Parameter(description = "分类id", required = true) @RequestParam("id") Long id) {
        seoCategoryService.deleteCategory(id);
        return R.ok("删除成功");
    }
}
