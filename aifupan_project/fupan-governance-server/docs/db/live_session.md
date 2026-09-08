# 场次表 (live_session)

存储一场直播的汇总信息，主键使用爱复盘推送的 session_id。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `live_session` (
    `id` BIGINT NOT NULL COMMENT '主键，对应爱复盘推送的session_id',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `live_room_id` BIGINT NOT NULL COMMENT '直播间ID',
    `batch_number` VARCHAR(64) DEFAULT NULL COMMENT '直播批次号',
    `start_time` DATETIME NOT NULL COMMENT '场次开始时间',
    `end_time` DATETIME NOT NULL COMMENT '场次结束时间',
    `duration` INT NOT NULL DEFAULT 0 COMMENT '总时长（秒）',
    `oss_url` VARCHAR(512) DEFAULT NULL COMMENT '实时数据',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统推送，2-手动录入',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `view_count` INT DEFAULT NULL COMMENT '总场观',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款总额',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放总额',
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
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户ID |
| live_room_id | BIGINT | 直播间ID |
| batch_number | VARCHAR(64) | 直播批次号 |
| start_time | DATETIME | 场次开始时间 |
| end_time | DATETIME | 场次结束时间 |
| duration | INT | 总时长（秒） |
| view_count | INT | 总场观 |
| sales_revenue | DECIMAL(12,2) | 销售额 |
| refund | DECIMAL(12,2) | 退款总额 |
| investment | DECIMAL(12,2) | 投放总额 |
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
| oss_url | VARCHAR(512) | 实时数据 |
| company_id | BIGINT | 公司ID |
| dept_id | BIGINT | 部门ID |
| team_id | BIGINT | 小组ID |
| source | TINYINT | 数据来源：1-系统推送，2-手动录入 |
| version | INT | 乐观锁版本号 |

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

- 与 `schedule_session` 表：一对多，一个场次关联多个排班
- 与 `session_product` 表：一对多，一个场次关联多个商品

## 特殊说明

1. **主键规则**：使用雪花ID
2. **乐观锁**：version 字段用于并发控制，更新时需校验版本号
3. **净销售额计算**：net_sales = sales_revenue - refund
4. **ROI计算**：roi = net_sales / investment（分母为0时为0）
