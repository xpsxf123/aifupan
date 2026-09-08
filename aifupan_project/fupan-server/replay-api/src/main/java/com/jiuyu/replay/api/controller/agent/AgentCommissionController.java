package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.AgentCommissionBo;
import com.jiuyu.replay.agent.bo.AgentCommissionListBo;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.agent.vo.AgentCommissionListVo;
import com.jiuyu.replay.agent.vo.CommissionRecordsVo;
import com.jiuyu.replay.api.logic.agent.AgentCommissionLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@RestController
@CrossOrigin
@RequestMapping("replay/agentcommission")
@Tag(name = "代理商佣金")
public class AgentCommissionController {

    @Resource
    private AgentCommissionLogic agentCommissionLogic;

    /**
     * 代理商佣金列表
     * @param agentCommissionListBo 代理商佣金列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理商佣金列表")
    public R<PageUtils<AgentCommissionListVo>> list(@Parameter(description = "代理商佣金列表查询参数", required = true) @RequestBody AgentCommissionListBo agentCommissionListBo){

        return agentCommissionLogic.queryPage(agentCommissionListBo);
    }


    /**
     * 代理商佣金信息
     * @param id 代理商佣金id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理商佣金信息")
    public R<AgentCommissionInfoVo> info(@Parameter(description = "代理商佣金id", required = true) @RequestParam("id") Long id){

        return agentCommissionLogic.info(id);
    }

    /**
     * 新增代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理商佣金")
    public R<String> save(@Parameter(description = "代理商佣金对象", required = true) @RequestBody AgentCommissionBo agentCommissionBo){

        return agentCommissionLogic.save(agentCommissionBo);
    }

    /**
     * 修改代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理商佣金")
    public R<String> update(@Parameter(description = "代理商佣金对象", required = true) @RequestBody AgentCommissionBo agentCommissionBo){

        return agentCommissionLogic.update(agentCommissionBo);
    }

    /**
     * 删除代理商佣金
     * @param id 代理商佣金id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理商佣金")
    public R<String> delete(@Parameter(description = "代理商佣金id", required = true) @RequestParam("id") Long id){

        return agentCommissionLogic.delete(id);
    }

    @GetMapping("/commissionRecords")
    @Operation(summary = "佣金结算记录")
    public R<List<CommissionRecordsVo>> commissionRecords(Long agentId){
        return agentCommissionLogic.commissionRecords(agentId);
    }

}
