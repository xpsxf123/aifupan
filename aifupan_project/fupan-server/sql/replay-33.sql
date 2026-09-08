-- ======================================================
-- replay-33.sql — B9: 已读语义迁入 tb_script_monitor_report 主表
--                  + 删除 tb_script_monitor_read / tb_script_monitor_role_confirm
-- ======================================================
-- 执行方式：step-by-step 手工逐条执行，任一 step 失败立即停止，禁批量复制粘贴
-- 顺序：先 ADD COLUMN（Step 1/2，幂等 IF NOT EXISTS）再 DROP TABLE（Step 3/4）
-- DDL 一旦执行不可回滚；回滚需重建 tb_script_monitor_read / tb_script_monitor_role_confirm
--                       并恢复 replay-32 版本代码
-- ======================================================

-- Step 1: tb_script_monitor_report 新增 is_read 字段
ALTER TABLE tb_script_monitor_report
    ADD COLUMN IF NOT EXISTS is_read TINYINT NOT NULL DEFAULT 0
    COMMENT '0未读 1已读（仅录制人查看 detail 接口后置1，重新生成时重置为0）'
    AFTER update_date;

-- Step 2: tb_script_monitor_report 新增 confirmed_at 字段
ALTER TABLE tb_script_monitor_report
    ADD COLUMN IF NOT EXISTS confirmed_at DATETIME NULL
    COMMENT '录制人首次查看时间戳（已读时间；重新生成时重置为 NULL）'
    AFTER is_read;

-- Step 3: 删除已读独立表
DROP TABLE IF EXISTS tb_script_monitor_read;

-- Step 4: 删除角色已知晓独立表
DROP TABLE IF EXISTS tb_script_monitor_role_confirm;
