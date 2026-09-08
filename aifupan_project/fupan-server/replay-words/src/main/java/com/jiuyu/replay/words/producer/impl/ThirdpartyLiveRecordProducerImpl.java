package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.generic.vo.words.ThirdpartyLiveRecordVo;
import com.jiuyu.replay.words.entity.ThirdpartyLiveRecordEntity;
import com.jiuyu.replay.words.producer.ThirdpartyLiveRecordProducer;
import com.jiuyu.replay.words.repository.dao.ThirdpartyLiveRecordDao;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 第三方直播录制记录ProducerImpl
 *
 * @author System
 * @date 2026-04-09
 */
@Service
@AllArgsConstructor
public class ThirdpartyLiveRecordProducerImpl implements ThirdpartyLiveRecordProducer {

    private final ThirdpartyLiveRecordDao thirdpartyLiveRecordDao;

    @Override
    public List<ThirdpartyLiveRecordVo> randomByTradeIds(List<Long> tradeIds, int limit) {
        List<ThirdpartyLiveRecordEntity> entities = thirdpartyLiveRecordDao.randomByTradeIds(tradeIds, limit);
        return convertToVo(entities);
    }

    @Override
    public List<ThirdpartyLiveRecordVo> randomAll(int limit) {
        List<ThirdpartyLiveRecordEntity> entities = thirdpartyLiveRecordDao.randomAll(limit);
        return convertToVo(entities);
    }

    /**
     * Entity转Vo（只暴露三个字段）
     */
    private List<ThirdpartyLiveRecordVo> convertToVo(List<ThirdpartyLiveRecordEntity> entities) {
        List<ThirdpartyLiveRecordVo> voList = new ArrayList<>();
        for (ThirdpartyLiveRecordEntity entity : entities) {
            ThirdpartyLiveRecordVo vo = new ThirdpartyLiveRecordVo();
            vo.setCloudUrl(entity.getCloudUrl());
            vo.setViewers(entity.getViewers());
            vo.setMonthlySales(entity.getMonthlySales());
            voList.add(vo);
        }
        return voList;
    }
}
