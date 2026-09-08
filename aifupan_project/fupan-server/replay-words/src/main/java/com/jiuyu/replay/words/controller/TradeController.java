package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.words.bll.TradeBll;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("wordsTradeController")
@RequestMapping("replay/trade")
@Tag(name = "客户端行业")
public class TradeController {

    @Resource
    private TradeBll tradeBll;

    /**
     * 根据行业ID获取行业信息
     * @param tradeId 行业ID
     * @return 行业信息
     */
    @GetMapping("/getById")
    @Operation(summary = "根据行业ID获取行业信息")
    public R<TradeVo> getById(@Parameter(description = "行业ID", required = true) @RequestParam Long tradeId) {
        return tradeBll.getTradeById(tradeId);
    }
}
