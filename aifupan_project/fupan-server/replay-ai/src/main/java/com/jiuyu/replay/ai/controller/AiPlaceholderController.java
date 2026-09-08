package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.AiPlaceholderBll;
import com.jiuyu.replay.ai.bo.AiPlaceholderBo;
import com.jiuyu.replay.ai.bo.AiPlaceholderListBo;
import com.jiuyu.replay.ai.vo.AiPlaceholderVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI占位符配置控制器
 *
 * @author jy
 * @date 2026-06-16
 */
@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping("replay/ai/placeholder")
@Tag(name = "AI占位符配置")
public class AiPlaceholderController {

    private final AiPlaceholderBll aiPlaceholderBll;

    /**
     * 分页查询占位符列表
     *
     * @param listBo 查询参数，包含分页信息和搜索关键词
     * @return 分页结果
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询占位符列表")
    public R<PageUtils<AiPlaceholderVo>> list(
            @Parameter(description = "查询参数，keyword 可选")
            @RequestBody AiPlaceholderListBo listBo) {
        return R.ok(aiPlaceholderBll.queryPage(listBo));
    }

    /**
     * 获取占位符详情
     *
     * @param id 占位符ID
     * @return 占位符详情
     */
    @GetMapping("/info")
    @Operation(summary = "获取占位符详情")
    public R<AiPlaceholderVo> info(
            @Parameter(description = "占位符ID", required = true)
            @NotNull(message = "ID不能为空")
            @RequestParam Long id) {
        return R.ok(aiPlaceholderBll.info(id));
    }

    /**
     * 新增占位符
     *
     * @param bo 占位符对象
     * @return 新增后的占位符
     */
    @PostMapping("/save")
    @Operation(summary = "新增占位符")
    public R<AiPlaceholderVo> save(
            @Parameter(description = "占位符对象")
            @Validated
            @RequestBody AiPlaceholderBo bo) {
        return R.ok(aiPlaceholderBll.save(bo));
    }

    /**
     * 修改占位符
     *
     * @param bo 占位符对象，必须包含ID
     * @return 修改成功消息
     */
    @PostMapping("/update")
    @Operation(summary = "修改占位符")
    public R<String> update(
            @Parameter(description = "占位符对象，必须包含ID")
            @Validated
            @RequestBody AiPlaceholderBo bo) {
        aiPlaceholderBll.update(bo);
        return R.ok("修改成功");
    }

    /**
     * 删除占位符
     *
     * @param id 占位符ID
     * @return 删除成功消息
     */
    @GetMapping("/delete")
    @Operation(summary = "删除占位符")
    public R<String> delete(
            @Parameter(description = "占位符ID", required = true)
            @NotNull(message = "ID不能为空")
            @RequestParam Long id) {
        aiPlaceholderBll.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 查询所有启用的占位符（前端展示用）
     *
     * @return 占位符列表
     */
    @GetMapping("/listAll")
    @Operation(summary = "查询所有启用的占位符")
    public R<List<AiPlaceholderVo>> listAll() {
        return R.ok(aiPlaceholderBll.listAll());
    }

    /**
     * 客户端获取占位符配置列表
     * <p>返回所有前端展示、未删除、已启用的占位符，按 sort 升序排列。
     * 前端使用时取 {@code placeholderKey} 字段作为 value（如 {@code #{trade}}）。</p>
     *
     * @return 占位符配置列表
     */
    @GetMapping("/listForClient")
    @Operation(summary = "客户端获取占位符配置列表")
    public R<List<AiPlaceholderVo>> listForClient() {
        return R.ok(aiPlaceholderBll.listForClient());
    }
}
