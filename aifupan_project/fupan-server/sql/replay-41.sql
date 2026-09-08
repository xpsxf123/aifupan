
-- ============================================================
-- replay-41.sql
-- 用户/租户算力消耗汇总表（XXL-Job timingUpdatePowerConsume 增量刷新）
-- 数据源：tb_user_property_details（commodity_type_code='aiTokenNum'）
-- 口径：净额 = SUM(CASE WHEN signs = 0 THEN quantity ELSE -quantity END)
--       signs=1 为执行失败回退，必须冲减；quantity 存绝对值
-- ============================================================

-- 用户维度：每用户一行
CREATE TABLE `tb_user_power_rollup` (
  `id` bigint(20) NOT NULL COMMENT '主键，Snowflake',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID(实际消费者)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户id(tb_tenant.id，主账号拥有的租户)',
  `user_power_consume` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户累计算力消耗(token数,净额=扣减-失败回退)',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否已删除 0否 1是',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `update_date` datetime NOT NULL COMMENT '最后刷新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant` (`user_id`,`tenant_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户算力消耗汇总表';

-- 增量水位线：记录已处理到的明细表最大 id（保证增量累加恰好一次）
CREATE TABLE `tb_power_rollup_watermark` (
  `id`             BIGINT      NOT NULL COMMENT '主键，Snowflake',
  `biz_code`       VARCHAR(64) NOT NULL COMMENT '业务标识，本轮固定 aiTokenNum',
  `last_detail_id` BIGINT      NOT NULL DEFAULT 0 COMMENT '已处理到的 tb_user_property_details 最大id(含)',
  `is_deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已删除 0否 1是',
  `create_date`    DATETIME    NOT NULL COMMENT '创建时间',
  `update_date`    DATETIME    NOT NULL COMMENT '最后推进时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算力汇总增量水位线';

-- 租户维度：每租户(主账号)一行
CREATE TABLE `tb_tenant_power_rollup` (
  `id`                   BIGINT   NOT NULL COMMENT '主键，Snowflake',
  `tenant_id`            BIGINT   NOT NULL COMMENT '租户id(tb_tenant.id)',
  `tenant_power_consume` BIGINT   NOT NULL DEFAULT 0 COMMENT '租户全体累计算力消耗(token数,净额=扣减-失败回退)',
  `is_deleted`           TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已删除 0否 1是',
  `create_date`          DATETIME NOT NULL COMMENT '创建时间',
  `update_date`          DATETIME NOT NULL COMMENT '最后刷新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户算力消耗汇总表';


-- ============================================================
-- 存量初始化（一次性，人工执行）
--
-- ⚠️ 执行顺序硬约束：
--   1. 上面三条 CREATE TABLE 必须先于代码上线执行，否则 pageListNew 的
--      LEFT JOIN 直接报错。
--   2. 下面四步存量初始化必须先于 XXL-Job "timingUpdatePowerConsume"
--      第一次触发执行。定时任务读不到水位线行会抛业务异常、任务在
--      XXL-Job Admin 上呈现为「失败」（不会退化为全量跑），不会写脏
--      数据，但也不会产出任何汇总值。
--   3. 四步必须共用同一个上界（第 ① 步锁定的 locked_max_id），否则灌数
--      期间新产生的流水会被重复计入或漏计。
--
-- ⚠️ 执行方式（务必逐步手工执行，不要整段一次性跑）：
--   先单独执行第 ① 步，把它返回的 locked_max_id 记下来，然后把第 ②③④
--   步 SQL 文本里的占位符 <LOCKED_MAX_ID> 全部替换成这个数字再执行。
--   这样即使灌数过程耗时很久、期间不断有新流水写入，上界也是被「锁定」
--   的固定值，与最后写入水位线的值严格一致。
-- ============================================================

-- ① 锁定上界：先单独执行本条，记录返回的 locked_max_id
--    COALESCE 保证空库返回 0 而不是 NULL（NULL 会让第 ④ 步插入 NOT NULL 列报错）
SELECT COALESCE(MAX(id), 0) AS locked_max_id FROM tb_user_property_details;

-- ② 灌用户维度（id 用递增计数器，与 Snowflake 值域不冲突）
--    ⚠️ 把 <LOCKED_MAX_ID> 替换为第 ① 步记下的值；②③④ 三处必须是同一个值
--    租户 tenant_id 由主账号(parent_user_id 归一化) JOIN tb_tenant(user_id) 取 tb_tenant.id；
--    主账号无租户行的用户会被 INNER JOIN 排除（正常注册都建租户，属极少数遗留数据）
SET @rn := 0;
INSERT INTO tb_user_power_rollup (id, user_id, tenant_id, user_power_consume, is_deleted, create_date, update_date)
SELECT (@rn := @rn + 1), t.user_id, t.tenant_id, t.net, 0, NOW(), NOW()
FROM (
  SELECT upd.user_id,
         tnt.id AS tenant_id,
         SUM(CASE WHEN upd.signs = 0 THEN upd.quantity ELSE -upd.quantity END) AS net
  FROM tb_user_property_details upd
  JOIN tb_tenant tnt ON tnt.user_id = (CASE WHEN upd.parent_user_id > 0 THEN upd.parent_user_id ELSE upd.user_id END) AND tnt.is_deleted = 0
  WHERE upd.commodity_type_code = 'aiTokenNum' AND upd.id <= <LOCKED_MAX_ID>
  GROUP BY upd.user_id, tnt.id
) t;

-- ③ 灌租户维度（同源、同上界，按 tenant_id 归集）
--    ⚠️ 这里的 <LOCKED_MAX_ID> 必须与第 ② 步完全相同，否则两表口径不一致
SET @rn2 := 0;
INSERT INTO tb_tenant_power_rollup (id, tenant_id, tenant_power_consume, is_deleted, create_date, update_date)
SELECT (@rn2 := @rn2 + 1), t.tenant_id, t.net, 0, NOW(), NOW()
FROM (
  SELECT tnt.id AS tenant_id,
         SUM(CASE WHEN upd.signs = 0 THEN upd.quantity ELSE -upd.quantity END) AS net
  FROM tb_user_property_details upd
  JOIN tb_tenant tnt ON tnt.user_id = (CASE WHEN upd.parent_user_id > 0 THEN upd.parent_user_id ELSE upd.user_id END) AND tnt.is_deleted = 0
  WHERE upd.commodity_type_code = 'aiTokenNum' AND upd.id <= <LOCKED_MAX_ID>
  GROUP BY tnt.id
) t;

-- ④ 最后一步：把同一个锁定上界写进水位线（定时任务从这里续跑）
--    ⚠️ 必须在 ②③ 都执行成功之后再执行，且 <LOCKED_MAX_ID> 仍是第 ① 步那个值；
--       写小了会重复累加，写大了会永久漏算
INSERT INTO tb_power_rollup_watermark (id, biz_code, last_detail_id, is_deleted, create_date, update_date)
VALUES (1, 'aiTokenNum', <LOCKED_MAX_ID>, 0, NOW(), NOW());
