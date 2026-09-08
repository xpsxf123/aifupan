-- ============================================================================
-- 一次性数据补录：为部署前发生的一次 slug 变更补记历史，让旧地址能 301
--
-- 【为什么需要这个脚本】
--   2026-08-18，运营在测试环境后台把文章
--     「直播复盘怎么做？操盘手必学的 6 步实操方法（附复盘模板）」
--   的 slug 从 ...-fufupanmoban 改成了 ...-bushicaofangfa。
--
--   当时 feature/seo-article 尚未部署，执行保存的是旧代码——没有 slug 历史
--   写入逻辑，因此 tb_seo_slug_history 里没有这条记录，旧地址直接变成永久 404。
--
--   若旧地址已被搜索引擎收录，404 会让它累积的排名与外链一起作废，且没有任何
--   报错或告警。本脚本补上该记录，使官网对旧地址发 301 到当前 slug。
--
-- 【适用范围】
--   只补这一条。部署之后发生的 slug 变更由代码自动记录，不需要人工介入。
--
-- 【幂等】
--   唯一键 uk_type_old_slug (entity_type, old_slug) 冲突时更新 entity_id，
--   可重复执行，不会产生重复行。
--
-- 分支 feature/seo-article ｜ openspec: .claude/runs/Change__2026-08-18_seo-site-api/
-- ============================================================================


-- ---------------------------------------------------------------- 第一步：核对
-- 先跑这句，确认要补录的是哪篇文章。预期只返回 1 行，且 is_deleted = 0。
-- 如果返回 0 行，说明标题也被改过——改用下面注释里的 id 方式定位。

SELECT id, slug, title, article_status, is_deleted, update_date
FROM tb_seo_article
WHERE title = '直播复盘怎么做？操盘手必学的 6 步实操方法（附复盘模板）'
  AND is_deleted = 0;


-- ---------------------------------------------------------------- 第二步：补录
-- 用子查询按标题定位 id，避免手工抄错雪花 ID。
--
-- ⚠️ 主键 1955000000000000901 是手工指定的示例雪花值（表的 id 是 IdType.INPUT，
--    不会自增）。执行前请确认它未被占用：
--      SELECT COUNT(*) FROM tb_seo_slug_history WHERE id = 1955000000000000901;
--    若已占用，换一个未使用的值即可，该值本身无业务含义。

INSERT INTO tb_seo_slug_history (id, entity_type, entity_id, old_slug, create_date)
SELECT
    1955000000000000901,
    1,  -- entity_type: 1 文章 / 2 分类 / 3 标签，见 SeoConstant.SLUG_ENTITY_*
    a.id,
    'zhibofupanzenmezuo-caopanshoubixuede-6-bushicaofangfa-fufupanmoban',
    NOW()
FROM tb_seo_article a
WHERE a.title = '直播复盘怎么做？操盘手必学的 6 步实操方法（附复盘模板）'
  AND a.is_deleted = 0
ON DUPLICATE KEY UPDATE
    entity_id   = VALUES(entity_id),
    create_date = NOW();


-- ---------------------------------------------------------------- 第三步：验证
-- 预期返回 1 行，且 entity_id 指向上面查到的那篇文章。

SELECT h.id, h.entity_type, h.entity_id, h.old_slug, h.create_date,
       a.slug AS current_slug, a.article_status
FROM tb_seo_slug_history h
LEFT JOIN tb_seo_article a ON a.id = h.entity_id
WHERE h.entity_type = 1
  AND h.old_slug = 'zhibofupanzenmezuo-caopanshoubixuede-6-bushicaofangfa-fufupanmoban';

-- 补录成功后，接口应返回 redirectSlug 而不再是 404：
--   curl "https://testapi.aifupan.com.cn/replay/site/article/detail?slug=zhibofupanzenmezuo-caopanshoubixuede-6-bushicaofangfa-fufupanmoban"
-- 预期：{"code":0,...,"data":{"redirectSlug":"<当前 slug>", 其余字段为 null}}
--
-- 注意 article_status 必须是 1：代码在命中历史表后会回查实体，
-- 若文章已下架或已删除则仍返回 404——301 到一个 404 页面比直接 404 更糟。


-- ---------------------------------------------------------------- 回滚
-- DELETE FROM tb_seo_slug_history
--  WHERE entity_type = 1
--    AND old_slug = 'zhibofupanzenmezuo-caopanshoubixuede-6-bushicaofangfa-fufupanmoban';
