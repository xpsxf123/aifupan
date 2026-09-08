package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.WordRuleLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.WordRuleBo;
import com.jiuyu.replay.words.bo.WordRuleListBo;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import com.jiuyu.replay.words.vo.WordRuleListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@RestController
@CrossOrigin
@RequestMapping("words/wordrule")
@Tag(name = "词语匹配规则")
public class WordRuleController {

    @Resource
    private WordRuleLogic wordRuleLogic;

    /**
     * 词语匹配规则列表
     * @param wordRuleListBo 词语匹配规则列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "词语匹配规则列表")
    public R<PageUtils<WordRuleListVo>> list(@Parameter(description = "词语匹配规则列表查询参数", required = true) @RequestBody WordRuleListBo wordRuleListBo){

        return wordRuleLogic.queryPage(wordRuleListBo);
    }


    /**
     * 词语匹配规则信息
     * @param id 词语匹配规则id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "词语匹配规则信息")
    public R<WordRuleInfoVo> info(@Parameter(description = "词语匹配规则id", required = true) @RequestParam("id") Long id){

        return wordRuleLogic.info(id);
    }

    /**
     * 新增词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增词语匹配规则")
    public R<String> save(@Parameter(description = "词语匹配规则对象", required = true) @RequestBody WordRuleBo wordRuleBo){

        return wordRuleLogic.save(wordRuleBo);
    }

    /**
     * 修改词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改词语匹配规则")
    public R<String> update(@Parameter(description = "词语匹配规则对象", required = true) @RequestBody WordRuleBo wordRuleBo){

        return wordRuleLogic.update(wordRuleBo);
    }

    /**
     * 删除词语匹配规则
     * @param id 词语匹配规则id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除词语匹配规则")
    public R<String> delete(@Parameter(description = "词语匹配规则id", required = true) @RequestParam("id") Long id){

        return wordRuleLogic.delete(id);
    }

}
