package com.jiuyu.replay.agent.controller;

import com.jiuyu.replay.agent.bll.AgentSaleBll;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ：lujie
 * &#064;description：代理商销售相关的控制器
 * @date ：2025/7/21 上午10:46
 */
@RestController
@RequestMapping("replay/agent/agentSale")
@Tag(name = "代理商销售相关的控制器")
public class AgentSaleController {

    @Resource
    private AgentSaleBll agentSaleBll;

    @GetMapping("/listByChannelId")
    @Operation(summary = "通过渠道ID查询代理商销售列表")
    public R<List<AgentSaleInfoVo>> listByChannelId(String channelId) {
        return R.ok(agentSaleBll.listByChannelId(channelId));
    }


}
