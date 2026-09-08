package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.ModelCruxLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.ModelCruxBo;
import com.jiuyu.replay.words.bo.ModelCruxListBo;
import com.jiuyu.replay.words.vo.ModelCruxInfoVo;
import com.jiuyu.replay.words.vo.ModelCruxListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 模型-关键词类型-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@RestController
@CrossOrigin
@RequestMapping("replay/modelcrux")
@Tag(name = "模型-关键词类型-关联表")
public class ModelCruxController {

    @Resource
    private ModelCruxLogic modelCruxLogic;

    /**
     * 模型-关键词类型-关联表列表
     * @param modelCruxListBo 模型-关键词类型-关联表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "模型-关键词类型-关联表列表")
    public R<PageUtils<ModelCruxListVo>> list(@Parameter(description = "模型-关键词类型-关联表列表查询参数", required = true) @RequestBody ModelCruxListBo modelCruxListBo){

        return modelCruxLogic.queryPage(modelCruxListBo);
    }


    /**
     * 模型-关键词类型-关联表信息
     * @param id 模型-关键词类型-关联表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "模型-关键词类型-关联表信息")
    public R<ModelCruxInfoVo> info(@Parameter(description = "模型-关键词类型-关联表id", required = true) @RequestParam("id") Long id){

        return modelCruxLogic.info(id);
    }

    /**
     * 新增模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增模型-关键词类型-关联表")
    public R<String> save(@Parameter(description = "模型-关键词类型-关联表对象", required = true) @RequestBody ModelCruxBo modelCruxBo){

        return modelCruxLogic.save(modelCruxBo);
    }

    /**
     * 修改模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改模型-关键词类型-关联表")
    public R<String> update(@Parameter(description = "模型-关键词类型-关联表对象", required = true) @RequestBody ModelCruxBo modelCruxBo){

        return modelCruxLogic.update(modelCruxBo);
    }

    /**
     * 删除模型-关键词类型-关联表
     * @param id 模型-关键词类型-关联表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除模型-关键词类型-关联表")
    public R<String> delete(@Parameter(description = "模型-关键词类型-关联表id", required = true) @RequestParam("id") Long id){

        return modelCruxLogic.delete(id);
    }

}
