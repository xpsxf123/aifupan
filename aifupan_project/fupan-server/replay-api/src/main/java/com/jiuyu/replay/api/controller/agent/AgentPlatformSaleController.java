package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.AgentPlatformSaleBo;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleListBo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleListVo;
import com.jiuyu.replay.api.logic.agent.AgentPlatformSaleLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 代理商平台销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@RestController
@CrossOrigin
@RequestMapping("replay/agentplatformsale")
@Tag(name = "代理商平台销售")
public class AgentPlatformSaleController {

    @Resource
    private AgentPlatformSaleLogic agentPlatformSaleLogic;

    /**
     * 获取代理商的平台销售列表
     * @param agentId 代理商id
     * @return
     */
    @GetMapping("/listByAgentId")
    @Operation(summary = "获取代理商的平台销售列表")
    public R<List<AgentPlatformSaleInfoVo>> listByAgentId(
            @Parameter(description = "代理商id", required = true) @RequestParam("agentId") Long agentId,
            @Parameter(description = "销售类型 0平台销售，1代理商销售") @RequestParam(value = "salesType", required = false) Integer salesType
    ) {
        if (salesType == null) salesType = 0;
        return agentPlatformSaleLogic.listByAgentId(agentId, salesType);
    }

    /**
     * 代理商平台销售列表
     * @param agentPlatformSaleListBo 代理商平台销售列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理商平台销售列表")
    public R<PageUtils<AgentPlatformSaleListVo>> list(@Parameter(description = "代理商平台销售列表查询参数", required = true) @RequestBody AgentPlatformSaleListBo agentPlatformSaleListBo){

        return agentPlatformSaleLogic.queryPage(agentPlatformSaleListBo);
    }


    /**
     * 代理商平台销售信息
     * @param id 代理商平台销售id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理商平台销售信息")
    public R<AgentPlatformSaleInfoVo> info(@Parameter(description = "代理商平台销售id", required = true) @RequestParam("id") Long id){

        return agentPlatformSaleLogic.info(id);
    }

    /**
     * 新增代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理商平台销售")
    public R<String> save(@Parameter(description = "代理商平台销售对象", required = true) @RequestBody AgentPlatformSaleBo agentPlatformSaleBo){

        return agentPlatformSaleLogic.save(agentPlatformSaleBo);
    }

    /**
     * 修改代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理商平台销售")
    public R<String> update(@Parameter(description = "代理商平台销售对象", required = true) @RequestBody AgentPlatformSaleBo agentPlatformSaleBo){

        return agentPlatformSaleLogic.update(agentPlatformSaleBo);
    }

    /**
     * 删除代理商平台销售
     * @param id 代理商平台销售id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理商平台销售")
    public R<String> delete(@Parameter(description = "代理商平台销售id", required = true) @RequestParam("id") Long id){

        return agentPlatformSaleLogic.delete(id);
    }

}
