package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.CustPromptBll;
import com.jiuyu.replay.ai.bo.CustPromptBo;
import com.jiuyu.replay.ai.bo.CustPromptListBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.CustPromptVo;
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
 * 用户自定义提示词控制器
 *
 * @author jxy
 * @date 2025-01-21
 */
@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping("replay/ai/custPrompt")
@Tag(name = "用户自定义提示词")
public class CustPromptController {

    private final CustPromptBll custPromptBll;
    private final UserFeign userFeign;

    /**
     * 分页查询用户自定义提示词列表
     *
     * @param listBo 查询参数，包含分页信息和搜索关键词
     * @return 分页结果，包含提示词列表
     */
    @PostMapping("/privateList")
    @Operation(
            summary = "分页查询用户自定义提示词列表",
            description = "根据分页参数和搜索关键词查询当前用户的自定义提示词列表，支持按标题和内容模糊搜索"
    )
    public R<PageUtils<CustPromptVo>> list(
            @Parameter(
                    description = "查询参数，包含：\n" +
                            "- page(页码) [必填]\n" +
                            "- limit(每页数量) [必填]\n" +
                            "- keyword(搜索关键词) [可选]\n" +
                            "- promptTitle(提示词标题) [可选]\n" +
                            "- promptContent(提示词内容) [可选]",
                    required = true,
                    schema = @Schema(implementation = CustPromptListBo.class)
            )
            @RequestBody CustPromptListBo listBo) {
        listBo.setUserId(ResultUtil.getUserResult(userFeign.getLocalUser()).getId());
        return R.ok("获取成功", custPromptBll.queryPage(listBo));
    }

    /**
     * 获取用户自定义提示词详情
     *
     * @param id 提示词ID
     * @return 提示词详情信息
     */
    @GetMapping("/info")
    @Operation(
            summary = "获取用户自定义提示词详情",
            description = "根据提示词ID获取该提示词的详细信息，包括标题、内容、排序等"
    )
    public R<CustPromptVo> info(
            @Parameter(
                    description = "提示词ID [必填] 必须是有效的数字ID",
                    required = true,
                    example = "1234567890"
            )
            @RequestParam Long id) {
        return R.ok("获取成功", custPromptBll.info(id));
    }

    /**
     * 新增用户自定义提示词
     *
     * @param custPromptBo 提示词对象，包含标题、内容、排序等信息
     * @return 新增后的提示词详情
     */
    @PostMapping("/save")
    @Operation(
            summary = "新增用户自定义提示词",
            description = "创建一个新的用户自定义提示词，需要提供标题、内容和排序值。系统会自动关联当前登录用户"
    )
    public R<CustPromptVo> save(
            @Parameter(
                    description = "提示词对象，包含：\n" +
                            "- promptTitle(标题) [必填] 最多10个字符\n" +
                            "- promptContent(内容) [必填] 最多300个字符\n" +
                            "- promptSort(排序) [必填] 0-99之间的整数",
                    required = true,
                    schema = @Schema(implementation = CustPromptBo.class)
            )
            @Validated(Insert.class)
            @RequestBody CustPromptBo custPromptBo) {
        return R.ok("添加成功", custPromptBll.save(custPromptBo));
    }

    /**
     * 修改用户自定义提示词
     *
     * @param custPromptBo 提示词对象，必须包含ID和要修改的字段
     * @return 修改成功消息
     */
    @PostMapping("/update")
    @Operation(
            summary = "修改用户自定义提示词",
            description = "修改已存在的用户自定义提示词，需要提供提示词ID和要修改的字段。只有提示词所有者可以修改"
    )
    public R<String> update(
            @Parameter(
                    description = "提示词对象，包含：\n" +
                            "- id(提示词ID) [必填]\n" +
                            "- promptTitle(标题) [必填] 最多10个字符\n" +
                            "- promptContent(内容) [必填] 最多300个字符\n" +
                            "- promptSort(排序) [必填] 0-99之间的整数",
                    required = true,
                    schema = @Schema(implementation = CustPromptBo.class)
            )
            @Validated(Update.class)
            @RequestBody CustPromptBo custPromptBo) {
        custPromptBll.update(custPromptBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户自定义提示词
     *
     * @param id 提示词ID
     * @return 删除成功消息
     */
    @GetMapping("/delete")
    @Operation(
            summary = "删除用户自定义提示词",
            description = "删除指定的用户自定义提示词。删除操作为软删除，数据不会物理删除。只有提示词所有者可以删除"
    )
    public R<String> delete(
            @Parameter(
                    description = "提示词ID [必填] 必须是有效的数字ID，不能为空",
                    required = true,
                    example = "1234567890"
            )
            @NotNull(message = "提示词ID不能为空")
            @RequestParam Long id) {
        custPromptBll.delete(id);
        return R.ok("删除成功");
    }
}

