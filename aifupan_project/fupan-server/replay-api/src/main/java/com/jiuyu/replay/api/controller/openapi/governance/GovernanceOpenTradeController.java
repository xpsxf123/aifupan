package com.jiuyu.replay.api.controller.openapi.governance;

import com.jiuyu.replay.api.controller.openapi.governance.response.IdName;
import com.jiuyu.replay.api.interceptor.FeatureSignature;
import com.jiuyu.replay.api.logic.words.TradeLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对企业管理平台 开发的行业API
 *
 * @author HeHui
 * @date 2026-03-24 18:01
 */
@FeatureSignature(client = "governance")
@RestController
@RequestMapping("/replay/openapi/governance/trade")
public class GovernanceOpenTradeController {

    private final TradeLogic tradeLogic;

    public GovernanceOpenTradeController(TradeLogic tradeLogic) {
        this.tradeLogic = tradeLogic;
    }


    /**
     * 获取行业列表（树形结构，简化版-客户端用）
     *
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     *
     * @return {@link R }<{@link List }<{@link TradeSimpleTreeVo }>>
     */
    @GetMapping("/simple-tree")
    public R<List<TradeSimpleTreeVo>> listSimpleTree(@RequestParam(required = false) Integer childrenNotNull) {
        return tradeLogic.listSimpleTree(childrenNotNull);
    }


    /**
     * 获取行业名称
     *
     * @param tradeIds 行业id列表
     *
     * @return {@link R }<{@link List }<{@link IdName }>>
     */
    @PostMapping("/name")
    public R<List<IdName>> getTradeNames(@RequestBody List<Long> tradeIds) {
        return R.ok(tradeLogic.getTradeNames(tradeIds));
    }
}
