package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.vo.words.TradeVo;

/**
 * 行业RSE接口
 *
 * @author jxy
 * @email 1776764427@qq.com
 */
public interface TradeRse {

    /**
     * 根据行业ID获取行业信息
     *
     * @param tradeId 行业ID
     * @return 行业信息
     */
    TradeVo getById(Long tradeId);
}
