package com.jiuyu.governance.openfeign.replay.impl;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.governance.common.pojo.bo.IdName;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.TradeService;
import com.jiuyu.governance.openfeign.replay.response.TradeSimpleTreeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行业服务 实现
 *
 * @author HeHui
 * @date 2026-03-26 16:44
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final ReplayHttpServer replayHttpServer;

    /**
     * 获取行业树结构
     *
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     *
     * @return 行业树结构
     */
    @Override
    public ApiResponse<List<TradeSimpleTreeResponse>> getTradeSimpleTree(int childrenNotNull) {
        return replayHttpServer.get("/replay/openapi/governance/trade/simple-tree?childrenNotNull=" + childrenNotNull, null)
            .retrieve().body(ReplayApiResponseType.TRADE_SIMPLE_TREE_LIST_TYPE);
    }

    /**
     * 获取行业名称
     *
     * @param tradeIds 行业id列表
     *
     * @return 行业名称
     */
    @Override
    public Map<Long, String> getTradeNameMap(List<Long> tradeIds) {
        if (EmptyUtil.isEmpty(tradeIds)) {
            return Map.of();
        }
        ApiResponse<List<IdName>> apiResponse = replayHttpServer.post("/replay/openapi/governance/trade/name", tradeIds, null)
            .retrieve().body(ReplayApiResponseType.ID_NAME_LIST_TYPE);
        if (apiResponse == null) {
            return Map.of();
        }
        if (apiResponse.failed()) {
            log.error("获取行业名称失败：{}", apiResponse.getMsg());
            return Map.of();
        }
        if (EmptyUtil.isEmpty(apiResponse.getData())) {
            return Map.of();
        }
        return apiResponse.getData().stream().collect(Collectors.toMap(IdName::getId, IdName::getName, FunctionUtil::mergeFirst));

    }
}
