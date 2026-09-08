package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AddAnchorKeywordsBo;
import com.jiuyu.replay.words.bo.RemoveAnchorKeywordsBo;
import com.jiuyu.replay.words.bll.AnchorCruxWordsBll;
import com.jiuyu.replay.words.vo.AnchorCruxWordsVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主播关键词Controller
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@RestController
@RequestMapping("replay/anchorCruxWords")
@Tag(name = "主播关键词管理")
public class AnchorCruxWordsController {

    @Resource
    private AnchorCruxWordsBll anchorCruxWordsBll;

    /**
     * 根据主播secUid查询关键词列表
     *
     * @param secUid 主播secUid
     * @return 关键词列表
     */
    @GetMapping("/listBySecUid")
    @Operation(summary = "根据主播secUid查询关键词列表")
    public R<List<AnchorCruxWordsVo>> listBySecUid(
            @Parameter(description = "主播secUid") @RequestParam String secUid) {

        return anchorCruxWordsBll.listBySecUid(secUid);
    }

    /**
     * 添加主播关键词
     *
     * @param bo 添加主播关键词请求参数
     * @return 添加结果
     */
    @PostMapping("/addKeywords")
    @Operation(summary = "添加主播关键词")
    public R<String> addKeywords(
            @Parameter(description = "添加主播关键词请求参数", required = true)
            @RequestBody @Validated AddAnchorKeywordsBo bo) {
        return anchorCruxWordsBll.addKeywords(bo);
    }

    /**
     * 移除主播关键词
     *
     * @param bo 移除主播关键词请求参数
     * @return 移除结果
     */
    @PostMapping("/removeKeywords")
    @Operation(summary = "移除主播关键词")
    public R<String> removeKeywords(
            @Parameter(description = "移除主播关键词请求参数", required = true)
            @RequestBody @Validated RemoveAnchorKeywordsBo bo) {
        return anchorCruxWordsBll.removeKeywords(bo);
    }

}
