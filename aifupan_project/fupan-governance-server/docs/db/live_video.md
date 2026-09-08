# 视频表 (live_video)

存储爱复盘推送的每个视频片段，作为数据推送的入口表，支持异步处理。

## 建表SQL

```sql
CREATE TABLE IF NOT EXISTS `live_video` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `batch_number` varchar(255) NOT NULL COMMENT '直播批次号',
    `video_id` varchar(255) NOT NULL COMMENT '视频唯一标识',
    `has_performance` TINYINT NOT NULL DEFAULT 0 COMMENT '业绩数据是否存在 0-丢失业绩数据，1-完好',
    `video_oss_url` VARCHAR(512) DEFAULT NULL COMMENT '视频文件OSS地址',
    `start_time` DATETIME NOT NULL COMMENT '视频开始时间',
    `end_time` DATETIME NOT NULL COMMENT '视频结束时间',
    `sec_uid` VARCHAR(128) DEFAULT NULL COMMENT '主播secUid',
    `platform` TINYINT NOT NULL DEFAULT 0 COMMENT '平台类型：0-抖音，1-快手，2-视频号',
    `process_status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理中，2-成功，3-失败',
    `process_time` DATETIME DEFAULT NULL COMMENT '最近处理时间',
    `fail_reason` LONGTEXT DEFAULT NULL COMMENT '处理失败原因',
    `view_count` INT DEFAULT NULL COMMENT '场观（累计观看人数）',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额（累计，单位：元）',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款（累计，单位：元）',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放（累计，单位：元）',
    `refund_quantity` INT DEFAULT NULL COMMENT '退款数量（累计）',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '销售单量',
    `net_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '净销售额（累计，单位：元）',
    `refund_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '退款率',
    `roi` DECIMAL(6,2) DEFAULT NULL COMMENT '投资回报率（ROI）',
    `thousand_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '千次成交',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频表';
```

## 字段定义

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | BIGINT | 主键，雪花ID |
| tenant_id | BIGINT | 租户ID |
| batch_number | VARCHAR(255) | 直播批次号 |
| video_id | VARCHAR(255) | 视频唯一标识 |
| has_performance | TINYINT | 业绩数据是否存在：0-丢失业绩数据，1-完好 |
| video_oss_url | VARCHAR(512) | 视频文件OSS地址 |
| start_time | DATETIME | 视频开始时间 |
| end_time | DATETIME | 视频结束时间 |
| sec_uid | VARCHAR(128) | 主播secUid |
| platform | TINYINT | 平台类型：0-抖音，1-快手，2-视频号 |
| process_status | TINYINT | 处理状态：0-待处理，1-处理中，2-成功，3-失败 |
| process_time | DATETIME | 最近处理时间 |
| fail_reason | LONGTEXT | 处理失败原因 |
| view_count | INT | 场观（累计观看人数） |
| sales_revenue | DECIMAL(12,2) | 销售额（累计，单位：元） |
| refund | DECIMAL(12,2) | 退款（累计，单位：元） |
| investment | DECIMAL(12,2) | 投放（累计，单位：元） |
| refund_quantity | INT | 退款数量（累计） |
| pay_combo_cnt | INT | 销售单量 |
| net_sales | DECIMAL(12,2) | 净销售额（累计，单位：元） |
| refund_rate | DECIMAL(6,2) | 退款率（退款数量/销售单量×100%） |
| roi | DECIMAL(6,2) | 投资回报率（ROI） |
| thousand_sales | DECIMAL(12,2) | 千次成交（成交金额/场观×1000） |

## 通用字段

| 字段 | 类型 | 说明 |
|-----|------|------|
| create_date | DATETIME | 创建时间 |
| update_date | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除：0-正常，-1-已删除 |

## 索引

| 索引名 | 字段 | 说明 |
|-------|------|------|
| PRIMARY | id | 主键 |

## 关联关系

- 与 `video_product` 表：一对多，一个视频包含多个商品

## 处理状态枚举

| 值 | 状态 | 说明 |
|---|------|------|
| 0 | 待处理 | 刚推送入库，等待定时任务处理 |
| 1 | 处理中 | 定时任务正在处理 |
| 2 | 处理成功 | 已成功更新场次和分摊数据 |
| 3 | 处理失败 | 处理异常，记录失败原因 |

## 业绩数据状态枚举

| 值 | 状态 | 说明 |
|---|------|------|
| 0 | 丢失业绩数据 | 视频对应的业绩数据缺失 |
| 1 | 完好 | 业绩数据完整 |

## 差值计算

视频的差值指标用于累加到场次：
- Δ销售额 = sales_revenue
