package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.LexiconLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.LexiconBo;
import com.jiuyu.replay.words.bo.LexiconListBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.vo.LexiconInfoVo;
import com.jiuyu.replay.words.vo.LexiconListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 词库
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@RestController
@CrossOrigin
@RequestMapping("replay/lexicon")
@Tag(name = "词库")
public class LexiconController {

    @Resource
    private LexiconLogic lexiconLogic;

    /**
     * 获取词库的词语列表
     * @param lexiconWordListBo 列表查询参数
     * @return
     */
    @PostMapping("/getWordsList")
    @Operation(summary = "获取词库的词语列表")
    public R<PageUtils<SensitiveWordsClientListVo>> getWordsList(@Parameter(description = "列表查询参数", required = true) @RequestBody LexiconWordListBo lexiconWordListBo){

        return lexiconLogic.getWordsList(lexiconWordListBo);
    }

    /**
     * 词库列表
     * @param lexiconListBo 词库列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "词库列表")
    public R<PageUtils<LexiconListVo>> list(@Parameter(description = "词库列表查询参数", required = true) @RequestBody LexiconListBo lexiconListBo){

        return lexiconLogic.queryPage(lexiconListBo);
    }


    /**
     * 词库信息
     * @param id 词库id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "词库信息")
    public R<LexiconInfoVo> info(@Parameter(description = "词库id", required = true) @RequestParam("id") Long id){

        return lexiconLogic.info(id);
    }

    /**
     * 新增词库
     * @param lexiconBo 词库对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增词库")
    public R<String> save(@Parameter(description = "词库对象", required = true) @RequestBody LexiconBo lexiconBo){

        return lexiconLogic.save(lexiconBo);
    }

    /**
     * 修改词库
     * @param lexiconBo 词库对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改词库")
    public R<String> update(@Parameter(description = "词库对象", required = true) @RequestBody LexiconBo lexiconBo){

        return lexiconLogic.update(lexiconBo);
    }

    /**
     * 删除词库
     * @param id 词库id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除词库")
    public R<String> delete(@Parameter(description = "词库id", required = true) @RequestParam("id") Long id){

        return lexiconLogic.delete(id);
    }

}
