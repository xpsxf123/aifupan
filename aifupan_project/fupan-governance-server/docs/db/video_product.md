# 视频商品表 (video_product)

存储爱复盘推送的视频关联商品数据，一个视频对应多个商品。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `video_product` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `video_id` VARCHAR(64) NOT NULL COMMENT '爱复盘视频ID',
    `batch_number` VARCHAR(64) DEFAULT NULL COMMENT '直播批次号',
    `product_id` VARCHAR(64) NOT NULL COMMENT '商品ID',
    `title` VARCHAR(255) NOT NULL COMMENT '商品标题',
    `image_uri` VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL',
    `market_price` DECIMAL(10,2) DEFAULT NULL COMMENT '到手价（元）',
    `product_bind_time` DATETIME DEFAULT NULL COMMENT '直播间上架时间',
    `product_down_time` DATETIME DEFAULT NULL COMMENT '直播间下架时间',
    `explain_cnt` INT DEFAULT NULL COMMENT '讲解次数',
    `product_show_ucnt` BIGINT DEFAULT NULL COMMENT '商品曝光人数',
    `product_click_ucnt` BIGINT DEFAULT NULL COMMENT '商品点击人数',
    `product_show_click_ucnt_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '曝光-点击转化率',
    `product_show_pay_ucnt_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '曝光-成交转化率',
    `product_click_pay_ucnt_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '点击-成交转化率',
    `gpm` DECIMAL(10,2) DEFAULT NULL COMMENT '商品千次曝光成交金额（元）',
    `pay_amt` DECIMAL(15,2) DEFAULT NULL COMMENT '累计成交金额（元）',
    `avg_max_pay_amt_min` DECIMAL(15,2) DEFAULT NULL COMMENT '分钟最高成交金额（元）',
    `pay_combo_cnt` BIGINT DEFAULT NULL COMMENT '累计成交件数',
    `pay_cnt` BIGINT DEFAULT NULL COMMENT '累计成交订单数',
    `create_cnt` BIGINT DEFAULT NULL COMMENT '创建订单数',
    `create_pay_ucnt_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '订单支付率',
    `pay_deposit_pre_order_cnt` BIGINT DEFAULT NULL COMMENT '预售订单数',
    `presale_depay_deamt` DECIMAL(15,2) DEFAULT NULL COMMENT '预售定金金额（元）',
    `pay_deposit_pre_order_amt` DECIMAL(15,2) DEFAULT NULL COMMENT '预售全款金额（元）',
    `refund_cnt` BIGINT DEFAULT NULL COMMENT '退款订单数',
    `real_refund_amt` DECIMAL(15,2) DEFAULT NULL COMMENT '退款金额（元）',
    `refund_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '退款率',
    `commodity_process_data` LONGTEXT DEFAULT NULL COMMENT '商品过程数据（JSON原始数据）',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_video_product` (`tenant_id`, `video_id`, `product_id`),
    KEY `idx_tenant_video` (`tenant_id`, `video_id`),
    KEY `idx_tenant_batch` (`tenant_id`, `batch_number`),
    KEY `idx_tenant_product` (`tenant_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频商品表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| video_id | VARCHAR(64) | 爱复盘视频ID |
| batch_number | VARCHAR(64) | 直播批次号 |
| product_id | VARCHAR(64) | 商品ID |
| title | VARCHAR(255) | 商品标题 |
| image_uri | VARCHAR(500) | 商品图片URL |
| market_price | DECIMAL(10,2) | 到手价（元） |
| product_bind_time | DATETIME | 直播间上架时间 |
| product_down_time | DATETIME | 直播间下架时间 |
| explain_cnt | INT | 讲解次数 |
| product_show_ucnt | BIGINT | 商品曝光人数 |
| product_click_ucnt | BIGINT | 商品点击人数 |
| product_show_click_ucnt_ratio | DECIMAL(5,4) | 曝光-点击转化率 |
| product_show_pay_ucnt_ratio | DECIMAL(5,4) | 曝光-成交转化率 |
| product_click_pay_ucnt_ratio | DECIMAL(5,4) | 点击-成交转化率 |
| gpm | DECIMAL(10,2) | 商品千次曝光成交金额（元） |
| pay_amt | DECIMAL(15,2) | 累计成交金额（元） |
| avg_max_pay_amt_min | DECIMAL(15,2) | 分钟最高成交金额（元） |
| pay_combo_cnt | BIGINT | 累计成交件数 |
| pay_cnt | BIGINT | 累计成交订单数 |
| create_cnt | BIGINT | 创建订单数 |
| create_pay_ucnt_ratio | DECIMAL(5,4) | 订单支付率 |
| pay_deposit_pre_order_cnt | BIGINT | 预售订单数 |
| presale_depay_deamt | DECIMAL(15,2) | 预售定金金额（元） |
| pay_deposit_pre_order_amt | DECIMAL(15,2) | 预售全款金额（元） |
| refund_cnt | BIGINT | 退款订单数 |
| real_refund_amt | DECIMAL(15,2) | 退款金额（元） |
| refund_rate | DECIMAL(5,4) | 退款率 |
| commodity_process_data | LONGTEXT | 商品过程数据（JSON原始数据） |

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
| uk_tenant_video_product | tenant_id, video_id, product_id | 唯一索引，同一视频同一商品只有一条 |
| idx_tenant_video | tenant_id, video_id | 按视频查询商品 |
| idx_tenant_batch | tenant_id, batch_number | 按批次号查询商品 |
| idx_tenant_product | tenant_id, product_id | 按商品ID查询 |

## 关联关系

- 与 `live_video` 表：多对一，多条商品记录属于同一视频
- 与 `live_session` 表：多对一，通过 batch_number 关联场次

## 数据来源

数据来源于爱复盘推送的 `tb_product_details` 表，在接收视频数据时同步接收商品数据。

## 指标说明

| 指标 | 计算方式 |
|-----|---------|
| 曝光-点击转化率 | 商品点击人数 / 商品曝光人数 |
| 曝光-成交转化率 | 成交人数 / 商品曝光人数 |
| 点击-成交转化率 | 成交人数 / 商品点击人数 |
| GPM | 商品千次曝光成交金额 |
| 订单支付率 | 成交订单数 / 创建订单数 |
| 退款率 | 退款订单数 / 成交订单数 |
