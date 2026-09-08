package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.DiagnosisCueLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueListBo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@RestController
@CrossOrigin
@RequestMapping("words/diagnosiscue")
@Tag(name = "ai诊断提示词配置")
public class DiagnosisCueController {

    @Resource
    private DiagnosisCueLogic diagnosisCueLogic;

    /**
     * ai诊断提示词配置列表
     * @param diagnosisCueListBo ai诊断提示词配置列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "ai诊断提示词配置列表")
    public R<PageUtils<DiagnosisCueListVo>> list(@Parameter(description = "ai诊断提示词配置列表查询参数", required = true) @RequestBody DiagnosisCueListBo diagnosisCueListBo){

        return diagnosisCueLogic.queryPage(diagnosisCueListBo);
    }

    /**
     * ai诊断提示词配置信息
     * @param id ai诊断提示词配置id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "ai诊断提示词配置信息")
    public R<DiagnosisCueInfoVo> info(@Parameter(description = "ai诊断提示词配置id", required = true) @RequestParam("id") Long id){

        return diagnosisCueLogic.info(id);
    }

    /**
     * 新增ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增ai诊断提示词配置")
    public R<String> save(@Parameter(description = "ai诊断提示词配置对象", required = true) @RequestBody DiagnosisCueBo diagnosisCueBo){

        return diagnosisCueLogic.save(diagnosisCueBo);
    }

    /**
     * 修改ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改ai诊断提示词配置")
    public R<String> update(@Parameter(description = "ai诊断提示词配置对象", required = true) @RequestBody DiagnosisCueBo diagnosisCueBo){

        return diagnosisCueLogic.update(diagnosisCueBo);
    }

    /**
     * 删除ai诊断提示词配置
     * @param id ai诊断提示词配置id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除ai诊断提示词配置")
    public R<String> delete(@Parameter(description = "ai诊断提示词配置id", required = true) @RequestParam("id") Long id){

        return diagnosisCueLogic.delete(id);
    }

}
