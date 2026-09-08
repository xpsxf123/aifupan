package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.CruxTypeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.CruxTypeInfoVo;
import com.jiuyu.replay.generic.vo.words.CruxTypeVo;
import com.jiuyu.replay.words.bo.CruxTypeBo;
import com.jiuyu.replay.words.bo.CruxTypeListBo;
import com.jiuyu.replay.words.vo.CruxTypeListVo;
import com.jiuyu.replay.words.vo.CruxTypeTreeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 关键词类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@RestController
@CrossOrigin
@RequestMapping("replay/cruxtype")
@Tag(name = "关键词类型")
public class CruxTypeController {

    @Resource
    private CruxTypeLogic cruxTypeLogic;

    /**
     * 关键词类型列表（树形结构）
     * @param childrenNotNull 当没有子关键词类型时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    @GetMapping("/listTree")
    @Operation(summary = "关键词类型列表（树形结构）")
    public R<List<CruxTypeTreeVo>> listTree(@RequestParam(required = false) Integer childrenNotNull) {

        return cruxTypeLogic.listTree(childrenNotNull);
    }


    /**
     * 获取在数据罗盘展示的关键词类型列表
     * @return
     */
    @GetMapping("/getShowCompassList")
    @Operation(summary = "获取在数据罗盘展示的关键词类型列表")
    public R<List<CruxTypeVo>> getShowCompassList() {

        return this.cruxTypeLogic.getShowCompassList();
    }

    /**
     * 关键词类型列表
     * @param cruxTypeListBo 关键词类型列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "关键词类型列表")
    public R<PageUtils<CruxTypeListVo>> list(@Parameter(description = "关键词类型列表查询参数", required = true) @RequestBody CruxTypeListBo cruxTypeListBo){

        return cruxTypeLogic.queryPage(cruxTypeListBo);
    }


    /**
     * 关键词类型信息
     * @param id 关键词类型id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "关键词类型信息")
    public R<CruxTypeInfoVo> info(@Parameter(description = "关键词类型id", required = true) @RequestParam("id") Long id){

        return cruxTypeLogic.info(id);
    }

    /**
     * 新增关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增关键词类型")
    public R<String> save(@Parameter(description = "关键词类型对象", required = true) @RequestBody CruxTypeBo cruxTypeBo){

        return cruxTypeLogic.save(cruxTypeBo);
    }

    /**
     * 修改关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改关键词类型")
    public R<String> update(@Parameter(description = "关键词类型对象", required = true) @RequestBody CruxTypeBo cruxTypeBo){

        return cruxTypeLogic.update(cruxTypeBo);
    }

    /**
     * 删除关键词类型
     * @param id 关键词类型id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除关键词类型")
    public R<String> delete(@Parameter(description = "关键词类型id", required = true) @RequestParam("id") Long id){

        return cruxTypeLogic.delete(id);
    }

}
