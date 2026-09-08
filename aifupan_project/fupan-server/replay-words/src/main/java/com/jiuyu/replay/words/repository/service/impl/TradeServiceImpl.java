package com.jiuyu.replay.words.repository.service.impl;

import com.jiuyu.framework.util.EmptyUtil;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.TradeDao;
import com.jiuyu.replay.words.entity.TradeEntity;
import com.jiuyu.replay.words.repository.service.TradeService;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;


@Service("tradeService")
public class TradeServiceImpl extends ServiceImpl<TradeDao, TradeEntity> implements TradeService {


    /**
     * 获取行业名称
     *
     * @param tradeIds 行业ID
     *
     * @return 行业名称
     */
    @Override
    public Map<Long, String> getTradeNameMap(Collection<Long> tradeIds) {
        if (EmptyUtil.isEmpty(tradeIds)) {
            return Map.of();
        }
        return super.lambdaQuery().in(TradeEntity::getId, tradeIds)
            .select(TradeEntity::getId, TradeEntity::getName)
            .list()
            .stream()
            .collect(Collectors.toMap(TradeEntity::getId, TradeEntity::getName, (a, b) -> a));
    }
}
