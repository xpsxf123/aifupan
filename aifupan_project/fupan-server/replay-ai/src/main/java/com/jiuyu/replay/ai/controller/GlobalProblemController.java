package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.GlobalProblemBll;
import com.jiuyu.replay.ai.bo.GlobalProblemBo;
import com.jiuyu.replay.ai.bo.GlobalProblemListBo;
import com.jiuyu.replay.ai.vo.GlobalProblemListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 全局提示词控制器
 *
 * @author lj
 * @date 2026-05-21
 */
@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping("replay/ai/globalProblem")
@Tag(name = "全局提示词")
public class GlobalProblemController {

    private final GlobalProblemBll globalProblemBll;

    @PostMapping("/list")
    @Operation(summary = "分页查询全局提示词列表")
    public R<PageUtils<GlobalProblemListVo>> list(
            @Parameter(description = "查询参数", required = true,
                    schema = @Schema(implementation = GlobalProblemListBo.class))
            @RequestBody GlobalProblemListBo listBo) {
        return R.ok("获取成功", globalProblemBll.queryPage(listBo));
    }

    @GetMapping("/info")
    @Operation(summary = "获取全局提示词详情")
    public R<GlobalProblemListVo> info(
            @Parameter(description = "提示词ID", required = true)
            @NotNull(message = "ID不能为空")
            @RequestParam Long id) {
        return R.ok("获取成功", globalProblemBll.info(id));
    }

    @PostMapping("/save")
    @Operation(summary = "新增全局提示词")
    public R<GlobalProblemListVo> save(
            @Parameter(description = "提示词对象", required = true,
                    schema = @Schema(implementation = GlobalProblemBo.class))
            @Validated(Insert.class)
            @RequestBody GlobalProblemBo bo) {
        return R.ok("添加成功", globalProblemBll.save(bo));
    }

    @PostMapping("/update")
    @Operation(summary = "修改全局提示词")
    public R<String> update(
            @Parameter(description = "提示词对象", required = true,
                    schema = @Schema(implementation = GlobalProblemBo.class))
            @Validated(Update.class)
            @RequestBody GlobalProblemBo bo) {
        globalProblemBll.update(bo);
        return R.ok("修改成功");
    }

    @GetMapping("/delete")
    @Operation(summary = "删除全局提示词")
    public R<String> delete(
            @Parameter(description = "提示词ID", required = true)
            @NotNull(message = "ID不能为空")
            @RequestParam Long id) {
        globalProblemBll.delete(id);
        return R.ok("删除成功");
    }
}
