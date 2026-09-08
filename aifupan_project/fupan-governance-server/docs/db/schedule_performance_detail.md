# 排班业绩贡献明细表 (schedule_performance_detail)

记录每个场次对排班业绩的贡献明细。一个排班可能跨越多个场次，每个场次独立记录其贡献值，汇总后回写到 `schedule_performance`。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `schedule_performance_detail` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_performance_id` BIGINT NOT NULL COMMENT '排班业绩ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `start_time` DATETIME DEFAULT NULL COMMENT '重叠开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '重叠结束时间',
    `view_count` INT DEFAULT NULL COMMENT '场观',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放',
    `refund_quantity` INT DEFAULT NULL COMMENT '退款数量（累计）',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '销售单量',
    `net_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '净销售额',
    `refund_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '退款率',
    `roi` DECIMAL(6,2) DEFAULT NULL COMMENT '投资回报率',
    `thousand_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '千次成交',
    `exposure_count` INT DEFAULT NULL COMMENT '曝光次数',
    `follow_count` INT DEFAULT NULL COMMENT '涨粉人数',
    `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率',
    `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率',
    `max_online` INT DEFAULT NULL COMMENT '最高在线',
    `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率',
    `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值',
    `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_schedule_performance` (`tenant_id`, `schedule_performance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班业绩贡献明细表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| schedule_performance_id | BIGINT | 排班业绩ID，关联 schedule_performance.id |
| session_id | BIGINT | 场次ID，关联 live_session.id |
| start_time | DATETIME | 该场次与排班的重叠开始时间 |
| end_time | DATETIME | 该场次与排班的重叠结束时间 |
| view_count | INT | 该场次对排班的场观贡献 |
| sales_revenue | DECIMAL(12,2) | 该场次对排班的销售额贡献 |
| refund | DECIMAL(12,2) | 该场次对排班的退款贡献 |
| investment | DECIMAL(12,2) | 该场次对排班的投放贡献 |
| refund_quantity | INT | 退款数量（累计） |
| pay_combo_cnt | INT | 销售单量 |
| net_sales | DECIMAL(12,2) | 该场次对排班的净销售额贡献 |
| refund_rate | DECIMAL(6,2) | 退款率（退款数量/销售单量×100%） |
| roi | DECIMAL(6,2) | 该场次对排班的投资回报率 |
| thousand_sales | DECIMAL(12,2) | 千次成交（成交金额/场观×1000） |
| exposure_count | INT | 曝光次数 |
| follow_count | INT | 涨粉人数 |
| click_payment_rate | DECIMAL(6,2) | 点击-成交率 |
| interaction_rate | DECIMAL(6,2) | 互动率 |
| max_online | INT | 最高在线 |
| conversion_rate | DECIMAL(6,2) | 带货转化率（成交单量/场观×100%） |
| uv_value | DECIMAL(12,2) | UV价值（销售额/场观） |
| follow_rate | DECIMAL(6,2) | 涨粉率（涨粉人数/场观×100%） |

## 通用字段

| 字段 | 类型 | 说明 |
|-----|------|------|
| create_date | DATETIME | 创建时间 |
| update_date | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除：0-正常，-1-已删除 |

## 索引

| 索引名 | 字段 | 说明 |
|-------|------|------|
| PRIMARY | id | 主键 |
| uk_tenant_performance_session | tenant_id, schedule_performance_id, session_id | 唯一索引，同一排班业绩下同一场次只有一条贡献记录 |
| idx_tenant_schedule_performance | tenant_id, schedule_performance_id | 按排班业绩查询所有贡献明细 |

## 关联关系

- 与 `schedule_performance` 表：多对一，多条贡献明细属于同一排班业绩
- 与 `live_session` 表：多对一，一个场次可贡献到多个排班业绩

## 设计说明

1. **解决跨场次排班问题**：一个排班时间范围可能跨越多个场次，每个场次独立计算对该排班的业绩贡献并记录到本表
2. **天然幂等**：同一场次重复处理时，通过唯一索引 `(tenant_id, schedule_performance_id, session_id)` upsert，只会覆盖自己的贡献记录
3. **汇总回写**：每次写入贡献明细后，SUM 该排班下所有明细的业绩字段，回写到 `schedule_performance` 主表
