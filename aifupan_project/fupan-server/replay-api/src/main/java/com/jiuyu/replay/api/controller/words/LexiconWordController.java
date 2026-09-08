package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.LexiconWordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.LexiconWordBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.vo.LexiconWordInfoVo;
import com.jiuyu.replay.words.vo.LexiconWordListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 词库-词语关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@RestController
@CrossOrigin
@RequestMapping("words/lexiconword")
@Tag(name = "词库-词语关联")
public class LexiconWordController {

    @Resource
    private LexiconWordLogic lexiconWordLogic;

    /**
     * 词库-词语关联列表
     * @param lexiconWordListBo 词库-词语关联列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "词库-词语关联列表")
    public R<PageUtils<LexiconWordListVo>> list(@Parameter(description = "词库-词语关联列表查询参数", required = true) @RequestBody LexiconWordListBo lexiconWordListBo){

        return lexiconWordLogic.queryPage(lexiconWordListBo);
    }


    /**
     * 词库-词语关联信息
     * @param id 词库-词语关联id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "词库-词语关联信息")
    public R<LexiconWordInfoVo> info(@Parameter(description = "词库-词语关联id", required = true) @RequestParam("id") Long id){

        return lexiconWordLogic.info(id);
    }

    /**
     * 新增词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增词库-词语关联")
    public R<String> save(@Parameter(description = "词库-词语关联对象", required = true) @RequestBody LexiconWordBo lexiconWordBo){

        return lexiconWordLogic.save(lexiconWordBo);
    }

    /**
     * 修改词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改词库-词语关联")
    public R<String> update(@Parameter(description = "词库-词语关联对象", required = true) @RequestBody LexiconWordBo lexiconWordBo){

        return lexiconWordLogic.update(lexiconWordBo);
    }

    /**
     * 删除词库-词语关联
     * @param id 词库-词语关联id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除词库-词语关联")
    public R<String> delete(@Parameter(description = "词库-词语关联id", required = true) @RequestParam("id") Long id){

        return lexiconWordLogic.delete(id);
    }

}
