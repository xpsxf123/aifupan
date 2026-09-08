-- ============================================================
-- replay-43.sql
-- 算力租户口径修正：给 tb_order / tb_user_property_details 加 tenant_id，
-- 回填历史，并按新口径重灌算力汇总（replay-41 的存量作废，以本脚本为准）。
--
-- 背景 / 为什么这么改：
--   1. 明细表原本没有租户列，靠 parent_user_id → tb_tenant 推断租户。该推断方式在
--      多租户/换租户场景下无法准确归属（consumption 应属「消费当时所在租户」，推断给不出）。
--      现改为写入时把租户固化到明细行 tenant_id 上，聚合直读，不再推断。
--   2. 现在改为：写入时把租户固化到明细行 tenant_id 上
--        - 消费行     = 消费用户当时的 active_tenant_id
--        - 清零/reset 行 = 订单固化的 tenant_id（资产归属租户，不随人换租户跑）
--      聚合直接读明细 tenant_id，不再 JOIN tb_tenant。
--   3. 口径修正：只算「用户创建」的真实消费，排除系统清零/reset（asset_creation_type=1），
--      否则清零余量(signs=0)/月度reset(signs=1)会污染算力值（之前线上偏高）。
--
-- ⚠️ 执行顺序：Part1 建列 → Part2/3 回填 → Part4 重灌。
--    Part4 请在同一个数据库会话里整段执行（用了会话变量 @max_id）。
-- ============================================================


-- ============================================================
-- Part 1. 加 tenant_id 列（MySQL 8 INSTANT，大表也快）
-- ============================================================

ALTER TABLE tb_order
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0
  COMMENT '租户id(下单主账号当时的 active_tenant_id，固化)' AFTER user_id;

ALTER TABLE tb_user_property_details
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0
  COMMENT '租户id(消费=当时active_tenant_id；清零/reset=订单固化租户)' AFTER parent_user_id;


-- ============================================================
-- Part 2. 回填订单历史 tenant_id
--   只有主账号下单，主账号 active_tenant_id 恒为自身租户，直接取之。
-- ============================================================

UPDATE tb_order o
  JOIN tb_user u ON u.id = o.user_id
SET o.tenant_id = IFNULL(u.active_tenant_id, 0)
WHERE IFNULL(o.tenant_id, 0) = 0;


-- ============================================================
-- Part 3. 回填明细表历史 tenant_id（全资产类型，不限 aiTokenNum，令该列对所有资产完整）
--   规则：parent_user_id=0 用 user_id、>0 用 parent_user_id，去 tb_tenant 找 tenant_id。
--   tb_tenant 中一个 user_id 只有一行（不存在一主账号多租户），故直连 JOIN，无需 GROUP BY/MIN。
--   owner 无 tb_tenant 行的老数据 → JOIN 未命中，tenant_id 保持 0（归入「未知租户」，已知取舍）。
-- ============================================================

UPDATE tb_user_property_details upd
  JOIN tb_tenant tnt
    ON tnt.user_id = (CASE WHEN upd.parent_user_id > 0 THEN upd.parent_user_id ELSE upd.user_id END)
   AND tnt.is_deleted = 0
SET upd.tenant_id = tnt.id
WHERE IFNULL(upd.tenant_id, 0) = 0;


-- ============================================================
-- Part 4. 按新口径重灌算力汇总（请整段在同一会话执行）
--   - 读明细 tenant_id（不再 JOIN tb_tenant）
--   - 只算 IFNULL(asset_creation_type,0)=0 的真实消费（排除系统清零/reset）
--   - 净额 = SUM(signs=0 加 / signs=1 减)
--   - 用户维度按 (user_id, tenant_id) 分组（同一用户多租户各一行，对应 uk_user_tenant）
-- ============================================================

-- ① 锁定上界（会话变量，后续 INSERT/水位线共用同一值）
SET @max_id := (SELECT COALESCE(MAX(id), 0) FROM tb_user_property_details);

-- ② 清空三张汇总表（TRUNCATE 自动提交，重灌前先清）
TRUNCATE TABLE tb_user_power_rollup;
TRUNCATE TABLE tb_tenant_power_rollup;
TRUNCATE TABLE tb_power_rollup_watermark;

-- ③ 灌用户维度：每 (user_id, tenant_id) 一行
SET @rn := 0;
INSERT INTO tb_user_power_rollup (id, user_id, tenant_id, user_power_consume, is_deleted, create_date, update_date)
SELECT (@rn := @rn + 1), t.user_id, t.tenant_id, t.net, 0, NOW(), NOW()
FROM (
  SELECT upd.user_id,
         upd.tenant_id,
         SUM(CASE WHEN upd.signs = 0 THEN upd.quantity ELSE -upd.quantity END) AS net
  FROM tb_user_property_details upd
  WHERE upd.commodity_type_code = 'aiTokenNum'
    AND IFNULL(upd.asset_creation_type, 0) = 0
    AND upd.id <= @max_id
  GROUP BY upd.user_id, upd.tenant_id
) t;

-- ④ 灌租户维度：每 tenant_id 一行（同源、同上界）
SET @rn2 := 0;
INSERT INTO tb_tenant_power_rollup (id, tenant_id, tenant_power_consume, is_deleted, create_date, update_date)
SELECT (@rn2 := @rn2 + 1), t.tenant_id, t.net, 0, NOW(), NOW()
FROM (
  SELECT upd.tenant_id,
         SUM(CASE WHEN upd.signs = 0 THEN upd.quantity ELSE -upd.quantity END) AS net
  FROM tb_user_property_details upd
  WHERE upd.commodity_type_code = 'aiTokenNum'
    AND IFNULL(upd.asset_creation_type, 0) = 0
    AND upd.id <= @max_id
  GROUP BY upd.tenant_id
) t;

-- ⑤ 写水位线为同一上界（定时任务从这里续跑增量）
INSERT INTO tb_power_rollup_watermark (id, biz_code, last_detail_id, is_deleted, create_date, update_date)
VALUES (1, 'aiTokenNum', @max_id, 0, NOW(), NOW());
