/*
 2.6.01 话术智能监控 — 增量 DDL

 新增：tb_standard_script（标准直播稿）、tb_script_monitor_report（AI监控报告，含任务状态）、tb_script_monitor_read（报告已读确认）
 变更：tb_anchor_url_user 增加三个 AI 监控开关 + 当前已确认标准稿 ID

 Target Server Version : MySQL 8.0.39
 File Encoding         : 65001
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_standard_script（replay-words 新建）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_standard_script`  (
  `id` bigint NOT NULL COMMENT '主键 Snowflake',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '确认者用户ID',
  `anchor_url_user_id` bigint NOT NULL DEFAULT 0 COMMENT '关联→tb_anchor_url_user.id，新增直播间时先为0',
  `speech_mode` tinyint NOT NULL COMMENT '话术模式 0非循环 1循环',
  `speech_speed` smallint NOT NULL DEFAULT 280 COMMENT '语速 字/分钟 100-500',
  `cycle_duration_minutes` smallint NULL DEFAULT NULL COMMENT '循环话术预估时长分钟',
  `reference_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参考直播脚本原文',
  `time_axis_script` json NULL COMMENT '时间轴 [{"timeRange":"00:00-05:00","title":"开场","content":"..."}]',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `update_date` datetime NOT NULL COMMENT '最后修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_anchor`(`tenant_id`, `anchor_url_user_id`, `is_deleted`) USING BTREE,
  INDEX `idx_tenant_user`(`tenant_id`, `user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标准直播稿已确认表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_script_monitor_report（replay-ai 新建）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_script_monitor_report`  (
  `id` bigint NOT NULL COMMENT '主键 Snowflake',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `source_type` tinyint NOT NULL COMMENT '资源类型 0录制视频 1上传文件',
  `scene_type` tinyint NOT NULL COMMENT '业务场景 0复盘 1视频分析 2文案预审',
  `source_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源ID',
  `monitor_type` tinyint NOT NULL COMMENT '监控类型 0质检 1还原度 2巡检',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0未生成 1生成中 2已生成 3失败 4不可生成',
  `report_body_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MongoDB ObjectId',
  `crash_count` smallint NULL DEFAULT NULL COMMENT '崩盘话术句数(质检)',
  `slack_count` smallint NULL DEFAULT NULL COMMENT '摸鱼话术句数(质检)',
  `brand_damage_count` smallint NULL DEFAULT NULL COMMENT '品牌伤害话术句数(质检)',
  `after_sales_count` smallint NULL DEFAULT NULL COMMENT '售后风险话术句数(质检)',
  `score` tinyint NULL DEFAULT NULL COMMENT '还原度评分 0-100(还原度)',
  `speech_speed` smallint NULL DEFAULT NULL COMMENT '实际语速(还原度)',
  `deviation_summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '偏差摘要(还原度)',
  `interaction_rate` tinyint NULL DEFAULT NULL COMMENT '互动有效性百分比 0-100(巡检)',
  `summary_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '巡检摘要(巡检)',
  `unavailable_reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可生成原因',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `update_date` datetime NOT NULL COMMENT '最后修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_source_monitor`(`tenant_id`, `source_type`, `scene_type`, `source_id`, `monitor_type`) USING BTREE,
  INDEX `idx_tenant_monitor_status`(`tenant_id`, `monitor_type`, `status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI监控报告表(含任务状态)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for tb_script_monitor_read（replay-ai 新建）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_script_monitor_read`  (
  `id` bigint NOT NULL COMMENT '主键 Snowflake',
  `report_id` bigint NOT NULL COMMENT '报告ID → tb_script_monitor_report.id',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `update_date` datetime NOT NULL COMMENT '最后修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_user`(`report_id`, `user_id`, `is_deleted`) USING BTREE,
  INDEX `idx_report_id`(`report_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI监控报告已读确认表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Alter table tb_anchor_url_user（replay-words 增加 AI 监控开关 + 标准稿 ID）
-- ----------------------------
ALTER TABLE `tb_anchor_url_user`
  ADD COLUMN `is_script_quality_inspection` tinyint NOT NULL DEFAULT 0 COMMENT '话术质检开关 0否 1是',
  ADD COLUMN `is_script_fidelity_monitor` tinyint NOT NULL DEFAULT 0 COMMENT '话术还原度开关 0否 1是',
  ADD COLUMN `is_interaction_patrol` tinyint NOT NULL DEFAULT 0 COMMENT '互动巡检开关 0否 1是',
  ADD COLUMN `standard_script_id` bigint NOT NULL DEFAULT 0 COMMENT '当前已确认标准稿ID→tb_standard_script.id';

SET FOREIGN_KEY_CHECKS = 1;
