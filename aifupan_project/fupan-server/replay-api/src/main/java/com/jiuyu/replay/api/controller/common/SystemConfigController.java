package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.logic.common.SystemConfigLogic;
import com.jiuyu.replay.common.bo.SystemConfigBo;
import com.jiuyu.replay.common.bo.SystemConfigListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.vo.SystemConfigListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;




/**
 * 系统配置
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
@RestController
@CrossOrigin
@RequestMapping("replay/systemconfig")
@Tag(name = "系统配置")
public class SystemConfigController {

    @Resource
    private SystemConfigLogic systemConfigLogic;

    /**
     * 系统配置列表
     * @param systemConfigListBo 系统配置列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "系统配置列表")
    public R<PageUtils<SystemConfigListVo>> list(@Parameter(description = "系统配置列表查询参数", required = true) @RequestBody SystemConfigListBo systemConfigListBo){

        return systemConfigLogic.queryPage(systemConfigListBo);
    }


    /**
     * 系统配置信息
     * @param id 系统配置id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "系统配置信息")
    public R<SystemConfigInfoVo> info(@Parameter(description = "系统配置id", required = true) @RequestParam("id") Long id){

        return systemConfigLogic.info(id);
    }

    /**
     * 新增系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增系统配置")
    public R<String> save(@Parameter(description = "系统配置对象", required = true) @RequestBody SystemConfigBo systemConfigBo){

        return systemConfigLogic.save(systemConfigBo);
    }

    /**
     * 修改系统配置
     * @param systemConfigBo 系统配置对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改系统配置")
    public R<String> update(@Parameter(description = "系统配置对象", required = true) @RequestBody SystemConfigBo systemConfigBo){

        return systemConfigLogic.update(systemConfigBo);
    }

    /**
     * 删除系统配置
     * @param id 系统配置id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除系统配置")
    public R<String> delete(@Parameter(description = "系统配置id", required = true) @RequestParam("id") Long id){

        return systemConfigLogic.delete(id);
    }

}
