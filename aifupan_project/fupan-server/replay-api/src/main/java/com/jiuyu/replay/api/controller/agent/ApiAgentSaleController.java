package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.AgentSaleBo;
import com.jiuyu.replay.agent.bo.AgentSaleListBo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleListVo;
import com.jiuyu.replay.api.logic.agent.AgentSaleLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 代理商销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@RestController
@CrossOrigin
@RequestMapping("replay/agentsale")
@Tag(name = "代理商销售")
public class ApiAgentSaleController {

    @Resource
    private AgentSaleLogic agentSaleLogic;

    /**
     * 获取代理商的销售列表
     * @param agentId 代理商id
     * @return
     */
    @GetMapping("/listByAgentId")
    @Operation(summary = "获取代理商的销售列表")
    public R<List<AgentSaleInfoVo>> listByAgentId(@Parameter(description = "代理商id", required = true) @RequestParam("agentId") Long agentId){

        return agentSaleLogic.listByAgentId(agentId);
    }

    /**
     * 代理商销售列表
     * @param agentSaleListBo 代理商销售列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理商销售列表")
    public R<PageUtils<AgentSaleListVo>> list(@Parameter(description = "代理商销售列表查询参数", required = true) @RequestBody AgentSaleListBo agentSaleListBo){

        return agentSaleLogic.queryPage(agentSaleListBo);
    }


    /**
     * 代理商销售信息
     * @param id 代理商销售id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理商销售信息")
    public R<AgentSaleInfoVo> info(@Parameter(description = "代理商销售id", required = true) @RequestParam("id") Long id){

        return agentSaleLogic.info(id);
    }

    /**
     * 新增代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理商销售")
    public R<String> save(@Parameter(description = "代理商销售对象", required = true) @RequestBody AgentSaleBo agentSaleBo){

        return agentSaleLogic.save(agentSaleBo);
    }

    /**
     * 修改代理商销售
     * @param agentSaleBo 代理商销售对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理商销售")
    public R<String> update(@Parameter(description = "代理商销售对象", required = true) @RequestBody AgentSaleBo agentSaleBo){

        return agentSaleLogic.update(agentSaleBo);
    }

    /**
     * 删除代理商销售
     * @param id 代理商销售id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理商销售")
    public R<String> delete(@Parameter(description = "代理商销售id", required = true) @RequestParam("id") Long id){

        return agentSaleLogic.delete(id);
    }

}
