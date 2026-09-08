-- 第三方直播录制记录表
CREATE TABLE `tb_thirdparty_live_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `trade_id` bigint NOT NULL COMMENT '行业ID，关联tb_trade.id',
  `anchor_nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主播抖音昵称',
  `douyin_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '抖音号',
  `cloud_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '云空间链接',
  `viewers` bigint DEFAULT '0' COMMENT '场观',
  `monthly_sales` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '月销售额（区间值，如100万-500万）',
  `create_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_trade_id_deleted` (`trade_id`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方直播录制记录表';
