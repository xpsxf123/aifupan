# 商品表 (product)

存储商品基础信息。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `product_id` VARCHAR(256) NOT NULL COMMENT '商品ID',
    `name` VARCHAR(256) NOT NULL COMMENT '商品名称',
    `image_uri` VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_name` (`tenant_id`, `name`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| product_id | VARCHAR(256) | 商品ID |
| name | VARCHAR(256) | 商品名称 |
| image_uri | VARCHAR(500) | 商品图片URL |

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
| idx_tenant_name | tenant_id, name(64) | 按商品名称查询（前64字符） |

## 关联关系

- 与 `session_product` 表：一对多，一个商品关联多个场次
