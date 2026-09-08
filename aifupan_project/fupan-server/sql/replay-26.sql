-- 话术智能监控：tb_script_monitor_report 加触发者用户ID（C1 修补）
ALTER TABLE `tb_script_monitor_report`
    ADD COLUMN `user_id` BIGINT NOT NULL DEFAULT 0 COMMENT '触发者用户ID（手动触发时按 user.getId() 写入）';
