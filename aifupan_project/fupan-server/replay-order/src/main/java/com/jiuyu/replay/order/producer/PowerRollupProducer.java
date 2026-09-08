package com.jiuyu.replay.order.producer;

/**
 * 算力消耗汇总刷新
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
public interface PowerRollupProducer {

    /**
     * 增量刷新用户/租户算力消耗汇总表
     * 读水位线 → 唯一一条增量聚合 SQL → 内存归并租户维度 → 双表 += delta 累加 → 同事务推进水位线
     * 水位线行不存在（存量未初始化）时记 ERROR 并终止，不退化为全量跑
     */
    void refreshPowerRollup();
}
