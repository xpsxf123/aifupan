package com.jiuyu.governance.business.room.controller;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.openfeign.replay.TradeService;
import com.jiuyu.governance.openfeign.replay.response.TradeSimpleTreeResponse;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业端 - 行业API
 *
 * @author HeHui
 * @date 2026-03-26 17:02
 */
@RestController
@RequestMapping("/api/governance/trade")
@GovernanceUser
@RequiredArgsConstructor
@Slf4j
public class TradeController {

    private final TradeService tradeService;


    /**
     * 获取行业树结构
     *
     * @return 行业树结构
     */
    @GetMapping("/tree")
    public ApiResponse<List<TradeSimpleTreeResponse>> getTradeSimpleTree() {
        return tradeService.getTradeSimpleTree(1);
    }
}
