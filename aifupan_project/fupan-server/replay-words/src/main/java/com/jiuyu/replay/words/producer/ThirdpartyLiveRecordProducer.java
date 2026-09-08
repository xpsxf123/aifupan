package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.vo.words.ThirdpartyLiveRecordVo;

import java.util.List;

/**
 * 第三方直播录制记录Producer
 *
 * @author System
 * @date 2026-04-09
 */
public interface ThirdpartyLiveRecordProducer {

    /**
     * 按行业ID集合随机查询指定数量的记录
     *
     * @param tradeIds 行业ID集合
     * @param limit    查询数量
     * @return 记录列表
     */
    List<ThirdpartyLiveRecordVo> randomByTradeIds(List<Long> tradeIds, int limit);

    /**
     * 全表随机查询（兜底）
     *
     * @param limit 查询数量
     * @return 记录列表
     */
    List<ThirdpartyLiveRecordVo> randomAll(int limit);
}
