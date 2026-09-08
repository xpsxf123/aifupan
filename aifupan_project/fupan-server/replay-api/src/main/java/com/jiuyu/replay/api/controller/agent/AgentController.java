package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.AgentBo;
import com.jiuyu.replay.agent.bo.AgentListBo;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentListVo;
import com.jiuyu.replay.api.logic.agent.AgentLogic;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@RestController
@CrossOrigin
@RequestMapping("replay/agent")
@Tag(name = "代理商")
public class AgentController {

    @Resource
    private AgentLogic agentLogic;

    /**
     * 代理商列表
     * @param agentListBo 代理商列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理商列表")
    public R<PageUtils<AgentListVo>> list(@Parameter(description = "代理商列表查询参数", required = true) @RequestBody AgentListBo agentListBo){

        return agentLogic.queryPage(agentListBo);
    }


    /**
     * 代理商信息
     * @param id 代理商id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理商信息")
    public R<AgentInfoVo> info(@Parameter(description = "代理商id", required = true) @RequestParam("id") Long id){

        return agentLogic.info(id);
    }

    /**
     * 代理商用户获取对应的代理商信息
     *
     * @return 代理商信息
     */
    @GetMapping("/currentUserInfo")
    @Operation(summary = "代理商用户获取对应的代理商信息")
    public R<AgentInfoVo> currentUserInfo() {
        return agentLogic.currentUserInfo();
    }

    /**
     * 新增代理商
     * @param agentBo 代理商对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理商")
    public R<String> save(@Parameter(description = "代理商对象", required = true) @RequestBody AgentBo agentBo){

        return agentLogic.save(agentBo);
    }

    /**
     * 修改代理商
     * @param agentBo 代理商对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理商")
    public R<String> update(@Parameter(description = "代理商对象", required = true) @RequestBody AgentBo agentBo) {
        // 验证必填字段
        RRException.isNotEmpty(agentBo.getId(), "代理商ID不能为空");
        RRException.isNotEmpty(agentBo.getChannelId(), "渠道ID不能为空");
        return agentLogic.update(agentBo);
    }

    /**
     * 删除代理商
     * @param id 代理商id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理商")
    public R<String> delete(@Parameter(description = "代理商id", required = true) @RequestParam("id") Long id){

        return agentLogic.delete(id);
    }

    @PostMapping("/updateAgentStatus")
    @Operation(summary = "修改代理商状态")
    public R<String> updateAgentStatus(@Parameter(description = "代理商对象", required = true) @RequestBody AgentBo agentBo) {
        return agentLogic.updateAgentStatus(agentBo.getId(), agentBo.getAgentStatus());
    }

}
