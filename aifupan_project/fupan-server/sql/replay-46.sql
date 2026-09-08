-- ============================================================================
-- SEO slug 变更历史（官网旧地址 301）
-- 分支 feature/seo-site-api ｜ openspec: .claude/runs/Change__2026-08-18_seo-site-api/
--
-- 说明：
--   1. isolated additive DDL——无外键回指既有表，回滚只需删除这一张表
--   2. 不设 tenant_id，与 tb_seo_* 一致（SEO 内容是平台级运营数据）
-- ============================================================================

CREATE TABLE `tb_seo_slug_history` (
  `id`          BIGINT       NOT NULL             COMMENT '主键，雪花ID',
  `entity_type` TINYINT      NOT NULL             COMMENT '对象类型：1 文章 2 分类 3 标签',
  `entity_id`   BIGINT       NOT NULL             COMMENT '对应实体主键（tb_seo_article/category/tag.id）',
  `old_slug`    VARCHAR(180) NOT NULL             COMMENT '被替换掉的旧 slug。长度对齐 tb_seo_article.slug',
  `create_date` DATETIME     NOT NULL             COMMENT '记录时间',
  PRIMARY KEY (`id`),
  -- 同一类型下旧 slug 唯一：一个旧地址只能指向一个实体。
  -- 反复改名（x→y→x→y）会重复插入同一条，靠 ON DUPLICATE KEY UPDATE 覆盖为最新归属
  UNIQUE KEY `uk_type_old_slug` (`entity_type`, `old_slug`),
  -- 供「某实体改过哪些 slug」的反查，删除实体时可一并清理
  KEY `idx_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO slug 变更历史，供官网 301';

-- ----------------------------------------------------------------------------
-- 【为什么只记 old_slug，不记 new_slug】
--
-- 新值永远从实体表现查。这样多次改名（x→y→z）时，查 x 和查 y 都**一步跳到最终的 z**，
-- 不会产生 301 跳转链——搜索引擎对跳转链的权重传递有衰减，多跳一次就少一分。
-- 若存了 new_slug，查 x 会得到 y，再查 y 得到 z，官网要么发两次 301，要么自己做链式解析。
--
-- 【为什么查询必须先当前表、后历史表】
--
-- 这个顺序自动处理掉三个边界，反过来则全错：
--   1. slug 改回原值（x→y→x）：查 x 命中当前表，正常渲染。历史表里那条 x 永不生效，无害
--   2. A 改 x→y 后，新文章 B 占用了 x：查 x 命中当前表的 B，返回 B 的内容。
--      若先查历史表，会把访问 B 的读者错误 301 到 A
--   3. A 改 x→y 后 A 被删除：查 x 命中历史表 → 回查实体 → is_deleted=1 → 返回 404。
--      删除后旧地址应该 404 而不是 301，内容确实没了
--
-- 【为什么逻辑删除时不写历史】
--
-- 同上第 3 点：删除意味着内容不存在，应当 404。写了历史反而会把读者导向一个
-- 「曾经存在」的假象，或者在实体已删的情况下产生悬空跳转。
-- ----------------------------------------------------------------------------
