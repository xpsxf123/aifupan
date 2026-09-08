# 每日统计表 (daily_stats)

预聚合的每日统计数据，用于快速查询各维度的业绩汇总。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `daily_stats` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `stats_date` DATE NOT NULL COMMENT '统计日期',
    `sec_uid` VARCHAR(128) DEFAULT NULL COMMENT '主播SecUid',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `room_id` BIGINT NOT NULL DEFAULT 0 COMMENT '直播间ID',
    `view_count` INT DEFAULT NULL COMMENT '场观',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放',
    `net_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '净销售额',
    `roi` DECIMAL(6,2) DEFAULT NULL COMMENT '投资回报率',
    `session_count` INT NOT NULL DEFAULT 0 COMMENT '场次数',
    `duration` INT NOT NULL DEFAULT 0 COMMENT '总时长（秒）',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| stats_date | DATE | 统计日期 |
| sec_uid | VARCHAR(128) | 主播SecUid |
| company_id | BIGINT | 公司ID |
| dept_id | BIGINT | 部门ID |
| team_id | BIGINT | 小组ID |
| room_id | BIGINT | 直播间ID |
| view_count | INT | 场观 |
| sales_revenue | DECIMAL(12,2) | 销售额 |
| refund | DECIMAL(12,2) | 退款 |
| investment | DECIMAL(12,2) | 投放 |
| net_sales | DECIMAL(12,2) | 净销售额 |
| roi | DECIMAL(6,2) | 投资回报率 |
| session_count | INT | 场次数或排班数 |
| duration | INT | 总时长（秒） |

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
| uk_stats_dimension | tenant_id, stats_date, sec_uid, company_id, dept_id, team_id, room_id | 唯一索引 |
| idx_tenant_date | tenant_id, stats_date | 按日期查询 |
| idx_tenant_company | tenant_id, company_id, dept_id, team_id | 按组织层级查询 |

## 组织维度说明

- `company_id=0`：全公司汇总
- `dept_id=0`：全部门汇总
- `team_id=0`：全小组汇总
- `room_id=0`：全直播间汇总

## 更新机制

使用 `INSERT ... ON DUPLICATE KEY UPDATE` 模式：

```sql
INSERT INTO daily_stats (
    id, tenant_id, stats_date, sec_uid, company_id, dept_id, team_id, room_id,
    view_count, end_sales, refund, investment, net_sales, roi, session_count, duration
) VALUES (...)
ON DUPLICATE KEY UPDATE
    view_count = VALUES(view_count),
    end_sales = VALUES(end_sales),
    refund = VALUES(refund),
    investment = VALUES(investment),
    net_sales = VALUES(net_sales),
    roi = VALUES(roi),
    session_count = VALUES(session_count),
    duration = VALUES(duration),
    update_date = NOW();
```

## 典型查询场景

1. **今日/昨日/本周/本月业绩**：按 stats_date 范围查询
2. **组织层级下钻**：按 company_id → dept_id → team_id 逐级查询
3. **直播间维度**：按 room_id 查询特定直播间业绩
4. **趋势图**：按日期范围查询，用于绘制近7/15/30/90天趋势
