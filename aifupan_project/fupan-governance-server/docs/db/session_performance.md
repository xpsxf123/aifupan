# 场次业绩表 (session_performance)

以天为维度存储场次的业绩数据。数据来源为 `live_session` 表，场次在 `live_session` 中唯一，但场次可能跨天，因此一个场次在 `session_performance` 中可能对应多天的记录。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `session_performance` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `stats_date` DATE NOT NULL COMMENT '统计日期',
    `start_time` DATETIME DEFAULT NULL COMMENT '当天开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '当天结束时间',
    `sec_uid` VARCHAR(128) DEFAULT NULL COMMENT '主播SecUid',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统，2-手动',
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
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次业绩表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| session_id | BIGINT | 场次ID，关联 live_session.id |
| stats_date | DATE | 统计日期 |
| start_time | DATETIME | 当天开始时间（场次在该天的起始时间） |
| end_time | DATETIME | 当天结束时间（场次在该天的结束时间） |
| sec_uid | VARCHAR(128) | 主播SecUid |
| source | TINYINT | 数据来源：1-系统，2-手动 |
| view_count | INT | 场观 |
| sales_revenue | DECIMAL(12,2) | 销售额 |
| refund | DECIMAL(12,2) | 退款 |
| investment | DECIMAL(12,2) | 投放 |
| refund_quantity | INT | 退款数量（累计） |
| pay_combo_cnt | INT | 销售单量 |
| net_sales | DECIMAL(12,2) | 净销售额 |
| refund_rate | DECIMAL(6,2) | 退款率（退款数量/销售单量×100%） |
| roi | DECIMAL(6,2) | 投资回报率 |
| thousand_sales | DECIMAL(12,2) | 千次成交（成交金额/场观×1000） |
| exposure_count | INT | 曝光次数 |
| follow_count | INT | 涨粉人数 |
| click_payment_rate | DECIMAL(6,2) | 点击-成交率 |
| interaction_rate | DECIMAL(6,2) | 互动率 |
| max_online | INT | 最高在线 |
| conversion_rate | DECIMAL(6,2) | 带货转化率（成交单量/场观×100%） |
| uv_value | DECIMAL(12,2) | UV价值（销售额/场观） |
| follow_rate | DECIMAL(6,2) | 涨粉率（涨粉人数/场观×100%） |
| company_id | BIGINT | 公司ID |
| dept_id | BIGINT | 部门ID |
| team_id | BIGINT | 小组ID |

## 通用字段

| 字段 | 类型 | 说明 |
|-----|------|------|
| create_date | DATETIME | 创建时间 |
| update_date | DATETIME | 更新时间 |
| create_by | BIGINT | 创建人 |
| update_by | BIGINT | 更新人 |
| is_deleted | TINYINT | 逻辑删除：0-正常，-1-已删除 |

## 索引

| 索引名 | 字段 | 说明 |
|-------|------|------|
| PRIMARY | id | 主键 |

## 关联关系

- 与 `live_session` 表：多对一，一个场次跨天时产生多条记录

## 数据来源

数据来源为 `live_session` 表。当场次创建或更新时，根据场次的 `start_time` 和 `end_time` 拆分为按天的业绩记录。

## 跨天场次处理

当场次跨天时（例如：2026-03-24 22:00 ~ 2026-03-25 02:00），会生成两条记录：

| stats_date | start_time | end_time |
|-----------|------------|----------|
| 2026-03-24 | 2026-03-24 22:00 | 2026-03-24 23:59:59 |
| 2026-03-25 | 2026-03-25 00:00 | 2026-03-25 02:00 |

## 特殊说明

1. **一场次一天一记录**：同一场次在同一天只有一条业绩记录
2. **跨天拆分**：场次跨天时按天拆分，每天各一条记录
3. **业绩数据**：业绩指标（场观、销售额等）取自 `live_session` 的汇总数据，跨天场景下各天记录的业绩数据相同（均为场次总业绩）
4. **净销售额计算**：net_sales = sales_revenue - refund
5. **ROI计算**：roi = net_sales / investment（分母为0时为0）
