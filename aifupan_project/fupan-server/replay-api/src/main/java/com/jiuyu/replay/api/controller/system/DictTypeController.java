package com.jiuyu.replay.api.controller.system;

import com.jiuyu.replay.api.logic.system.DictTypeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.DictTypeBo;
import com.jiuyu.replay.system.bo.DictTypeListBo;
import com.jiuyu.replay.system.vo.DictTypeInfoVo;
import com.jiuyu.replay.system.vo.DictTypeListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 字典类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@RestController
@CrossOrigin
@RequestMapping("replay/dicttype")
@Tag(name = "字典类型")
public class DictTypeController {

    @Resource
    private DictTypeLogic dictTypeLogic;

    /**
     * 字典类型列表
     * @param dictTypeListBo 字典类型列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "字典类型列表")
    public R<PageUtils<DictTypeListVo>> list(@Parameter(description = "字典类型列表查询参数", required = true) @RequestBody DictTypeListBo dictTypeListBo){

        return dictTypeLogic.queryPage(dictTypeListBo);
    }


    /**
     * 字典类型信息
     * @param id 字典类型id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "字典类型信息")
    public R<DictTypeInfoVo> info(@Parameter(description = "字典类型id", required = true) @RequestParam("id") Long id){

        return dictTypeLogic.info(id);
    }

    /**
     * 新增字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增字典类型")
    public R<String> save(@Parameter(description = "字典类型对象", required = true) @RequestBody DictTypeBo dictTypeBo){

        return dictTypeLogic.save(dictTypeBo);
    }

    /**
     * 修改字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改字典类型")
    public R<String> update(@Parameter(description = "字典类型对象", required = true) @RequestBody DictTypeBo dictTypeBo){

        return dictTypeLogic.update(dictTypeBo);
    }

    /**
     * 删除字典类型
     * @param id 字典类型id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除字典类型")
    public R<String> delete(@Parameter(description = "字典类型id", required = true) @RequestParam("id") Long id){

        return dictTypeLogic.delete(id);
    }

}
