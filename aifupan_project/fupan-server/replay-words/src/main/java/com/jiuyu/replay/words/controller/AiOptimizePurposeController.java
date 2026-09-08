package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AiOptimizePurposeBll;
import com.jiuyu.replay.words.bo.AiOptimizePurposeSaveBo;
import com.jiuyu.replay.words.bo.AiOptimizePurposeUpdateBo;
import com.jiuyu.replay.generic.vo.words.AiOptimizePurposeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * AI优化目的Controller
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@RestController
@RequestMapping("replay/aiOptimizePurpose")
@Tag(name = "AI优化目的")
public class AiOptimizePurposeController {

    @Resource
    private AiOptimizePurposeBll aiOptimizePurposeBll;

    /**
     * 新增AI优化目的
     *
     * @param saveBo 保存参数
     * @return 新增的ID
     */
    @PostMapping("/save")
    @Operation(summary = "新增AI优化目的")
    public R<Long> save(@RequestBody AiOptimizePurposeSaveBo saveBo) {

        return aiOptimizePurposeBll.save(saveBo);
    }

    /**
     * 修改AI优化目的
     *
     * @param updateBo 更新参数
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "修改AI优化目的")
    public R<String> update(@RequestBody AiOptimizePurposeUpdateBo updateBo) {
        return aiOptimizePurposeBll.update(updateBo);
    }

    /**
     * 根据来源id查询AI优化目的信息
     *
     * @param sourceId   来源id
     * @return 优化目的信息
     */
    @GetMapping("/getBySourceId")
    @Operation(summary = "根据来源id查询AI优化目的信息")
    public R<AiOptimizePurposeVo> getBySourceId(@Parameter(description = "来源id", required = true) @RequestParam String sourceId) {
        return aiOptimizePurposeBll.getBySourceId(sourceId);
    }
}
