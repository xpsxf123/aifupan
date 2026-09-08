
-- ======================================================
-- replay-35.sql — 用户电脑配置表
-- ======================================================
-- 执行方式：逐条执行，任一步失败即停止

-- Step 1: 创建 tb_computer_config 表
CREATE TABLE IF NOT EXISTS `tb_computer_config` (
  `id` bigint NOT NULL COMMENT '主键 Snowflake',
  `cpu_id` varchar(64) NOT NULL COMMENT '机器码MD5（cpu+磁盘+主板）',
  `user_id` bigint NOT NULL COMMENT '最近上报用户ID',
  `cpu_model` varchar(256) DEFAULT NULL COMMENT 'CPU型号',
  `cpu_cores` int DEFAULT NULL COMMENT 'CPU物理核心数',
  `cpu_threads` int DEFAULT NULL COMMENT 'CPU逻辑线程数',
  `cpu_frequency_mhz` int DEFAULT NULL COMMENT 'CPU主频(MHz)',
  `ram_gb` decimal(6,2) DEFAULT NULL COMMENT '物理内存(GB)',
  `gpu_model` varchar(512) DEFAULT NULL COMMENT 'GPU型号（多个用;分隔）',
  `vram_gb` decimal(6,2) DEFAULT NULL COMMENT 'GPU显存(GB)',
  `disk_total_gb` decimal(10,2) DEFAULT NULL COMMENT '系统盘总空间(GB)',
  `disk_free_gb` decimal(10,2) DEFAULT NULL COMMENT '系统盘剩余空间(GB)',
  `os_version` varchar(128) DEFAULT NULL COMMENT '操作系统版本',
  `client_version` varchar(32) DEFAULT NULL COMMENT '客户端版本号',
  `mac_address` varchar(64) DEFAULT NULL COMMENT '物理网卡MAC地址',
  `computer_name` varchar(128) DEFAULT NULL COMMENT '计算机名',
  `ip_address` varchar(64) DEFAULT NULL COMMENT '本机IPv4地址',
  `screen_resolution` varchar(32) DEFAULT NULL COMMENT '主屏幕分辨率(w×h)',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `update_date` datetime NOT NULL COMMENT '最后修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cpu_id`(`cpu_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户电脑配置表' ROW_FORMAT = DYNAMIC;


/*
 2.6.01 话术还原度 -- Slice A DDL 变更
 变更：tb_standard_script
   - DROP INDEX uk_tenant_anchor（基于 anchor_url_user_id 的旧唯一索引）
   - DROP COLUMN anchor_url_user_id（旧关联字段，改用 sec_uid 联合定位）
   - ADD COLUMN sec_uid VARCHAR(64)（主播唯一标识，标准稿主定位字段）
   - ADD UNIQUE INDEX uk_tenant_user_secuid (tenant_id, user_id, sec_uid, is_deleted)

 背景（ADR-2）：当前 tb_standard_script 无业务数据（还原度未上线），
   DROP COLUMN + DROP INDEX 安全无回滚成本。
   secUid 比 anchorUrlUserId 更稳定（主播平台唯一标识，全生命周期不变），
   彻底消除占位/回填/幽灵行问题。

 操作顺序：先 DROP INDEX（避免 unique 冲突），再 DROP COLUMN，再 ADD COLUMN + ADD INDEX

 Target Server Version : MySQL 8.0.39
 File Encoding         : 65001
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `tb_standard_script`
  DROP INDEX `uk_tenant_anchor`,
  DROP COLUMN `anchor_url_user_id`,
  ADD COLUMN `sec_uid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '主播唯一标识（secUid）；标准稿按 (tenant_id, user_id, sec_uid) 联合定位' AFTER `user_id`,
  ADD UNIQUE INDEX `uk_tenant_user_secuid` (`tenant_id`, `user_id`, `sec_uid`, `is_deleted`) USING BTREE;

SET FOREIGN_KEY_CHECKS = 1;
