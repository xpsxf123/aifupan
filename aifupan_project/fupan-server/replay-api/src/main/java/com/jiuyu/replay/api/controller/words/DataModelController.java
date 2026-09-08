package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.DataModelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.DataModelBo;
import com.jiuyu.replay.words.bo.DataModelListBo;
import com.jiuyu.replay.words.bo.DataModelSaveBo;
import com.jiuyu.replay.words.vo.DataModelInfoVo;
import com.jiuyu.replay.words.vo.DataModelListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 罗盘数据模型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@RestController
@CrossOrigin
@RequestMapping("replay/datamodel")
@Tag(name = "罗盘数据模型")
public class DataModelController {

    @Resource
    private DataModelLogic dataModelLogic;

    /**
     * 罗盘数据模型列表
     * @param dataModelListBo 罗盘数据模型列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "罗盘数据模型列表")
    public R<PageUtils<DataModelListVo>> list(@Parameter(description = "罗盘数据模型列表查询参数", required = true) @RequestBody DataModelListBo dataModelListBo){

        return dataModelLogic.queryPage(dataModelListBo);
    }


    /**
     * 罗盘数据模型信息
     * @param id 罗盘数据模型id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "罗盘数据模型信息")
    public R<DataModelInfoVo> info(@Parameter(description = "罗盘数据模型id", required = true) @RequestParam("id") Long id){

        return dataModelLogic.info(id);
    }

    /**
     * 新增罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增罗盘数据模型")
    public R<String> save(@Parameter(description = "罗盘数据模型对象", required = true) @RequestBody DataModelBo dataModelBo){

        return dataModelLogic.save(dataModelBo);

    }

    /**
     * 修改罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改罗盘数据模型")
    public R<String> update(@Parameter(description = "罗盘数据模型对象", required = true) @RequestBody DataModelBo dataModelBo){

        return dataModelLogic.update(dataModelBo);
    }

    /**
     * 删除罗盘数据模型
     * @param id 罗盘数据模型id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除罗盘数据模型")
    public R<String> delete(@Parameter(description = "罗盘数据模型id", required = true) @RequestParam("id") Long id){

        return dataModelLogic.delete(id);
    }


    /**
     * 新增通用模型及其关键词类型
     * @param dataModelBo
     * @return
     */
    @PostMapping("/saveDataModel")
    @Operation(summary = "新增通用模型及其关键词类型")
    public R<String> saveDataModel(@RequestBody DataModelSaveBo dataModelBo){

        return dataModelLogic.saveDataModel(dataModelBo);
    }

    /**
     * 查询模型和其下关键词类型
     * @param dataModelBo
     * @return
     */
    @PostMapping("/modelCruxTypeList")
    @Operation(summary = "查询模型和其下关键词类型")
    public R<PageUtils<DataModelSaveBo>> modelCruxTypeList(@RequestBody DataModelListBo dataModelBo){
        return dataModelLogic.modelCruxTypeList(dataModelBo);
    }

    /**
     * 修改通用模型及其关键词类型
     * @param dataModelBo
     * @return
     */
    @PostMapping("/updateDataModel")
    @Operation(summary = "修改通用模型及其关键词类型")
    public R<String> updateDataModel(@RequestBody DataModelSaveBo dataModelBo){

        return dataModelLogic.updateDataModel(dataModelBo);
    }

    /**
     * 删除通用模型及其关键词类型
     * @param id
     * @return
     */
    @GetMapping("/deleteDataModel")
    @Operation(summary = "删除通用模型及其关键词类型")
    public R<String> deleteDataModel(@Parameter(description = "罗盘数据模型id", required = true) @RequestParam("id") Long id){
        return dataModelLogic.deleteDataModel(id);
    }

    /**
     * 根据ID获取模型及其关键词类型
     * @param id
     * @return
     */
    @GetMapping("/infoDataModel")
    @Operation(summary = "根据ID获取模型及其关键词类型")
    public R<DataModelSaveBo> infoDataModel(@Parameter(description = "罗盘数据模型id", required = true) @RequestParam("id") Long id){
        return dataModelLogic.infoDataModel(id);
    }

}
