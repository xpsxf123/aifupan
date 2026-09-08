CREATE TABLE `tb_asr_engine_config` (
  `id`          BIGINT       NOT NULL                COMMENT '主键，雪花ID',
  `tenant_id`   BIGINT       NOT NULL                COMMENT '租户ID',
  `user_id`     BIGINT       NOT NULL DEFAULT 0      COMMENT '用户ID；0 表示租户级默认配置（不绑定具体用户）',
  `language`    VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '语言编码；空串表示不限语言（适配该用户/租户下所有语言）',
  `engines`     VARCHAR(512) NOT NULL DEFAULT ''     COMMENT 'ASR 引擎列表，逗号分隔，顺序即返回顺序，例如 sense-voice,whisper',
  `remarks`     VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0      COMMENT '软删除标记：0 正常 1 已删除',
  `create_date` DATETIME     NOT NULL                COMMENT '创建时间',
  `update_date` DATETIME     NOT NULL                COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user_lang` (`tenant_id`, `user_id`, `language`, `is_deleted`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ASR 引擎按租户/用户/语言优先级配置';
