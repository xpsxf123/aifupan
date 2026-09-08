package com.jiuyu.replay.words.rse.impl;

import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.words.entity.TradeEntity;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.rse.TradeRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 行业RSE接口实现类
 *
 * @author jxy
 * @email 1776764427@qq.com
 */
@Service
public class TradeRseImpl implements TradeRse {

    @Resource
    private TradeService tradeService;

    @Override
    public TradeVo getById(Long tradeId) {
        if (tradeId == null) {
            return null;
        }
        TradeEntity tradeEntity = this.tradeService.getById(tradeId);
        if (tradeEntity != null) {
            return BeanConvertUtils.convert(tradeEntity, TradeVo.class);
        }
        return null;
    }
}
