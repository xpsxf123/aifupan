package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AiAnalysisSensitiveRelaLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaListBo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@RestController
@CrossOrigin
@RequestMapping("words/aianalysissensitiverela")
@Tag(name = "AI分析关键词与记录关联关系表")
public class AiAnalysisSensitiveRelaController {

    @Resource
    private AiAnalysisSensitiveRelaLogic aiAnalysisSensitiveRelaLogic;

    /**
     * AI分析关键词与记录关联关系表列表
     * @param aiAnalysisSensitiveRelaListBo AI分析关键词与记录关联关系表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "AI分析关键词与记录关联关系表列表")
    public R<PageUtils<AiAnalysisSensitiveRelaListVo>> list(@Parameter(description = "AI分析关键词与记录关联关系表列表查询参数", required = true) @RequestBody AiAnalysisSensitiveRelaListBo aiAnalysisSensitiveRelaListBo){

        return aiAnalysisSensitiveRelaLogic.queryPage(aiAnalysisSensitiveRelaListBo);
    }


    /**
     * AI分析关键词与记录关联关系表信息
     * @param id AI分析关键词与记录关联关系表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "AI分析关键词与记录关联关系表信息")
    public R<AiAnalysisSensitiveRelaInfoVo> info(@Parameter(description = "AI分析关键词与记录关联关系表id", required = true) @RequestParam("id") Long id){

        return aiAnalysisSensitiveRelaLogic.info(id);
    }

    /**
     * 新增AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增AI分析关键词与记录关联关系表")
    public R<String> save(@Parameter(description = "AI分析关键词与记录关联关系表对象", required = true) @RequestBody AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo){

        return aiAnalysisSensitiveRelaLogic.save(aiAnalysisSensitiveRelaBo);
    }

    /**
     * 修改AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改AI分析关键词与记录关联关系表")
    public R<String> update(@Parameter(description = "AI分析关键词与记录关联关系表对象", required = true) @RequestBody AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo){

        return aiAnalysisSensitiveRelaLogic.update(aiAnalysisSensitiveRelaBo);
    }

    /**
     * 删除AI分析关键词与记录关联关系表
     * @param id AI分析关键词与记录关联关系表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除AI分析关键词与记录关联关系表")
    public R<String> delete(@Parameter(description = "AI分析关键词与记录关联关系表id", required = true) @RequestParam("id") Long id){

        return aiAnalysisSensitiveRelaLogic.delete(id);
    }

}
