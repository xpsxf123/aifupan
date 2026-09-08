-- =====================================================
-- 业绩模块数据库表结构
-- 版本: V8.0
-- 创建日期: 2026-03-18
-- 更新日期: 2026-04-22 - schedulePerformanceSave接口字段扩展：新增5个凭证图片URL字段
-- =====================================================

-- 1. 场次表 (live_session)
-- 存储一场直播的汇总信息，主键使用爱复盘推送的 session_id
CREATE TABLE IF NOT EXISTS `live_session` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `live_room_id` BIGINT NOT NULL COMMENT '直播间ID',
    `batch_number` VARCHAR(64) DEFAULT NULL COMMENT '直播批次号',
    `start_time` DATETIME NOT NULL COMMENT '场次开始时间',
    `end_time` DATETIME NOT NULL COMMENT '场次结束时间',
    `duration` INT NOT NULL DEFAULT 0 COMMENT '总时长（秒）',
    `view_count` INT DEFAULT NULL COMMENT '总场观',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '累计销售额',
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
    `oss_url` VARCHAR(512) DEFAULT NULL COMMENT '实时数据',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统推送，2-手动录入',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次表';

-- 2. 视频表 (live_video)
-- 存储爱复盘推送的每个视频片段
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
    `data_update_time` DATETIME DEFAULT NULL COMMENT '数据最新更新时间（最近一次业绩数据推送时间）',
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
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频表';

-- 3. 排班与场次表 (schedule_session)
-- 存储排班与场次之间的分摊结果
CREATE TABLE IF NOT EXISTS `schedule_session` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '分摊场观',
    `sales_revenue` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '开始销售额',
    `refund` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '分摊退款',
    `investment` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '分摊投放',
    `net_sales` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '分摊净销售额',
    `roi` DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '投资回报率',
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

-- 4. 人员业绩表 (staff_performance)
-- 存储每个人员（按排班）的业绩汇总
CREATE TABLE IF NOT EXISTS `staff_performance` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '人员ID',
    `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `stats_date` DATE NOT NULL COMMENT '排班日期',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '场观',
    `sales_revenue` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '销售额',
    `refund` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '退款',
    `investment` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '投放',
    `net_sales` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '净销售额',
    `roi` DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '投资回报率',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_tenant_schedule` (`tenant_id`, `schedule_id`),
    KEY `idx_tenant_date` (`tenant_id`, `stats_date`),
    KEY `idx_tenant_company` (`tenant_id`, `company_id`, `dept_id`, `team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人员业绩表';

-- 5. 每日统计表 (daily_stats)
-- 预聚合的每日统计数据
CREATE TABLE IF NOT EXISTS `daily_stats` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `stats_date` DATE NOT NULL COMMENT '统计日期',
    `dimension` TINYINT NOT NULL DEFAULT 1 COMMENT '维度：1-场次维度，2-排班维度',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID，0表示全局',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID，0表示全局',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID，0表示全局',
    `room_id` BIGINT NOT NULL DEFAULT 0 COMMENT '直播间ID，0表示全局',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '场观',
    `sales_revenue` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '销售额',
    `refund` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '退款',
    `investment` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '投放',
    `net_sales` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '净销售额',
    `roi` DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '投资回报率',
    `session_count` INT NOT NULL DEFAULT 0 COMMENT '场次数',
    `duration` INT NOT NULL DEFAULT 0 COMMENT '总时长（秒）',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stats_dimension` (`tenant_id`, `stats_date`, `dimension`, `company_id`, `dept_id`, `team_id`, `room_id`),
    KEY `idx_tenant_date` (`tenant_id`, `stats_date`),
    KEY `idx_tenant_company` (`tenant_id`, `company_id`, `dept_id`, `team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计表';

-- 6. 商品表 (product)
-- 商品基础信息
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `name` VARCHAR(256) NOT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_name` (`tenant_id`, `name`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 7. 场次商品关联表 (session_product)
-- 场次与商品的关联，包含商品维度的详细数据
CREATE TABLE IF NOT EXISTS `session_product` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '销量',
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_session_product` (`tenant_id`, `session_id`, `product_id`),
    KEY `idx_tenant_session` (`tenant_id`, `session_id`),
    KEY `idx_tenant_product` (`tenant_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次商品关联表';

-- 8. 视频商品表 (video_product)
-- 存储爱复盘推送的视频关联商品数据
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
    `product_oss_key` VARCHAR(500) DEFAULT NULL COMMENT '商品过程数据OSS路径（governance-process/.../{productId}.json）',
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

-- 9. 场次业绩表 (session_performance)
-- 以天为维度的场次业绩数据
CREATE TABLE IF NOT EXISTS `session_performance` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `stats_date` DATE NOT NULL COMMENT '统计日期',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `sec_uid` VARCHAR(128) DEFAULT NULL COMMENT '主播SecUid',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统，2-手动',
    `view_count` INT DEFAULT NULL COMMENT '场观（观看人数）',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款',
    `refund_quantity` INT DEFAULT NULL COMMENT '退款单量',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '成交单量',
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
    `live_room_id` BIGINT NOT NULL DEFAULT 0 COMMENT '直播间ID',
    `company_id` BIGINT NOT NULL DEFAULT 0 COMMENT '公司ID',
    `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
    `team_id` BIGINT NOT NULL DEFAULT 0 COMMENT '小组ID',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_session` (`tenant_id`, `session_id`),
    KEY `idx_tenant_stats_date` (`tenant_id`, `stats_date`),
    KEY `idx_tenant_live_room` (`tenant_id`, `live_room_id`),
    KEY `idx_tenant_company` (`tenant_id`, `company_id`, `dept_id`, `team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次业绩表';

-- 10. 场次原始值表 (session_original_value)
-- 记录场次业绩数据首次修改前的原始值，用于前端对比显示修改标记
CREATE TABLE IF NOT EXISTS `session_original_value` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `source_id` BIGINT NOT NULL COMMENT '来源ID',
    `source_type` TINYINT NOT NULL DEFAULT 0 COMMENT '来源类型：0-live_session，1-session_performance，2-schedule_performance',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统，2-手动',
    `view_count` INT DEFAULT NULL COMMENT '原始场观（观看人数）',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '原始累计销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '原始退款总额',
    `refund_quantity` INT DEFAULT NULL COMMENT '原始退款单量',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '原始投放总额',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '原始成交单量',
    `net_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '原始净销售额',
    `refund_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始退款率',
    `roi` DECIMAL(6,2) DEFAULT NULL COMMENT '原始投资回报率',
    `thousand_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '原始千次成交',
    `exposure_count` INT DEFAULT NULL COMMENT '原始曝光次数',
    `follow_count` INT DEFAULT NULL COMMENT '原始涨粉人数',
    `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始点击-成交率',
    `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始互动率',
    `max_online` INT DEFAULT NULL COMMENT '原始最高在线',
    `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始带货转化率',
    `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT '原始UV价值',
    `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '原始涨粉率',
    `create_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_source` (`tenant_id`, `source_id`, `source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次原始值表';

-- 11. 排班业绩凭证图片表 (schedule_performance_image)
-- 存储排班业绩的凭证图片URL，与schedule_performance一对一关联
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

-- 12. 排班业绩表 (schedule_performance)
-- 存储排班维度的业绩汇总数据
CREATE TABLE IF NOT EXISTS `schedule_performance` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `live_room_id` BIGINT NOT NULL COMMENT '直播间ID',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `sec_uid` VARCHAR(128) DEFAULT '' COMMENT '主播SecUid',
    `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统，2-手动',
    `view_count` INT DEFAULT NULL COMMENT '场观（观看人数）',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款',
    `refund_quantity` INT DEFAULT NULL COMMENT '退款单量',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '成交单量',
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_schedule` (`tenant_id`, `schedule_id`),
    KEY `idx_tenant_live_room` (`tenant_id`, `live_room_id`),
    KEY `idx_tenant_company` (`tenant_id`, `company_id`, `dept_id`, `team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班业绩表';

-- 13. 排班业绩明细表 (schedule_performance_detail)
-- 存储排班业绩对应的场次明细数据
CREATE TABLE IF NOT EXISTS `schedule_performance_detail` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `schedule_performance_id` BIGINT NOT NULL COMMENT '排班业绩主表ID',
    `session_id` BIGINT NOT NULL COMMENT '场次ID',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `view_count` INT DEFAULT NULL COMMENT '场观（观看人数）',
    `sales_revenue` DECIMAL(12,2) DEFAULT NULL COMMENT '销售额',
    `refund` DECIMAL(12,2) DEFAULT NULL COMMENT '退款',
    `refund_quantity` INT DEFAULT NULL COMMENT '退款单量',
    `investment` DECIMAL(12,2) DEFAULT NULL COMMENT '投放',
    `pay_combo_cnt` INT DEFAULT NULL COMMENT '成交单量',
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
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，-1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_schedule_performance` (`tenant_id`, `schedule_performance_id`),
    KEY `idx_tenant_session` (`tenant_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班业绩明细表';

-- =====================================================
-- ALTER TABLE DDL - 新增8个业绩字段
-- 执行日期: 2026-04-16
-- =====================================================

-- 1. live_video 表新增字段
-- ALTER TABLE `live_video`
--     ADD COLUMN `exposure_count` INT DEFAULT NULL COMMENT '曝光次数' AFTER `thousand_sales`,
--     ADD COLUMN `follow_count` INT DEFAULT NULL COMMENT '涨粉人数' AFTER `exposure_count`,
--     ADD COLUMN `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率' AFTER `follow_count`,
--     ADD COLUMN `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率' AFTER `click_payment_rate`,
--     ADD COLUMN `max_online` INT DEFAULT NULL COMMENT '最高在线' AFTER `interaction_rate`,
--     ADD COLUMN `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率' AFTER `max_online`,
--     ADD COLUMN `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值' AFTER `conversion_rate`,
--     ADD COLUMN `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率' AFTER `uv_value`;

-- 2. live_session 表新增字段
-- ALTER TABLE `live_session`
--     ADD COLUMN `exposure_count` INT DEFAULT NULL COMMENT '曝光次数' AFTER `thousand_sales`,
--     ADD COLUMN `follow_count` INT DEFAULT NULL COMMENT '涨粉人数' AFTER `exposure_count`,
--     ADD COLUMN `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率' AFTER `follow_count`,
--     ADD COLUMN `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率' AFTER `click_payment_rate`,
--     ADD COLUMN `max_online` INT DEFAULT NULL COMMENT '最高在线' AFTER `interaction_rate`,
--     ADD COLUMN `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率' AFTER `max_online`,
--     ADD COLUMN `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值' AFTER `conversion_rate`,
--     ADD COLUMN `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率' AFTER `uv_value`;

-- 3. schedule_performance 表新增字段
-- ALTER TABLE `schedule_performance`
--     ADD COLUMN `exposure_count` INT DEFAULT NULL COMMENT '曝光次数' AFTER `thousand_sales`,
--     ADD COLUMN `follow_count` INT DEFAULT NULL COMMENT '涨粉人数' AFTER `exposure_count`,
--     ADD COLUMN `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率' AFTER `follow_count`,
--     ADD COLUMN `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率' AFTER `click_payment_rate`,
--     ADD COLUMN `max_online` INT DEFAULT NULL COMMENT '最高在线' AFTER `interaction_rate`,
--     ADD COLUMN `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率' AFTER `max_online`,
--     ADD COLUMN `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值' AFTER `conversion_rate`,
--     ADD COLUMN `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率' AFTER `uv_value`;

-- 4. schedule_performance_detail 表新增字段
-- ALTER TABLE `schedule_performance_detail`
--     ADD COLUMN `exposure_count` INT DEFAULT NULL COMMENT '曝光次数' AFTER `thousand_sales`,
--     ADD COLUMN `follow_count` INT DEFAULT NULL COMMENT '涨粉人数' AFTER `exposure_count`,
--     ADD COLUMN `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率' AFTER `follow_count`,
--     ADD COLUMN `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率' AFTER `click_payment_rate`,
--     ADD COLUMN `max_online` INT DEFAULT NULL COMMENT '最高在线' AFTER `interaction_rate`,
--     ADD COLUMN `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率' AFTER `max_online`,
--     ADD COLUMN `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值' AFTER `conversion_rate`,
--     ADD COLUMN `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率' AFTER `uv_value`;

-- 5. session_performance 表新增字段（补充BasePerformanceEntity完整字段）
-- ALTER TABLE `session_performance`
--     ADD COLUMN `refund_quantity` INT DEFAULT NULL COMMENT '退款单量' AFTER `refund`,
--     ADD COLUMN `pay_combo_cnt` INT DEFAULT NULL COMMENT '成交单量' AFTER `refund_quantity`,
--     ADD COLUMN `refund_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '退款率' AFTER `pay_combo_cnt`,
--     ADD COLUMN `thousand_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '千次成交' AFTER `refund_rate`,
--     ADD COLUMN `exposure_count` INT DEFAULT NULL COMMENT '曝光次数' AFTER `thousand_sales`,
--     ADD COLUMN `follow_count` INT DEFAULT NULL COMMENT '涨粉人数' AFTER `exposure_count`,
--     ADD COLUMN `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率' AFTER `follow_count`,
--     ADD COLUMN `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率' AFTER `click_payment_rate`,
--     ADD COLUMN `max_online` INT DEFAULT NULL COMMENT '最高在线' AFTER `interaction_rate`,
--     ADD COLUMN `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率' AFTER `max_online`,
--     ADD COLUMN `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值' AFTER `conversion_rate`,
--     ADD COLUMN `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率' AFTER `uv_value`;

-- 6. session_original_value 表新增字段
-- ALTER TABLE `session_original_value`
--     ADD COLUMN `source` TINYINT NOT NULL DEFAULT 1 COMMENT '数据来源：1-系统，2-手动' AFTER `source_type`,
--     ADD COLUMN `refund_quantity` INT DEFAULT NULL COMMENT '退款单量' AFTER `refund`,
--     ADD COLUMN `pay_combo_cnt` INT DEFAULT NULL COMMENT '成交单量' AFTER `refund_quantity`,
--     ADD COLUMN `refund_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '退款率' AFTER `pay_combo_cnt`,
--     ADD COLUMN `thousand_sales` DECIMAL(12,2) DEFAULT NULL COMMENT '千次成交' AFTER `roi`,
--     ADD COLUMN `exposure_count` INT DEFAULT NULL COMMENT '曝光次数' AFTER `thousand_sales`,
--     ADD COLUMN `follow_count` INT DEFAULT NULL COMMENT '涨粉人数' AFTER `exposure_count`,
--     ADD COLUMN `click_payment_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '点击-成交率' AFTER `follow_count`,
--     ADD COLUMN `interaction_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '互动率' AFTER `click_payment_rate`,
--     ADD COLUMN `max_online` INT DEFAULT NULL COMMENT '最高在线' AFTER `interaction_rate`,
--     ADD COLUMN `conversion_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '带货转化率' AFTER `max_online`,
--     ADD COLUMN `uv_value` DECIMAL(12,2) DEFAULT NULL COMMENT 'UV价值' AFTER `conversion_rate`,
--     ADD COLUMN `follow_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '涨粉率' AFTER `uv_value`;

-- =====================================================
-- ALTER TABLE DDL - schedulePerformanceSave接口字段扩展
-- 执行日期: 2026-04-22
-- 说明: 新增5个凭证图片URL字段到schedule_performance_image表
-- =====================================================

-- ALTER TABLE `schedule_performance_image`
--     ADD COLUMN `exposure_count_image_url` VARCHAR(512) DEFAULT NULL COMMENT '曝光次数图片URL' AFTER `pay_combo_cnt_image_url`,
--     ADD COLUMN `follow_count_image_url` VARCHAR(512) DEFAULT NULL COMMENT '涨粉人数图片URL' AFTER `exposure_count_image_url`,
--     ADD COLUMN `click_payment_rate_image_url` VARCHAR(512) DEFAULT NULL COMMENT '点击-成交率图片URL' AFTER `follow_count_image_url`,
--     ADD COLUMN `interaction_rate_image_url` VARCHAR(512) DEFAULT NULL COMMENT '互动率图片URL' AFTER `click_payment_rate_image_url`,
--     ADD COLUMN `max_online_image_url` VARCHAR(512) DEFAULT NULL COMMENT '最高在线图片URL' AFTER `interaction_rate_image_url`;