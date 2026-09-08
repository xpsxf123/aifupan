package com.jiuyu.replay.api.controller.system;

import com.jiuyu.replay.api.logic.system.DictDataLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.DictDataBo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.system.vo.DictDataInfoVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@RestController
@CrossOrigin
@RequestMapping("replay/dictdata")
@Tag(name = "字典")
public class DictDataController {

    @Resource
    private DictDataLogic dictDataLogic;

    /**
     * 字典列表
     * @param dictDataListBo 字典列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "字典列表")
    public R<PageUtils<DictDataListVo>> list(@Parameter(description = "字典列表查询参数", required = true) @RequestBody DictDataListBo dictDataListBo){

        return dictDataLogic.queryPage(dictDataListBo);
    }


    /**
     * 字典信息
     * @param id 字典id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "字典信息")
    public R<DictDataInfoVo> info(@Parameter(description = "字典id", required = true) @RequestParam("id") Long id){

        return dictDataLogic.info(id);
    }

    /**
     * 新增字典
     * @param dictDataBo 字典对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增字典")
    public R<String> save(@Parameter(description = "字典对象", required = true) @RequestBody DictDataBo dictDataBo){

        return dictDataLogic.save(dictDataBo);
    }

    /**
     * 修改字典
     * @param dictDataBo 字典对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改字典")
    public R<String> update(@Parameter(description = "字典对象", required = true) @RequestBody DictDataBo dictDataBo){

        return dictDataLogic.update(dictDataBo);
    }

    /**
     * 删除字典
     * @param id 字典id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除字典")
    public R<String> delete(@Parameter(description = "字典id", required = true) @RequestParam("id") Long id){

        return dictDataLogic.delete(id);
    }

}
