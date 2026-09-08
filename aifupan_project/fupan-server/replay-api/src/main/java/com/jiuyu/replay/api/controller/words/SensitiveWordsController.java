package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.vo.SensitiveWordsInfoVo;
import com.jiuyu.replay.words.vo.SensitiveWordsListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@RestController
@CrossOrigin
@RequestMapping("replay/sensitivewords")
@Tag(name = "敏感词")
public class SensitiveWordsController {

    @Resource
    private SensitiveWordsLogic sensitiveWordsLogic;

    /**
     * 敏感词导入
     * @param excel excel文件
     * @return
     */
    @Operation(summary = "敏感词导入")
    @PostMapping("/importExcel")
    public R<List<String>> importExcel(@RequestParam("file") MultipartFile excel) {
        return sensitiveWordsLogic.importExcel(excel);
    }

    /**
     * 二次分析
     * @return
     */
    @Operation(summary = "二次分析")
    @PostMapping("/wordsMarkReAnalysis")
    public R<List<SentenceMarkVo>> wordsMarkReAnalysis(@Parameter(description = "二次分析对象", required = true) @RequestBody WordsMarkReAnalysisBo wordsMarkReAnalysisBo) {

        return sensitiveWordsLogic.wordsMarkReAnalysis(wordsMarkReAnalysisBo);
    }

    /**
     * 文字关键词/敏感词标识
     * @return
     */
    @Operation(summary = "文字关键词/敏感词标")
    @PostMapping("/wordsMark")
    public R<List<SentenceMarkVo>> wordsMark(@Parameter(description = "文字内容对象", required = true) @RequestBody List<WordsMarkBo> wordsMarkBoList) {

        return sensitiveWordsLogic.wordsMark(wordsMarkBoList);
    }

    /**
     * 文本内容关键词/敏感词标识
     * @return
     */
    @Operation(summary = "文本关键词/敏感词标识")
    @PostMapping("/wordsMarkByText")
    public R<SentenceMarkVo> wordsMarkByText(@Parameter(description = "文字内容对象", required = true) @RequestBody WordsMarkBo wordsMarkBo) {

        return sensitiveWordsLogic.wordsMarkByText(wordsMarkBo);
    }

    /**
     * 敏感词列表
     * @param sensitiveWordsListBo 敏感词列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "敏感词列表")
    public R<PageUtils<SensitiveWordsListVo>> list(@Parameter(description = "敏感词列表查询参数", required = true) @RequestBody SensitiveWordsListBo sensitiveWordsListBo){

        return sensitiveWordsLogic.queryPage(sensitiveWordsListBo);
    }


    /**
     * 敏感词信息
     * @param id 敏感词id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "敏感词信息")
    public R<SensitiveWordsInfoVo> info(@Parameter(description = "敏感词id", required = true) @RequestParam("id") Long id){

        return sensitiveWordsLogic.info(id);
    }

    /**
     * 新增敏感词
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增敏感词")
    public R<List<String>> save(@Parameter(description = "敏感词对象", required = true) @RequestBody SensitiveWordsBo sensitiveWordsBo){

        return sensitiveWordsLogic.save(sensitiveWordsBo);
    }

    /**
     * 批量新增敏感词
     * @param sensitiveWordsBatchBo 敏感词对象
     * @return
     */
    @PostMapping("/saveBatch")
    @Operation(summary = "批量新增敏感词")
    public R<List<String>> saveBatch(@Parameter(description = "敏感词对象", required = true) @RequestBody SensitiveWordsBatchBo sensitiveWordsBatchBo){

        return sensitiveWordsLogic.saveBatch(sensitiveWordsBatchBo);
    }

    /**
     * 修改敏感词
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改敏感词")
    public R<String> update(@Parameter(description = "敏感词对象", required = true) @RequestBody SensitiveWordsBo sensitiveWordsBo){

        return sensitiveWordsLogic.update(sensitiveWordsBo);
    }

    /**
     * 删除敏感词
     * @param id 敏感词id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除敏感词")
    public R<String> delete(@Parameter(description = "敏感词id", required = true) @RequestParam("id") Long id){

        return sensitiveWordsLogic.delete(id);
    }

}
