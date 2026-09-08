SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `tb_scene_slice` (
    `id` bigint NOT NULL COMMENT '主键 Snowflake',
    `video_id` varchar(64) NOT NULL COMMENT '视频ID',
    `oss_key` varchar(512) DEFAULT NULL COMMENT 'OSS文件key',
    `slice_seconds` int NOT NULL DEFAULT 10 COMMENT '截取秒数(距视频结束)',
    `status_flag` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-待开始 1-处理中 2-处理完成 3-处理失败',
    `ai_result` text DEFAULT NULL COMMENT 'AI分析结果',
    `fail_reason` varchar(1024) DEFAULT NULL COMMENT '失败原因',
    `analysis_start_time` datetime DEFAULT NULL COMMENT 'AI分析开始时间',
    `analysis_time` datetime DEFAULT NULL COMMENT 'AI分析完成时间',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `create_date` datetime NOT NULL COMMENT '创建时间',
    `update_date` datetime NOT NULL COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否已删除 0-否 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_video_id` (`video_id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '场景切片记录表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
