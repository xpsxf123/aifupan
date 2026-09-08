package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.words.entity.DouyinPeerAvgEntity;
import com.jiuyu.replay.words.vo.peer.DouyinPeerAvgVo;

import java.util.Date;
import java.util.List;

/**
 * 抖音同行直播基础数据平均值 Producer
 *
 * @author jy
 * @date 2026-06-24
 */
public interface DouyinPeerAvgProducer {

    /**
     * 按唯一键 (tradeId, statDate) upsert。
     *
     * @param entity 实体（含 tradeId + statDate + 所有 avg 字段）
     */
    void upsert(DouyinPeerAvgEntity entity);

    /**
     * 按行业ID和日期范围查询已聚合的平均值列表。
     *
     * @param tradeId   行业ID
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（不含）
     * @return 聚合结果列表
     */
    List<DouyinPeerAvgVo> listByTradeIdAndDateRange(Long tradeId, Date startDate, Date endDate);
}
