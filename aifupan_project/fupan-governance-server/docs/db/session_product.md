# 场次商品关联表 (session_product)

存储场次与商品的关联，包含商品维度的详细数据。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `session_product` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '销量',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `sales_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '销售额',
    `exposure_click_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '曝光点击率(%)',
    `exposure_conversion_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '曝光成交率(%)',
    `gpm` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '千次观看成交额',
    `refund_quantity` INT NOT NULL DEFAULT 0 COMMENT '退单量',
    `refund_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '退款额',
    `refund_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '退款率(%)',
    `click_payment_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '点击付款率(%)',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次商品关联表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| session_id | BIGINT | 场次ID |
| product_id | BIGINT | 商品ID |
| quantity | INT | 销量 |
| price | DECIMAL(10,2) | 单价 |
| sales_amount | DECIMAL(12,2) | 销售额 |
| exposure_click_rate | DECIMAL(5,2) | 曝光点击率(%) |
| exposure_conversion_rate | DECIMAL(5,2) | 曝光成交率(%) |
| gpm | DECIMAL(10,2) | 千次观看成交额 |
| refund_quantity | INT | 退单量 |
| refund_amount | DECIMAL(12,2) | 退款额 |
| refund_rate | DECIMAL(5,2) | 退款率(%) |
| click_payment_rate | DECIMAL(5,2) | 点击付款率(%) |
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
| uk_tenant_session_product | tenant_id, session_id, product_id | 唯一索引，同一场次同一商品只有一条 |
| idx_tenant_session | tenant_id, session_id | 按场次查询商品 |
| idx_tenant_product | tenant_id, product_id | 按商品查询场次 |

## 关联关系

- 与 `live_session` 表：多对一，多条商品记录属于同一场次
- 与 `product` 表：多对一，多条记录关联同一商品

## 指标说明

| 指标 | 计算方式 |
|-----|---------|
| 曝光成交率 | (成交人数 / 曝光人数) × 100% |
| 千次观看成交额 | (销售额 / 场观) × 1000 |
| 退款率 | (退款额 / 销售额) × 100% |
| 点击付款率 | (付款人数 / 点击人数) × 100% |

## 更新机制

同一场次同一商品已存在时，使用累加模式更新各指标。
