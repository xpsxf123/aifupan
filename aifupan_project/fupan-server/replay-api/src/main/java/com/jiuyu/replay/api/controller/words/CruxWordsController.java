package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.CruxWordsLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.CruxWordsBatchBo;
import com.jiuyu.replay.words.bo.CruxWordsBo;
import com.jiuyu.replay.words.bo.CruxWordsListBo;
import com.jiuyu.replay.words.vo.CruxWordsInfoVo;
import com.jiuyu.replay.words.vo.CruxWordsListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@RestController
@CrossOrigin
@RequestMapping("replay/cruxwords")
@Tag(name = "关键词")
public class CruxWordsController {

    @Resource
    private CruxWordsLogic cruxWordsLogic;

    /**
     * 关键词列表
     * @param cruxWordsListBo 关键词列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "关键词列表")
    public R<PageUtils<CruxWordsListVo>> list(@Parameter(description = "关键词列表查询参数", required = true) @RequestBody CruxWordsListBo cruxWordsListBo){

        return cruxWordsLogic.queryPage(cruxWordsListBo);
    }


    /**
     * 关键词信息
     * @param id 关键词id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "关键词信息")
    public R<CruxWordsInfoVo> info(@Parameter(description = "关键词id", required = true) @RequestParam("id") Long id){


        return cruxWordsLogic.info(id);
    }

    /**
     * 新增关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增关键词")
    public R<String> save(@Parameter(description = "关键词对象", required = true) @RequestBody CruxWordsBo cruxWordsBo){

        return cruxWordsLogic.save(cruxWordsBo);
    }

    /**
     * 批量新增关键词
     * @param cruxWordsBatchBo 关键词对象
     * @return
     */
    @PostMapping("/saveBatch")
    @Operation(summary = "批量新增关键词")
    public R<String> saveBatch(@Parameter(description = "关键词对象", required = true) @RequestBody CruxWordsBatchBo cruxWordsBatchBo){

        return cruxWordsLogic.saveBatch(cruxWordsBatchBo);
    }

    /**
     * 修改关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改关键词")
    public R<String> update(@Parameter(description = "关键词对象", required = true) @RequestBody CruxWordsBo cruxWordsBo){

        return cruxWordsLogic.update(cruxWordsBo);
    }

    /**
     * 删除关键词
     * @param id 关键词id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除关键词")
    public R<String> delete(@Parameter(description = "关键词id", required = true) @RequestParam("id") Long id){

        return cruxWordsLogic.delete(id);
    }

}
