package com.jiuyu.replay.api.controller.common;

import java.util.Arrays;
import java.util.Map;
import java.util.Date;

import com.jiuyu.replay.api.logic.common.OperationLogLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.jiuyu.replay.common.utils.SnowflakeManager;


import com.jiuyu.replay.common.vo.OperationLogListVo;
import com.jiuyu.replay.common.vo.OperationLogInfoVo;
import com.jiuyu.replay.common.bo.OperationLogBo;
import com.jiuyu.replay.common.bo.OperationLogListBo;



/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@RestController
@CrossOrigin
@RequestMapping("replay/common/operationlog")
@Tag(name = "操作日志表")
public class OperationLogController {

    @Resource
    private OperationLogLogic operationLogLogic;

    /**
     * 操作日志表列表
     * @param operationLogListBo 操作日志表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "操作日志表列表")
    public R<PageUtils<OperationLogListVo>> list(@Parameter(description = "操作日志表列表查询参数", required = true) @RequestBody OperationLogListBo operationLogListBo){

        return operationLogLogic.queryPage(operationLogListBo);
    }


    /**
     * 操作日志表信息
     * @param id 操作日志表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "操作日志表信息")
    public R<OperationLogInfoVo> info(@Parameter(description = "操作日志表id", required = true) @RequestParam("id") Long id){

        return operationLogLogic.info(id);
    }

    /**
     * 新增操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增操作日志表")
    public R<String> save(@Parameter(description = "操作日志表对象", required = true) @RequestBody OperationLogBo operationLogBo){

        return operationLogLogic.save(operationLogBo);
    }

    /**
     * 修改操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改操作日志表")
    public R<String> update(@Parameter(description = "操作日志表对象", required = true) @RequestBody OperationLogBo operationLogBo){

        return operationLogLogic.update(operationLogBo);
    }

    /**
     * 删除操作日志表
     * @param id 操作日志表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除操作日志表")
    public R<String> delete(@Parameter(description = "操作日志表id", required = true) @RequestParam("id") Long id){

        return operationLogLogic.delete(id);
    }

}
