# 排班业绩凭证图片表 (schedule_performance_image)

存储排班业绩的凭证图片URL，与schedule_performance表一对一关联。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `schedule_performance_image` (
    `id` BIGINT NOT NULL COMMENT '主键，关联schedule_performance.id',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `view_count_image_url` VARCHAR(512) DEFAULT NULL COMMENT '场观图片URL',
    `sales_revenue_image_url` VARCHAR(512) DEFAULT NULL COMMENT '销售额图片URL',
    `refund_image_url` VARCHAR(512) DEFAULT NULL COMMENT '退款图片URL',
    `investment_image_url` VARCHAR(512) DEFAULT NULL COMMENT '投放图片URL',
    `net_sales_image_url` VARCHAR(512) DEFAULT NULL COMMENT '净销售额图片URL',
    `roi_image_url` VARCHAR(512) DEFAULT NULL COMMENT 'ROI图片URL',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班业绩凭证图片表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，关联schedule_performance.id |
| tenant_id | BIGINT | 租户ID |
| view_count_image_url | VARCHAR(512) | 场观图片URL |
| sales_revenue_image_url | VARCHAR(512) | 销售额图片URL |
| refund_image_url | VARCHAR(512) | 退款图片URL |
| investment_image_url | VARCHAR(512) | 投放图片URL |
| net_sales_image_url | VARCHAR(512) | 净销售额图片URL |
| roi_image_url | VARCHAR(512) | ROI图片URL |

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
| idx_tenant_id | tenant_id | 按租户查询 |

## 关联关系

- 与 `schedule_performance` 表：一对一可选关系
- `id` = `schedule_performance.id`（主键即外键）
- 有图片凭证时才创建记录

## 字段对应关系

| 图片字段 | 对应数值字段（schedule_performance表） |
|---------|---------------------------------------|
| view_count_image_url | view_count |
| sales_revenue_image_url | sales_revenue |
| refund_image_url | refund |
| investment_image_url | investment |
| net_sales_image_url | net_sales |
| roi_image_url | roi |

## 使用场景

1. **手动录入数据**：用户上传业绩数据时，同时上传凭证截图
2. **数据审核**：通过图片URL查看原始凭证，验证数据准确性
3. **数据溯源**：记录数据来源的图片证据

## 特殊说明

1. **一对一可选**：不是每条业绩记录都有图片凭证
2. **主键复用**：id直接使用schedule_performance的id，无需单独生成
