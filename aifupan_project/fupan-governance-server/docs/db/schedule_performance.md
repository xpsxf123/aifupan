# 排班业绩表 (schedule_performance)

存储每个排班的业绩汇总，基于排班的自动分摊记录生成。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `schedule_performance` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `live_room_id` BIGINT NOT NULL DEFAULT 0 COMMENT '直播间ID',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `sec_uid` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '主播SecUid',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班业绩表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| schedule_id | BIGINT | 排班ID |
| live_room_id | BIGINT | 直播间ID |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
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
| idx_tenant_schedule | tenant_id, schedule_id | 按排班查询业绩 |
| idx_tenant_company | tenant_id, company_id, dept_id, team_id | 按组织层级查询 |

## 关联关系

- 与排班表：多对一，多条业绩记录属于同一排班
- 与 `schedule_session` 表：数据来源，汇总自动分摊记录

## 计算规则

1. **数据来源**：仅统计 `schedule_session` 中 `source=1`（自动分摊）且 `is_deleted=0` 的记录
2. **触发时机**：当排班对应的分摊记录变更时，异步触发重新计算
3. **计算流程**：
   - 汇总该排班所有自动分摊记录的各项指标
   - 删除该排班下的旧记录
   - 插入一条新的排班业绩记录

## 特殊说明

1. **手动录入数据不参与**：`schedule_session.source=2` 的记录不计入排班业绩
2. **一排班一记录**：同一排班只有一条业绩记录
