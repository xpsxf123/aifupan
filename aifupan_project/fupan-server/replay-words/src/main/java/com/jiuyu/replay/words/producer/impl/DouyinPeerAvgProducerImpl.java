package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.entity.DouyinPeerAvgEntity;
import com.jiuyu.replay.words.producer.DouyinPeerAvgProducer;
import com.jiuyu.replay.words.repository.service.DouyinPeerAvgService;
import com.jiuyu.replay.words.vo.peer.DouyinPeerAvgVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 抖音同行直播基础数据平均值 ProducerImpl
 *
 * @author jy
 * @date 2026-06-24
 */
@Service
@Slf4j
public class DouyinPeerAvgProducerImpl implements DouyinPeerAvgProducer {

    @Resource
    private DouyinPeerAvgService douyinPeerAvgService;

    @Override
    public void upsert(DouyinPeerAvgEntity entity) {
        DouyinPeerAvgEntity existing = douyinPeerAvgService.lambdaQuery()
                .eq(DouyinPeerAvgEntity::getTradeId, entity.getTradeId())
                .eq(DouyinPeerAvgEntity::getStatDate, entity.getStatDate())
                .last("limit 1")
                .one();

        if (existing != null) {
            entity.setId(existing.getId());
            entity.setUpdateDate(new Date());
            douyinPeerAvgService.updateById(entity);
        } else {
            entity.setId(SnowflakeManager.nextValue());
            entity.setCreateDate(new Date());
            entity.setUpdateDate(new Date());
            entity.setIsDeleted(0);
            douyinPeerAvgService.save(entity);
        }
    }

    @Override
    public List<DouyinPeerAvgVo> listByTradeIdAndDateRange(Long tradeId, Date startDate, Date endDate) {
        List<DouyinPeerAvgEntity> list = douyinPeerAvgService.list(
                new LambdaQueryWrapper<DouyinPeerAvgEntity>()
                        .eq(DouyinPeerAvgEntity::getTradeId, tradeId)
                        .ge(DouyinPeerAvgEntity::getStatDate, startDate)
                        .lt(DouyinPeerAvgEntity::getStatDate, endDate)
                        .orderByDesc(DouyinPeerAvgEntity::getStatDate)
        );

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.stream().map(DouyinPeerAvgVo::from).collect(Collectors.toList());
    }
}
