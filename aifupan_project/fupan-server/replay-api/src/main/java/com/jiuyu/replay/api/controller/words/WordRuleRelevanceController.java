package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.WordRuleRelevanceLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.WordRuleRelevanceBo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceListBo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceInfoVo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 规则关联词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@RestController
@CrossOrigin
@RequestMapping("words/wordrulerelevance")
@Tag(name = "规则关联词")
public class WordRuleRelevanceController {

    @Resource
    private WordRuleRelevanceLogic wordRuleRelevanceLogic;

    /**
     * 规则关联词列表
     * @param wordRuleRelevanceListBo 规则关联词列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "规则关联词列表")
    public R<PageUtils<WordRuleRelevanceListVo>> list(@Parameter(description = "规则关联词列表查询参数", required = true) @RequestBody WordRuleRelevanceListBo wordRuleRelevanceListBo){

        return wordRuleRelevanceLogic.queryPage(wordRuleRelevanceListBo);
    }


    /**
     * 规则关联词信息
     * @param id 规则关联词id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "规则关联词信息")
    public R<WordRuleRelevanceInfoVo> info(@Parameter(description = "规则关联词id", required = true) @RequestParam("id") Long id){

        return wordRuleRelevanceLogic.info(id);
    }

    /**
     * 新增规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增规则关联词")
    public R<String> save(@Parameter(description = "规则关联词对象", required = true) @RequestBody WordRuleRelevanceBo wordRuleRelevanceBo){

        return wordRuleRelevanceLogic.save(wordRuleRelevanceBo);
    }

    /**
     * 修改规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改规则关联词")
    public R<String> update(@Parameter(description = "规则关联词对象", required = true) @RequestBody WordRuleRelevanceBo wordRuleRelevanceBo){

        return wordRuleRelevanceLogic.update(wordRuleRelevanceBo);
    }

    /**
     * 删除规则关联词
     * @param id 规则关联词id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除规则关联词")
    public R<String> delete(@Parameter(description = "规则关联词id", required = true) @RequestParam("id") Long id){

        return wordRuleRelevanceLogic.delete(id);
    }

}
