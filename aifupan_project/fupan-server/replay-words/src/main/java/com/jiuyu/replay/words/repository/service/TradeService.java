package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.TradeEntity;

import java.util.Collection;
import java.util.Map;

/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:10:10
 */
public interface TradeService extends IService<TradeEntity> {

    /**
     * 获取行业名称
     * @param tradeIds 行业ID
     * @return 行业名称
     */
    Map<Long, String> getTradeNameMap(Collection<Long> tradeIds);
}

