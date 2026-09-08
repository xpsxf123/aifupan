package com.jiuyu.replay.order.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 算力增量聚合行载体
 * 承载一条 (userId, tenantId) 维度的净增量聚合结果
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Data
public class PowerAggregateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID(实际消费者)。租户维度聚合结果复用本 VO 时该字段为 null
     */
    private Long userId;

    /**
     * 租户id(tb_tenant.id，由主账号 JOIN tb_tenant 取得)
     */
    private Long tenantId;

    /**
     * 本区间净增量(token数) = SUM(CASE WHEN signs = 0 THEN quantity ELSE -quantity END)
     */
    private Long powerConsume;

    /**
     * 本组扫描到的明细表最大 id，用于推进水位线
     */
    private Long maxDetailId;
}
