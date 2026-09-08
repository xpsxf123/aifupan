# 排班与场次表 (schedule_session)

存储排班与场次之间的分摊结果，记录每个排班在每个场次中获得的业绩分摊。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `schedule_session` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `view_count` INT DEFAULT NULL COMMENT '分摊场观',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '分摊销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '分摊退款',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '分摊投放',
    `net_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '分摊净销售额',
    `roi` DECIMAL(6,2) DEFAULT NULL COMMENT '投资回报率',
    `duration` INT NOT NULL DEFAULT 0 COMMENT '重叠时长（秒）',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-自动分摊，2-手动录入',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_schedule_session` (`tenant_id`, `schedule_id`, `session_id`),
    KEY `idx_tenant_session` (`tenant_id`, `session_id`),
    KEY `idx_tenant_schedule` (`tenant_id`, `schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班与场次表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| schedule_id | BIGINT | 排班ID（引用外部排班表） |
| session_id | BIGINT | 场次ID |
| view_count | INT | 分摊场观 |
| sales_revenue | DECIMAL(12,2) | 分摊销售额 |
| refund | DECIMAL(12,2) | 分摊退款 |
| investment | DECIMAL(12,2) | 分摊投放 |
| net_sales | DECIMAL(12,2) | 分摊净销售额 |
| roi | DECIMAL(6,2) | 投资回报率 |
| duration | INT | 重叠时长（秒） |
| source | TINYINT | 数据来源：1-自动分摊，2-手动录入 |
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
| uk_tenant_schedule_session | tenant_id, schedule_id, session_id | 唯一索引，同一排班同一场次只有一条 |
| idx_tenant_session | tenant_id, session_id | 按场次查询分摊 |
| idx_tenant_schedule | tenant_id, schedule_id | 按排班查询分摊 |

## 关联关系

- 与 `live_session` 表：多对一，多个分摊记录属于同一场次
- 与排班表：多对一，多个分摊记录属于同一排班
- 与 `staff_performance` 表：影响人员业绩计算（仅source=1参与）

## 数据来源枚举

| 值 | 来源 | 说明 |
|---|------|------|
| 1 | 自动分摊 | 系统根据时间重叠自动计算，参与人员业绩 |
| 2 | 手动录入 | 用户手动录入，不参与人员业绩计算 |

## 分摊算法

```
重叠时长 = max(0, min(视频结束, 排班结束) - max(视频开始, 排班开始))
分摊比例 = 重叠时长 / 视频时长
分摊值 = 差值指标 × 分摊比例
```

## 特殊说明

1. **累加模式**：同一排班-场次已存在时，累加各指标和重叠时长
2. **人员业绩计算规则**：仅 source=1 的记录参与人员业绩计算
3. **手动录入**：手动录入数据参与每日统计，但不影响人员业绩
