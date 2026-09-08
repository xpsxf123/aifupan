-- replay-30.sql
-- 任务：tb_script_monitor_report 摘要字段从 9 列合并为 1 列 summary_json TEXT
-- 日期：2026-06-02
-- Run dir: .claude/runs/Change__2026-06-02_summary-flatten-to-json
-- ADR-1：1 字段 vs 3 字段 vs 9 列 vs 子表 → 选 1 字段
-- ADR-2：TEXT vs MySQL JSON 类型 → 选 TEXT（无字段级查询需求；避免重新序列化 key 顺序漂移）
--
-- =====================================================================
-- 执行路径分两支：测试环境 / 生产环境
-- =====================================================================
--
-- 【测试环境】（数据可丢弃）
--   1. TRUNCATE TABLE tb_script_monitor_report;       -- 或 DELETE WHERE ...
--   2. 执行下方 Step 2 ADD summary_json
--   3. 执行下方 Step 3 DROP 旧 9 列
--
-- 【生产环境】（保留历史质检数据）
--   1. 执行下方 Step 1 UPDATE 迁移质检 4 count → JSON_OBJECT 写入新列
--      （前提：Step 2 ADD 列已先执行；建议同一脚本顺序为 Step 2 → Step 1 → Step 3）
--   2. 验证：SELECT COUNT(*) FROM tb_script_monitor_report
--           WHERE monitor_type=0 AND status=2 AND summary_json IS NULL;
--      正常应为 0。若非 0 排查 AI 历史数据空字段。
--   3. 执行下方 Step 3 DROP 旧 9 列
--
-- 还原度（monitor_type=1）/ 巡检（monitor_type=2）的 5 列从未有生成数据上线，
-- 直接 DROP，不需要迁移；未来 B3/B? 实现时直接走 summary_json 写。
-- =====================================================================

-- Step 2（生产 + 测试，先执行）：ADD 新列 summary_json
-- 幂等守护：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS（仅 MariaDB 10.3+ 支持），
-- 用 information_schema 检查存在性后 PREPARE 动态 SQL；重跑安全。
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tb_script_monitor_report' AND column_name = 'summary_json');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE tb_script_monitor_report ADD COLUMN summary_json TEXT NULL COMMENT ''摘要 JSON 字符串（全类型统一存储，前端解析）'' AFTER after_sales_count',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 1（生产专用，在 Step 2 之后执行）：迁移已有质检报告数据
-- 测试环境跳过本步（已 TRUNCATE）
UPDATE tb_script_monitor_report
SET summary_json = JSON_OBJECT(
        'crashCount', crash_count,
        'slackCount', slack_count,
        'brandDamageCount', brand_damage_count,
        'afterSalesCount', after_sales_count
    )
WHERE monitor_type = 0
  AND status = 2
  AND crash_count IS NOT NULL;

-- Step 3（生产 + 测试，最后执行）：DROP 旧 9 列
-- 注：MySQL 8.0+ 支持 IF EXISTS；如目标版本 < 8.0，需通过 information_schema 守护
ALTER TABLE tb_script_monitor_report
    DROP COLUMN crash_count,
    DROP COLUMN slack_count,
    DROP COLUMN brand_damage_count,
    DROP COLUMN after_sales_count,
    DROP COLUMN score,
    DROP COLUMN speech_speed,
    DROP COLUMN deviation_summary,
    DROP COLUMN interaction_rate,
    DROP COLUMN summary_text;
