-- 话术智能监控：角色已知晓勾选表（B4-3）
-- 用于质检报告「已知晓」角色确认勾选，运营/主播/主管三角色各独立一行
CREATE TABLE `tb_script_monitor_role_confirm` (
    `id`           BIGINT      NOT NULL COMMENT '主键 Snowflake',
    `tenant_id`    BIGINT      NOT NULL COMMENT '租户ID',
    `report_id`    BIGINT      NOT NULL COMMENT '报告ID → tb_script_monitor_report.id',
    `confirm_role` TINYINT     NOT NULL COMMENT '角色 1=运营 2=主播 3=主管',
    `user_id`      BIGINT      NOT NULL COMMENT '勾选者用户ID',
    `user_name`    VARCHAR(64) NOT NULL DEFAULT '' COMMENT '勾选者用户昵称快照',
    `create_date`  DATETIME    NOT NULL COMMENT '创建时间',
    `update_date`  DATETIME    NOT NULL COMMENT '最后修改时间',
    `is_deleted`   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已删除 0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_role` (`report_id`, `confirm_role`, `is_deleted`),
    KEY `idx_tenant_report` (`tenant_id`, `report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI监控报告角色已知晓勾选表';
