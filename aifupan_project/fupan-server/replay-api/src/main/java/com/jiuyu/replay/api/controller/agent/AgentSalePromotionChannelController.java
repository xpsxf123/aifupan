package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelListBo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelInfoVo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelListVo;
import com.jiuyu.replay.api.logic.agent.AgentSalePromotionChannelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@RestController
@CrossOrigin
@RequestMapping("replay/agentsalepromotionchannel")
@Tag(name = "代理商销售-渠道关联表")
public class AgentSalePromotionChannelController {

    @Resource
    private AgentSalePromotionChannelLogic agentSalePromotionChannelLogic;

    /**
     * 代理商销售-渠道关联表列表
     * @param agentSalePromotionChannelListBo 代理商销售-渠道关联表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理商销售-渠道关联表列表")
    public R<PageUtils<AgentSalePromotionChannelListVo>> list(@Parameter(description = "代理商销售-渠道关联表列表查询参数", required = true) @RequestBody AgentSalePromotionChannelListBo agentSalePromotionChannelListBo){

        return agentSalePromotionChannelLogic.queryPage(agentSalePromotionChannelListBo);
    }


    /**
     * 代理商销售-渠道关联表信息
     * @param id 代理商销售-渠道关联表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理商销售-渠道关联表信息")
    public R<AgentSalePromotionChannelInfoVo> info(@Parameter(description = "代理商销售-渠道关联表id", required = true) @RequestParam("id") Long id){

        return agentSalePromotionChannelLogic.info(id);
    }

    /**
     * 新增代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理商销售-渠道关联表")
    public R<String> save(@Parameter(description = "代理商销售-渠道关联表对象", required = true) @RequestBody AgentSalePromotionChannelBo agentSalePromotionChannelBo){

        return agentSalePromotionChannelLogic.save(agentSalePromotionChannelBo);
    }

    /**
     * 修改代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理商销售-渠道关联表")
    public R<String> update(@Parameter(description = "代理商销售-渠道关联表对象", required = true) @RequestBody AgentSalePromotionChannelBo agentSalePromotionChannelBo){

        return agentSalePromotionChannelLogic.update(agentSalePromotionChannelBo);
    }

    /**
     * 删除代理商销售-渠道关联表
     * @param id 代理商销售-渠道关联表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理商销售-渠道关联表")
    public R<String> delete(@Parameter(description = "代理商销售-渠道关联表id", required = true) @RequestParam("id") Long id){

        return agentSalePromotionChannelLogic.delete(id);
    }

}
