package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AiAnalysisLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisListBo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;



/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
@RestController
@CrossOrigin
@RequestMapping("replay/aianalysis")
@Tag(name = "AI分析")
public class AiAnalysisController {

    @Resource
    private AiAnalysisLogic aiAnalysisLogic;

    /**
     * AI分析内容
     * @param analysisBo Ai分析类
     * @return
     */
        @PostMapping("/aiAnalysis")
    @Operation(summary = "AI分析内容")
    public R<List<String>> aiAnalysis(@Parameter(description = "AI分析表对象", required = true) @RequestBody AiAnalysisBo analysisBo){
        return aiAnalysisLogic.aiAnalysis(analysisBo);
    }

    /**
     * 根据唯一标识查询所有AI分析记录
     * @param uuid 唯一标识
     * @return
     */
    @GetMapping("/listAiAnalysisByUuid")
    @Operation(summary = "根据唯一标识查询所有AI分析记录")
    public R<List<List<String>>> listAiAnalysisByUuid(@Parameter(description = "AI分析表id", required = true) @RequestParam("uuid") String uuid){
        return aiAnalysisLogic.listAiAnalysisByUuid(uuid);
    }

    /**
     * 根据唯一标识去判断是否有正在进行AI分析的文本;返回true代表正在分析,false则没有
     * @param uuid 唯一标识
     * @return
     */
    @GetMapping("/analysisStatusByUuid")
    @Operation(summary = "根据唯一标识去判断是否有正在进行AI分析的文本")
    public R<String> analysisStatusByUuid(@Parameter(description = "唯一标识",required = true) @RequestParam("uuid") String uuid,
                                          @Parameter(description = "模型id") @RequestParam(value = "modelId", required = false) String modelId){

        return aiAnalysisLogic.analysisStatusByUuid(uuid, modelId);
    }

    /**
     * 从redis获取所有指定前缀的sessionId,用于判断是否有正在分析的文件
     * @return
     */
    @GetMapping("/getAllSessionId")
    @Operation(summary = "从redis获取所有指定前缀的sessionId,用于判断是否有正在分析的文件")
    public R<Set<String>> getAllSessionId(){
        return aiAnalysisLogic.getAllSessionId();
    }

    /**
     * AI分析表列表
     * @param aiAnalysisListBo AI分析表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "AI分析表列表")
    public R<PageUtils<AiAnalysisListVo>> list(@Parameter(description = "AI分析表列表查询参数", required = true) @RequestBody AiAnalysisListBo aiAnalysisListBo){

        return aiAnalysisLogic.queryPage(aiAnalysisListBo);
    }

    /**
     * AI分析表信息
     * @param id AI分析表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "AI分析表信息")
    public R<AiAnalysisInfoVo> info(@Parameter(description = "AI分析表id", required = true) @RequestParam("id") Long id){

        return aiAnalysisLogic.info(id);
    }

    /**
     * 新增AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增AI分析表")
    public R<String> save(@Parameter(description = "AI分析表对象", required = true) @RequestBody AiAnalysisBo aiAnalysisBo){

        return aiAnalysisLogic.save(aiAnalysisBo);
    }

    /**
     * 修改AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改AI分析表")
    public R<String> update(@Parameter(description = "AI分析表对象", required = true) @RequestBody AiAnalysisBo aiAnalysisBo){

        return aiAnalysisLogic.update(aiAnalysisBo);
    }

    /**
     * 删除AI分析表
     * @param id AI分析表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除AI分析表")
    public R<String> delete(@Parameter(description = "AI分析表id", required = true) @RequestParam("id") Long id){
        return aiAnalysisLogic.delete(id);
    }

}
