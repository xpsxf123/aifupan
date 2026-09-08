package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.third.AiModelLogic;
import com.jiuyu.replay.api.logic.words.DiagnosisModelLogic;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelListVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@RestController
@CrossOrigin
@RequestMapping("words/diagnosismodel")
@Tag(name = "ai诊断中的模型设置-主播和视频")
@AllArgsConstructor
public class DiagnosisModelController {

    private final DiagnosisModelLogic diagnosisModelLogic;
    private final AiModelLogic aiModelLogic;


    /**
     * ai诊断中的模型设置-主播和视频列表
     * @param diagnosisModelListBo ai诊断中的模型设置-主播和视频列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "ai诊断中的模型设置-主播和视频列表")
    public R<PageUtils<DiagnosisModelListVo>> list(@Parameter(description = "ai诊断中的模型设置-主播和视频列表查询参数", required = true) @RequestBody DiagnosisModelListBo diagnosisModelListBo){

        return diagnosisModelLogic.queryPage(diagnosisModelListBo);
    }

    /**
     * ai诊断中的模型设置-主播和视频信息
     * @param id ai诊断中的模型设置-主播和视频id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "ai诊断中的模型设置-主播和视频信息")
    public R<DiagnosisModelInfoVo> info(@Parameter(description = "ai诊断中的模型设置-主播和视频id", required = true) @RequestParam("id") Long id){

        return diagnosisModelLogic.info(id);
    }

    /**
     * 新增ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增ai诊断中的模型设置-主播和视频")
    public R<String> save(@Parameter(description = "ai诊断中的模型设置-主播和视频对象", required = true) @RequestBody DiagnosisModelBo diagnosisModelBo){

        return diagnosisModelLogic.save(diagnosisModelBo);
    }

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改ai诊断中的模型设置-主播和视频")
    public R<String> update(@Parameter(description = "ai诊断中的模型设置-主播和视频对象", required = true) @RequestBody DiagnosisModelBo diagnosisModelBo){

        return diagnosisModelLogic.update(diagnosisModelBo);
    }

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "添加和修改诊断模型设置")
    @UserLock
    public R<String> saveOrUpdate(@Parameter(description = "ai诊断中的模型设置-主播和视频对象", required = true) @RequestBody DiagnosisModelBo diagnosisModelBo){
        return diagnosisModelLogic.saveOrUpdate(diagnosisModelBo);
    }

    /**
     * 删除ai诊断中的模型设置-主播和视频
     * @param id ai诊断中的模型设置-主播和视频id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除ai诊断中的模型设置-主播和视频")
    public R<String> delete(@Parameter(description = "ai诊断中的模型设置-主播和视频id", required = true) @RequestParam("id") Long id){

        return diagnosisModelLogic.delete(id);
    }

}
