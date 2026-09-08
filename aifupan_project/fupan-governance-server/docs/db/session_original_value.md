# 原始值表 (session_original_value)

记录业绩数据首次修改前的原始值，用于前端对比显示修改标记（修改后显示绿色）。支持多种来源类型（场次、场次业绩、排班业绩）。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `session_original_value` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `source_id` BIGINT NOT NULL COMMENT '来源ID',
    `source_type` TINYINT NOT NULL DEFAULT 0 COMMENT '来源类型：0-live_session，1-session_performance，2-schedule_performance',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统，2-手动',
    `view_count` INT DEFAULT NULL COMMENT '原始场观',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '原始累计销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '原始退款总额',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '原始投放总额',
    `refund_quantity` INT DEFAULT NULL COMMENT '原始退款数量',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '原始销售单量',
    `net_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '原始净销售额',
    `refund_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始退款率',
    `roi` DECIMAL(6,2) DEFAULT NULL COMMENT '原始投资回报率',
    `thousand_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '原始千次成交',
    `exposure_count` INT DEFAULT NULL COMMENT '原始曝光次数',
    `follow_count` INT DEFAULT NULL COMMENT '原始涨粉人数',
    `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始点击-成交率',
    `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始互动率',
    `max_online` INT DEFAULT NULL COMMENT '原始最高在线',
    `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始带货转化率',
    `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT '原始UV价值',
    `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始涨粉率',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`, `source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始值表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| source_id | BIGINT | 来源ID（对应不同来源表的主键） |
| source_type | TINYINT | 来源类型：0-live_session，1-session_performance，2-schedule_performance |
| source | TINYINT | 数据来源：1-系统，2-手动 |
| view_count | INT | 原始场观 |
| sales_revenue | DECIMAL(12,2) | 原始累计销售额 |
| refund | DECIMAL(12,2) | 原始退款总额 |
| investment | DECIMAL(12,2) | 原始投放总额 |
| refund_quantity | INT | 原始退款数量 |
| pay_combo_cnt | INT | 原始销售单量 |
| net_sales | DECIMAL(12,2) | 原始净销售额 |
| refund_rate | DECIMAL(6,2) | 原始退款率 |
| roi | DECIMAL(6,2) | 原始投资回报率 |
| thousand_sales | DECIMAL(12,2) | 原始千次成交 |
| exposure_count | INT | 原始曝光次数 |
| follow_count | INT | 原始涨粉人数 |
| click_payment_rate | DECIMAL(6,2) | 原始点击-成交率 |
| interaction_rate | DECIMAL(6,2) | 原始互动率 |
| max_online | INT | 原始最高在线 |
| conversion_rate | DECIMAL(6,2) | 原始带货转化率 |
| uv_value | DECIMAL(12,2) | 原始UV价值 |
| follow_rate | DECIMAL(6,2) | 原始涨粉率 |
| create_date | DATETIME | 创建时间 |
| create_by | BIGINT | 创建人 |

## 索引

| 索引名 | 字段 | 说明 |
|-------|------|------|
| PRIMARY | id | 主键 |
| uk_tenant_source | tenant_id, source_id, source_type | 唯一索引，保证每个来源只有一条原始值记录 |

## 来源类型枚举

| 值 | 说明 |
|-----|------|
| 0 | live_session（场次表） |
| 1 | session_performance（场次业绩表） |
| 2 | schedule_performance（排班业绩表） |

## 数据来源枚举

| 值 | 说明 |
|-----|------|
| 1 | 系统（视频处理任务自动计算） |
| 2 | 手动（用户通过前端手动编辑） |

## 关联关系

- 与 `live_session` 表：source_type=0 时，source_id 关联 live_session.id
- 与 `session_performance` 表：source_type=1 时，source_id 关联 session_performance.id
- 与 `schedule_performance` 表：source_type=2 时，source_id 关联 schedule_performance.id

## 特殊说明

1. **主键规则**：使用雪花ID
2. **字段允许NULL**：只有被修改的字段才记录原始值，未修改的字段保持NULL
3. **只记录一次**：原始值在首次修改时记录，后续修改不更新此表
4. **无逻辑删除**：随主表删除即可
5. **无更新时间**：原始值只在创建时写入，不会被更新

## 前端使用说明

1. 查询业绩列表时，LEFT JOIN 此表获取原始值（需匹配 source_id 和 source_type）
2. 对比当前值与原始值：
   - 原始值字段为NULL：该字段未被修改过
   - 原始值字段不为NULL：该字段被修改过，前端显示绿色标记
3. 可在tooltip中展示原始值，方便用户查看修改前后对比
