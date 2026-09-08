package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;

import java.util.List;

public interface TradeFeign {

    List<TradeInfoVo> listTradeIdsByTradeId(Long tradeId, Integer withGeneral);

    List<TradeVo> listTradeByIds(List<Long> ids);
}
