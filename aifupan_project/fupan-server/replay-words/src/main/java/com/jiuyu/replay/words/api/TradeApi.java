package com.jiuyu.replay.words.api;

import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.words.TradeFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.words.bll.TradeBll;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 下午2:53
 */
@Component
@AllArgsConstructor
public class TradeApi implements TradeFeign {

    private final TradeBll tradeBll;

    @Override
    public List<TradeInfoVo> listTradeIdsByTradeId(Long tradeId, Integer withGeneral) {
        R<List<TradeInfoVo>> listR = tradeBll.listTradeIdsByTradeId(tradeId, withGeneral);
        RRException.create(listR);
        return listR.getData();
    }

    @Override
    public List<TradeVo> listTradeByIds(List<Long> ids) {
        return tradeBll.listTradeByIds(ids);
    }
}
