package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AiAnalysisRecordLogic;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.AiAnalysisRecordListVo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordListBo;



/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@RestController
@CrossOrigin
@RequestMapping("words/aianalysisrecord")
@Tag(name = "AI分析出来的关键词总数和未匹配上词库的关键词个数")
public class AiAnalysisRecordController {

    @Resource
    private AiAnalysisRecordLogic aiAnalysisRecordLogic;

    /**
     * AI分析出来的关键词总数和未匹配上词库的关键词个数列表
     * @param aiAnalysisRecordListBo AI分析出来的关键词总数和未匹配上词库的关键词个数列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "AI分析出来的关键词总数和未匹配上词库的关键词个数列表")
    public R<PageUtils<AiAnalysisRecordListVo>> list(@Parameter(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数列表查询参数", required = true) @RequestBody AiAnalysisRecordListBo aiAnalysisRecordListBo){

        return aiAnalysisRecordLogic.queryPage(aiAnalysisRecordListBo);
    }


    /**
     * AI分析出来的关键词总数和未匹配上词库的关键词个数信息
     * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "AI分析出来的关键词总数和未匹配上词库的关键词个数信息")
    public R<AiAnalysisRecordInfoVo> info(@Parameter(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数id", required = true) @RequestParam("id") Long id){

        return aiAnalysisRecordLogic.info(id);
    }

    /**
     * 新增AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增AI分析出来的关键词总数和未匹配上词库的关键词个数")
    public R<String> save(@Parameter(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数对象", required = true) @RequestBody AiAnalysisRecordBo aiAnalysisRecordBo){

        return aiAnalysisRecordLogic.save(aiAnalysisRecordBo);
    }

    /**
     * 修改AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改AI分析出来的关键词总数和未匹配上词库的关键词个数")
    public R<String> update(@Parameter(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数对象", required = true) @RequestBody AiAnalysisRecordBo aiAnalysisRecordBo){

        return aiAnalysisRecordLogic.update(aiAnalysisRecordBo);
    }

    /**
     * 删除AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除AI分析出来的关键词总数和未匹配上词库的关键词个数")
    public R<String> delete(@Parameter(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数id", required = true) @RequestParam("id") Long id){

        return aiAnalysisRecordLogic.delete(id);
    }

}
