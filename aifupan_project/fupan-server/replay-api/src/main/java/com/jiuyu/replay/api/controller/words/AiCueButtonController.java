package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AiCueButtonLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiCueButtonBo;
import com.jiuyu.replay.words.bo.AiCueButtonListBo;
import com.jiuyu.replay.words.vo.AiCueButtonInfoVo;
import com.jiuyu.replay.words.vo.AiCueButtonListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 固定提示按钮
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@RestController
@CrossOrigin
@RequestMapping("replay/aicuebutton")
@Tag(name = "固定提示按钮")
public class AiCueButtonController {

    @Resource
    private AiCueButtonLogic aiCueButtonLogic;

    /**
     * 固定提示按钮列表
     * @param aiCueButtonListBo 固定提示按钮列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "固定提示按钮列表")
    public R<PageUtils<AiCueButtonListVo>> list(@Parameter(description = "固定提示按钮列表查询参数", required = true) @RequestBody AiCueButtonListBo aiCueButtonListBo) {
        return aiCueButtonLogic.queryPage(aiCueButtonListBo);
    }


    /**
     * 固定提示按钮信息
     * @param id 固定提示按钮id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "固定提示按钮信息")
    public R<AiCueButtonInfoVo> info(@Parameter(description = "固定提示按钮id", required = true) @RequestParam("id") Long id) {
        return aiCueButtonLogic.info(id);
    }

    /**
     * 新增固定提示按钮
     * @param aiCueButtonBo 固定提示按钮对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增固定提示按钮")
    public R<String> save(@Parameter(description = "固定提示按钮对象", required = true) @RequestBody AiCueButtonBo aiCueButtonBo) {
        return aiCueButtonLogic.save(aiCueButtonBo);
    }

    /**
     * 修改固定提示按钮
     * @param aiCueButtonBo 固定提示按钮对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改固定提示按钮")
    public R<String> update(@Parameter(description = "固定提示按钮对象", required = true) @RequestBody AiCueButtonBo aiCueButtonBo) {

        return aiCueButtonLogic.update(aiCueButtonBo);
    }

    /**
     * 删除固定提示按钮
     * @param id 固定提示按钮id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除固定提示按钮")
    public R<String> delete(@Parameter(description = "固定提示按钮id", required = true) @RequestParam("id") Long id) {

        return aiCueButtonLogic.delete(id);
    }

}
