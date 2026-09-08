package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.AgentPromotionBo;
import com.jiuyu.replay.agent.bo.AgentPromotionListBo;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.vo.AgentPromotionListVo;
import com.jiuyu.replay.api.logic.agent.AgentPromotionLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 代理商推广渠道
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@RestController
@CrossOrigin
@RequestMapping("replay/agentpromotion")
@Tag(name = "代理商推广渠道")
public class AgentPromotionController {

    @Resource
    private AgentPromotionLogic agentPromotionLogic;

    /**
     * 获取代理商的推广渠道列表
     * @param agentId 代理商id
     * @return
     */
    @GetMapping("/listByAgentId")
    @Operation(summary = "获取代理商的推广渠道列表")
    public R<List<AgentPromotionInfoVo>> listByAgentId(@Parameter(description = "代理商id", required = true) @RequestParam("agentId") Long agentId){

        return agentPromotionLogic.listByAgentId(agentId);
    }

    /**
     * 代理商推广渠道列表
     * @param agentPromotionListBo 代理商推广渠道列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理商推广渠道列表")
    public R<PageUtils<AgentPromotionListVo>> list(@Parameter(description = "代理商推广渠道列表查询参数", required = true) @RequestBody AgentPromotionListBo agentPromotionListBo){

        return agentPromotionLogic.queryPage(agentPromotionListBo);
    }


    /**
     * 代理商推广渠道信息
     * @param id 代理商推广渠道id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理商推广渠道信息")
    public R<AgentPromotionInfoVo> info(@Parameter(description = "代理商推广渠道id", required = true) @RequestParam("id") Long id){

        return agentPromotionLogic.info(id);
    }

    /**
     * 新增代理商推广渠道
     * @param agentPromotionBo 代理商推广渠道对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理商推广渠道")
    public R<String> save(@Parameter(description = "代理商推广渠道对象", required = true) @RequestBody AgentPromotionBo agentPromotionBo){

        return agentPromotionLogic.save(agentPromotionBo);
    }

    /**
     * 修改代理商推广渠道
     * @param agentPromotionBo 代理商推广渠道对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理商推广渠道")
    public R<String> update(@Parameter(description = "代理商推广渠道对象", required = true) @RequestBody AgentPromotionBo agentPromotionBo){

        return agentPromotionLogic.update(agentPromotionBo);
    }

    /**
     * 删除代理商推广渠道
     * @param id 代理商推广渠道id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理商推广渠道")
    public R<String> delete(@Parameter(description = "代理商推广渠道id", required = true) @RequestParam("id") Long id){

        return agentPromotionLogic.delete(id);
    }

}
