/*
 2.6.01 话术智能监控 B7 — tb_script_monitor_report 加 trigger_source 字段

 ALTER TABLE tb_script_monitor_report ADD COLUMN trigger_source
 说明：
   - DEFAULT 'manual' 确保存量历史记录的 trigger_source 填充为 manual（语义保守，历史均为手动触发）
   - 新建记录由业务层按实际触发来源写入 'auto' 或 'manual'
   - NOT NULL + DEFAULT：DDL 执行期间无行锁争用（MySQL 8.0 支持 instant ADD COLUMN）

 幂等执行：通过 information_schema.COLUMNS 检查字段是否已存在；重复执行跳过。

 Target Server Version : MySQL 8.0.39
 File Encoding         : 65001
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 幂等守护：字段不存在时才执行 ALTER
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'tb_script_monitor_report'
       AND COLUMN_NAME = 'trigger_source') = 0,
    'ALTER TABLE `tb_script_monitor_report`
         ADD COLUMN `trigger_source` VARCHAR(16) NOT NULL DEFAULT ''manual''
         COMMENT ''触发来源 manual手动 auto自动''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
