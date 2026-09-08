package com.jiuyu.governance.openfeign.replay;


import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.openfeign.replay.response.TradeSimpleTreeResponse;

import java.util.List;
import java.util.Map;

/**
 * 行业服务
 *
 * @author HeHui
 * @date 2026-03-26 16:42
 */
public interface TradeService {


    /**
     * 获取行业树结构
     *
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     *
     * @return 行业树结构
     */
    ApiResponse<List<TradeSimpleTreeResponse>> getTradeSimpleTree(int childrenNotNull);


    /**
     * 获取行业名称
     *
     * @param tradeIds 行业id列表
     *
     * @return 行业名称
     */
    Map<Long, String> getTradeNameMap(List<Long> tradeIds);
}
