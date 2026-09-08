# 排班业绩人员表 (schedule_performance_staff)

记录排班业绩对应的人员信息，一个排班业绩可关联多个人员，每人记录对应的岗位。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `schedule_performance_staff` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_performance_id` BIGINT NOT NULL COMMENT '排班业绩ID',
    `employee_id` BIGINT NOT NULL COMMENT '员工ID',
    `employee_name` VARCHAR(50) DEFAULT NULL COMMENT '员工名称（冗余）',
    `position_id` BIGINT NOT NULL DEFAULT 0 COMMENT '岗位ID',
    `position_name` VARCHAR(100) DEFAULT NULL COMMENT '岗位名称（冗余）',
    `position_code` VARCHAR(50) DEFAULT NULL COMMENT '岗位编码',
    `is_schedule_staff` TINYINT NOT NULL DEFAULT 0 COMMENT '是否排班中的人员：0-否，1-是',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_performance_employee` (`tenant_id`, `schedule_performance_id`, `employee_id`),
    KEY `idx_tenant_performance` (`tenant_id`, `schedule_performance_id`),
    KEY `idx_tenant_employee` (`tenant_id`, `employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班业绩人员表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| schedule_performance_id | BIGINT | 排班业绩ID，关联 schedule_performance.id |
| employee_id | BIGINT | 员工ID，关联 employee.id |
| position_id | BIGINT | 岗位ID，关联 position.id |
| position_name | VARCHAR(100) | 岗位名称（冗余，避免关联查询） |
| position_code | VARCHAR(50) | 岗位编码 |
| is_schedule_staff | TINYINT | 是否排班中的人员：0-否，1-是 |

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
| uk_tenant_performance_employee | tenant_id, schedule_performance_id, employee_id | 唯一索引，同一排班业绩下同一员工只有一条记录 |
| idx_tenant_performance | tenant_id, schedule_performance_id | 按排班业绩查询人员 |
| idx_tenant_employee | tenant_id, employee_id | 按员工查询关联的排班业绩 |

## 关联关系

- 与 `schedule_performance` 表：多对一，多条人员记录属于同一排班业绩
- 与 `employee` 表：多对一，一个员工可出现在多条排班业绩中
- 与 `position` 表：多对一，记录员工在该排班中的岗位

## 数据来源

数据来源于 `work_schedule` 表。计算排班业绩时，查询与场次时间重叠的排班记录，将排班中的 `employee_id` 和 `position_id` 写入本表，同时冗余 `position_name`。

## 特殊说明

1. **一排班业绩多人员**：同一个排班业绩可关联多个人员（同一时间段不同人员值班）
2. **岗位名称冗余**：`position_name` 冗余存储，避免频繁关联查询岗位表
3. **唯一约束**：同一排班业绩下同一员工只能有一条记录
